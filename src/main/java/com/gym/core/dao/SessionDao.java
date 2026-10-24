package com.gym.core.dao;

import com.gym.core.model.ClassSession;
import com.gym.core.model.PtSession;
import com.gym.core.model.Session;
import com.gym.core.model.SessionType;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DAO cho Session (lớp cha): đọc ra đúng kiểu con theo Session_Type (ClassSession hoặc PtSession);
 * thêm/sửa giao cho DAO của kiểu con. Dùng khi cần "mọi buổi tập" (ví dụ lịch tuần).
 */
public class SessionDao extends AbstractJdbcDao<Session, Integer> {

    private final ClassSessionDao classDao = new ClassSessionDao();
    private final PtSessionDao ptDao = new PtSessionDao();

    @Override
    protected String tableName() {
        return "[SESSION]";
    }

    @Override
    protected String idColumn() {
        return "s.Session_ID";
    }

    @Override
    protected String fromClause() {
        return "[SESSION] s "
                + "LEFT JOIN CLASS c ON c.Session_ID = s.Session_ID "
                + "LEFT JOIN PT_BOOKING p ON p.Session_ID = s.Session_ID";
    }

    @Override
    protected String selectColumns() {
        return "s.Session_ID, s.Session_Type, s.Room_ID, s.Trainer_ID, s.Start_Time, s.End_Time, "
                + "c.Class_Name, c.Max_Capacity, p.Visitor_ID AS Member_ID, p.Booking_Status";
    }

    @Override
    protected Session mapRow(ResultSet rs) throws SQLException {
        if (SessionType.fromDb(rs.getString("Session_Type")) == SessionType.CLASS) {
            return ClassSessionDao.map(rs);
        }
        return PtSessionDao.map(rs);
    }

    /** Điền các cột chung của bảng SESSION vào một đối tượng Session. */
    static void fillBase(Session s, ResultSet rs) throws SQLException {
        s.setSessionId(rs.getInt("Session_ID"));
        s.setSessionType(SessionType.fromDb(rs.getString("Session_Type")));
        s.setRoomId(rs.getInt("Room_ID"));
        s.setTrainerId(rs.getInt("Trainer_ID"));
        s.setStartTime(Sql.dateTime(rs, "Start_Time"));
        s.setEndTime(Sql.dateTime(rs, "End_Time"));
    }

    /** INSERT vào bảng SESSION trên kết nối có sẵn, trả về Session_ID mới. Dùng trong transaction của DAO con. */
    static int insertSessionRow(Connection cn, Session s, SessionType type) throws SQLException {
        return insertKey(cn, "INSERT INTO [SESSION] (Session_Type, Room_ID, Trainer_ID, Start_Time, End_Time) "
                + "VALUES (?, ?, ?, ?, ?)", ps -> {
            ps.setString(1, type.getDbValue());
            ps.setInt(2, s.getRoomId());
            ps.setInt(3, s.getTrainerId());
            Sql.setDateTime(ps, 4, s.getStartTime());
            Sql.setDateTime(ps, 5, s.getEndTime());
        });
    }

    /** UPDATE bảng SESSION (không đổi Session_Type). Dùng trong transaction của DAO con. */
    static void updateSessionRow(Connection cn, Session s) throws SQLException {
        exec(cn, "UPDATE [SESSION] SET Room_ID = ?, Trainer_ID = ?, Start_Time = ?, End_Time = ? "
                + "WHERE Session_ID = ?", ps -> {
            ps.setInt(1, s.getRoomId());
            ps.setInt(2, s.getTrainerId());
            Sql.setDateTime(ps, 3, s.getStartTime());
            Sql.setDateTime(ps, 4, s.getEndTime());
            ps.setInt(5, s.getSessionId());
        });
    }

    @Override
    public Session insert(Session s) {
        if (s instanceof ClassSession c) {
            return classDao.insert(c);
        }
        if (s instanceof PtSession p) {
            return ptDao.insert(p);
        }
        throw new IllegalArgumentException("Chỉ thêm được lớp (ClassSession) hoặc buổi PT (PtSession).");
    }

    @Override
    public void update(Session s) {
        if (s instanceof ClassSession c) {
            classDao.update(c);
        } else if (s instanceof PtSession p) {
            ptDao.update(p);
        } else {
            throw new IllegalArgumentException("Chỉ sửa được lớp (ClassSession) hoặc buổi PT (PtSession).");
        }
    }

    @Override
    public void deleteById(Integer id) {
        execute("DELETE FROM [SESSION] WHERE Session_ID = ?", ps -> ps.setInt(1, id));
    }

    /** Các buổi bắt đầu trong khoảng [from, to). Dùng cho lịch tuần (có chỉ mục theo thời gian). */
    public List<Session> findStartingBetween(LocalDateTime from, LocalDateTime to) {
        return queryList(selectSql() + " WHERE s.Start_Time >= ? AND s.Start_Time < ? ORDER BY s.Start_Time",
                ps -> {
                    Sql.setDateTime(ps, 1, from);
                    Sql.setDateTime(ps, 2, to);
                });
    }

    /**
 * Tìm các buổi có khả năng xung đột với khung giờ cần kiểm tra.
 * Chỉ lấy buổi cùng phòng HOẶC cùng huấn luyện viên.
 * Bỏ qua các buổi PT đã bị hủy.
 */
public List<Session> findPotentialConflicts(
        Integer roomId,
        Integer trainerId,
        LocalDateTime start,
        LocalDateTime end) {

    String sql = selectSql()
            + " WHERE (s.Room_ID = ? OR s.Trainer_ID = ?)"
            + " AND s.Start_Time < ?"
            + " AND s.End_Time > ?"
            + " AND (s.Session_Type <> 'PT' OR p.Booking_Status IS NULL"
            + " OR p.Booking_Status <> 'Cancelled')"
            + " ORDER BY s.Start_Time";

    return queryList(sql, ps -> {
        ps.setObject(1, roomId);
        ps.setObject(2, trainerId);
        Sql.setDateTime(ps, 3, end);
        Sql.setDateTime(ps, 4, start);
    });
}

public List<Session> findRoomConflicts(
        Integer roomId,
        LocalDateTime start,
        LocalDateTime end) {

    String sql = selectSql()
            + " WHERE s.Room_ID = ?"
            + " AND s.Start_Time < ?"
            + " AND s.End_Time > ?"
            + " AND (s.Session_Type <> 'PT'"
            + " OR p.Booking_Status IS NULL"
            + " OR p.Booking_Status <> 'Cancelled')"
            + " ORDER BY s.Start_Time";

    return queryList(sql, ps -> {
        ps.setObject(1, roomId);
        Sql.setDateTime(ps, 2, end);
        Sql.setDateTime(ps, 3, start);
    });
}

public List<Session> findTrainerConflicts(
        Integer trainerId,
        LocalDateTime start,
        LocalDateTime end) {

    String sql = selectSql()
            + " WHERE s.Trainer_ID = ?"
            + " AND s.Start_Time < ?"
            + " AND s.End_Time > ?"
            + " AND (s.Session_Type <> 'PT'"
            + " OR p.Booking_Status IS NULL"
            + " OR p.Booking_Status <> 'Cancelled')"
            + " ORDER BY s.Start_Time";

    return queryList(sql, ps -> {
        ps.setObject(1, trainerId);
        Sql.setDateTime(ps, 2, end);
        Sql.setDateTime(ps, 3, start);
    });
}
}
