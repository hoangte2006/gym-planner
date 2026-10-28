package com.gym.core.dao;

import com.gym.core.model.ClassSession;
import com.gym.core.model.Equipment;
import com.gym.core.model.EquipmentStatus;
import com.gym.core.model.PtSession;
import com.gym.core.model.SessionEquipmentUsage;
import com.gym.core.model.Session;
import com.gym.core.model.SessionType;
import com.gym.core.DBConnection;
import com.gym.core.exception.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    public static void fillBase(Session s, ResultSet rs) throws SQLException {
        s.setSessionId(rs.getInt("Session_ID"));
        s.setSessionType(SessionType.fromDb(rs.getString("Session_Type")));
        s.setRoomId(rs.getInt("Room_ID"));
        s.setTrainerId(rs.getInt("Trainer_ID"));
        s.setStartTime(Sql.dateTime(rs, "Start_Time"));
        s.setEndTime(Sql.dateTime(rs, "End_Time"));
    }

    /** INSERT vào bảng SESSION trên kết nối có sẵn, trả về Session_ID mới. Dùng trong transaction của DAO con. */
    public static int insertSessionRow(Connection cn, Session s, SessionType type) throws SQLException {
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
    public static void updateSessionRow(Connection cn, Session s) throws SQLException {
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

    public List<Session> findTrainerStartingBetween(
            int trainerId, LocalDateTime from, LocalDateTime to) {
        return queryList(selectSql()
                        + " WHERE s.Trainer_ID = ? AND s.Start_Time >= ? AND s.Start_Time < ?"
                        + " ORDER BY s.Start_Time",
                ps -> {
                    ps.setInt(1, trainerId);
                    Sql.setDateTime(ps, 2, from);
                    Sql.setDateTime(ps, 3, to);
                });
    }

    public List<Session> findByTrainer(int trainerId) {
        return queryList(selectSql() + " WHERE s.Trainer_ID = ? ORDER BY s.Start_Time",
                ps -> ps.setInt(1, trainerId));
    }

public List<Equipment> findAvailableEquipment(
        Integer roomId,
        LocalDateTime from,
        LocalDateTime to,
        Integer exceptSessionId) {
    if (roomId == null || from == null || to == null || !to.isAfter(from)) {
        throw new IllegalArgumentException("Phòng và khoảng thời gian đặt thiết bị không hợp lệ.");
    }

    String excludeSession = exceptSessionId == null ? "" : "AND u.Session_ID <> ? ";
    String sql = "SELECT e.Equipment_ID, e.Equipment_Name, e.Room_ID, "
            + "e.Operational_Status, e.Purchase_Date FROM EQUIPMENT e "
            + "WHERE e.Operational_Status = 'Active' AND (e.Room_ID IS NULL OR e.Room_ID = ?) "
            + "AND NOT EXISTS (SELECT 1 FROM SESSION_EQUIPMENT_USAGE u "
            + "LEFT JOIN PT_BOOKING p ON p.Session_ID = u.Session_ID "
            + "WHERE u.Equipment_ID = e.Equipment_ID "
            + "AND u.Reserved_From < ? AND u.Reserved_To > ? "
            + excludeSession
            + "AND (p.Session_ID IS NULL OR p.Booking_Status <> 'Cancelled')) "
            + "ORDER BY e.Equipment_ID";
    try (Connection cn = DBConnection.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql)) {
        ps.setInt(1, roomId);
        Sql.setDateTime(ps, 2, to);
        Sql.setDateTime(ps, 3, from);
        if (exceptSessionId != null) ps.setInt(4, exceptSessionId);
        try (ResultSet rs = ps.executeQuery()) {
            List<Equipment> result = new ArrayList<>();
            while (rs.next()) {
                Equipment equipment = new Equipment();
                equipment.setEquipmentId(rs.getInt("Equipment_ID"));
                equipment.setEquipmentName(rs.getString("Equipment_Name"));
                equipment.setRoomId(Sql.integer(rs, "Room_ID"));
                equipment.setOperationalStatus(
                        EquipmentStatus.fromDb(rs.getString("Operational_Status")));
                equipment.setPurchaseDate(Sql.date(rs, "Purchase_Date"));
                result.add(equipment);
            }
            return List.copyOf(result);
        }
    } catch (SQLException e) {
        throw new DataAccessException("Không tải được danh sách thiết bị khả dụng.", e);
    }
}

public SessionEquipmentUsage reserveEquipment(
        int equipmentId,
        int sessionId,
        LocalDateTime from,
        LocalDateTime to) {
    if (from == null || to == null || !to.isAfter(from)) {
        throw new IllegalArgumentException("Khoảng thời gian đặt thiết bị không hợp lệ.");
    }
    return inTransaction(cn -> {
        try (PreparedStatement ps = cn.prepareStatement(
                "SELECT e.Operational_Status, e.Room_ID, s.Room_ID AS Session_Room, "
                        + "s.Start_Time, s.End_Time, p.Booking_Status "
                        + "FROM EQUIPMENT e WITH (UPDLOCK, HOLDLOCK) "
                        + "CROSS JOIN [SESSION] s LEFT JOIN PT_BOOKING p ON p.Session_ID = s.Session_ID "
                        + "WHERE e.Equipment_ID = ? AND s.Session_ID = ?")) {
            ps.setInt(1, equipmentId);
            ps.setInt(2, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Không tìm thấy thiết bị hoặc buổi tập.");
                }
                if (!EquipmentStatus.ACTIVE.getDbValue().equals(rs.getString("Operational_Status"))) {
                    throw new IllegalStateException("Thiết bị đang hỏng hoặc bảo trì, không thể đặt.");
                }
                String bookingStatus = rs.getString("Booking_Status");
                if ("Cancelled".equals(bookingStatus) || "Completed".equals(bookingStatus)) {
                    throw new IllegalStateException("Buổi PT không còn ở trạng thái có thể đặt thiết bị.");
                }
                int equipmentRoom = rs.getInt("Room_ID");
                boolean equipmentRoomIsNull = rs.wasNull();
                int sessionRoom = rs.getInt("Session_Room");
                LocalDateTime sessionStart = Sql.dateTime(rs, "Start_Time");
                LocalDateTime sessionEnd = Sql.dateTime(rs, "End_Time");
                if (!equipmentRoomIsNull && equipmentRoom != sessionRoom) {
                    throw new IllegalArgumentException("Thiết bị không thuộc phòng của buổi tập.");
                }
                if (from.isBefore(sessionStart) || to.isAfter(sessionEnd)) {
                    throw new IllegalArgumentException(
                            "Thời gian đặt thiết bị phải nằm trong thời gian buổi tập.");
                }
            }
        }

        int conflicts = queryInt(cn,
                "SELECT COUNT(*) FROM SESSION_EQUIPMENT_USAGE WITH (UPDLOCK, HOLDLOCK) "
                        + "WHERE Equipment_ID = ? AND Reserved_From < ? AND Reserved_To > ? "
                        + "AND NOT EXISTS "
                        + "(SELECT 1 FROM PT_BOOKING p WHERE p.Session_ID = SESSION_EQUIPMENT_USAGE.Session_ID "
                        + "AND p.Booking_Status = 'Cancelled')",
                ps -> {
                    ps.setInt(1, equipmentId);
                    Sql.setDateTime(ps, 2, to);
                    Sql.setDateTime(ps, 3, from);
                });
        if (conflicts > 0) {
            throw new IllegalStateException("Thiết bị đã được buổi khác đặt trong khung giờ này.");
        }

        int id = insertKey(cn,
                "INSERT INTO SESSION_EQUIPMENT_USAGE "
                        + "(Equipment_ID, Session_ID, Reserved_From, Reserved_To) VALUES (?, ?, ?, ?)",
                ps -> {
                    ps.setInt(1, equipmentId);
                    ps.setInt(2, sessionId);
                    Sql.setDateTime(ps, 3, from);
                    Sql.setDateTime(ps, 4, to);
                });
        SessionEquipmentUsage usage = new SessionEquipmentUsage();
        usage.setUsageId(id);
        usage.setEquipmentId(equipmentId);
        usage.setSessionId(sessionId);
        usage.setReservedFrom(from);
        usage.setReservedTo(to);
        return usage;
    });
}

public boolean hasConflictingEquipmentReservations(int sessionId) {
    return count("SELECT COUNT(*) FROM SESSION_EQUIPMENT_USAGE mine "
                    + "JOIN SESSION_EQUIPMENT_USAGE theirs "
                    + "ON theirs.Equipment_ID = mine.Equipment_ID "
                    + "AND theirs.Usage_ID <> mine.Usage_ID "
                    + "LEFT JOIN PT_BOOKING p ON p.Session_ID = theirs.Session_ID "
                    + "WHERE mine.Session_ID = ? "
                    + "AND mine.Reserved_From < theirs.Reserved_To "
                    + "AND theirs.Reserved_From < mine.Reserved_To "
                    + "AND (p.Session_ID IS NULL OR p.Booking_Status <> 'Cancelled')",
            ps -> ps.setInt(1, sessionId)) > 0;
}

public void registerClassSeat(int sessionId, int visitorId) {
    inTransaction(cn -> {
        int capacity;
        try (PreparedStatement ps = cn.prepareStatement(
                "SELECT c.Max_Capacity FROM CLASS c WITH (UPDLOCK, HOLDLOCK) "
                        + "WHERE c.Session_ID = ?")) {
            ps.setInt(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Lớp tập không tồn tại.");
                }
                capacity = rs.getInt(1);
            }
        }

        String existingStatus = null;
        try (PreparedStatement ps = cn.prepareStatement(
                "SELECT Attendance_Status FROM CLASS_REGISTRATION "
                        + "WHERE Session_ID = ? AND Visitor_ID = ?")) {
            ps.setInt(1, sessionId);
            ps.setInt(2, visitorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    existingStatus = rs.getString(1);
                }
            }
        }

        if (existingStatus != null && !"Cancelled".equals(existingStatus)) {
            throw new IllegalArgumentException("Hội viên đã đăng ký lớp này.");
        }
        int registered = queryInt(cn,
                "SELECT COUNT(*) FROM CLASS_REGISTRATION WHERE Session_ID = ? "
                        + "AND Attendance_Status <> 'Cancelled'",
                ps -> ps.setInt(1, sessionId));
        if (registered >= capacity) {
            throw new IllegalStateException("Lớp đã đủ sĩ số.");
        }
        if (existingStatus != null) {
            exec(cn, "UPDATE CLASS_REGISTRATION SET Registration_Date = CURRENT_TIMESTAMP, "
                            + "Attendance_Status = 'Confirmed' WHERE Session_ID = ? AND Visitor_ID = ?",
                    ps -> {
                        ps.setInt(1, sessionId);
                        ps.setInt(2, visitorId);
                    });
            return null;
        }
        exec(cn, "INSERT INTO CLASS_REGISTRATION (Session_ID, Visitor_ID, Registration_Date, "
                        + "Attendance_Status) VALUES (?, ?, CURRENT_TIMESTAMP, 'Confirmed')",
                ps -> {
                    ps.setInt(1, sessionId);
                    ps.setInt(2, visitorId);
                });
        return null;
    });
}
}
