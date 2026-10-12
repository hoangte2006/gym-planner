package com.gym.core.dao;

import com.gym.core.model.BookingStatus;
import com.gym.core.model.PtSession;
import com.gym.core.model.SessionType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** DAO cho PtSession (buổi PT): nằm ở hai bảng SESSION và PT_BOOKING. Loại buổi luôn là PT. */
public class PtSessionDao extends AbstractJdbcDao<PtSession, Integer> {

    static final String FROM = "[SESSION] s JOIN PT_BOOKING p ON p.Session_ID = s.Session_ID";
    static final String COLUMNS = "s.Session_ID, s.Session_Type, s.Room_ID, s.Trainer_ID, s.Start_Time, s.End_Time, "
            + "p.Visitor_ID AS Member_ID, p.Booking_Status";

    @Override
    protected String tableName() {
        return "PT_BOOKING";
    }

    @Override
    protected String idColumn() {
        return "s.Session_ID";
    }

    @Override
    protected String fromClause() {
        return FROM;
    }

    @Override
    protected String selectColumns() {
        return COLUMNS;
    }

    @Override
    protected PtSession mapRow(ResultSet rs) throws SQLException {
        return map(rs);
    }

    /** Dùng chung với SessionDao (cùng tên cột). Cột con có thể NULL khi đọc qua LEFT JOIN. */
    static PtSession map(ResultSet rs) throws SQLException {
        PtSession p = new PtSession();
        SessionDao.fillBase(p, rs);
        p.setMemberId(Sql.integer(rs, "Member_ID"));
        String status = rs.getString("Booking_Status");
        if (status != null) {
            p.setBookingStatus(BookingStatus.fromDb(status));
        }
        return p;
    }

    @Override
    public PtSession insert(PtSession p) {
        int id = inTransaction(cn -> {
            int newId = SessionDao.insertSessionRow(cn, p, SessionType.PT);
            exec(cn, "INSERT INTO PT_BOOKING (Session_ID, Visitor_ID, Booking_Status) "
                    + "VALUES (?, ?, COALESCE(?, 'Scheduled'))", ps -> {
                ps.setInt(1, newId);
                ps.setInt(2, p.getMemberId());
                Sql.setEnum(ps, 3, p.getBookingStatus());
            });
            return newId;
        });
        p.setSessionId(id);
        p.setSessionType(SessionType.PT);
        findById(id).ifPresent(saved -> p.setBookingStatus(saved.getBookingStatus()));   // mặc định của DB
        return p;
    }

    @Override
    public void update(PtSession p) {
        inTransaction(cn -> {
            SessionDao.updateSessionRow(cn, p);
            exec(cn, "UPDATE PT_BOOKING SET Visitor_ID = ?, Booking_Status = COALESCE(?, Booking_Status) "
                    + "WHERE Session_ID = ?", ps -> {
                ps.setInt(1, p.getMemberId());
                Sql.setEnum(ps, 2, p.getBookingStatus());
                ps.setInt(3, p.getSessionId());
            });
            return null;
        });
    }

    /** Xóa buổi (bảng SESSION); dòng PT_BOOKING bị xóa theo nhờ ON DELETE CASCADE. */
    @Override
    public void deleteById(Integer id) {
        execute("DELETE FROM [SESSION] WHERE Session_ID = ?", ps -> ps.setInt(1, id));
    }

    /** Lịch PT của một hội viên, mới nhất trước. */
    public List<PtSession> findByMember(int visitorId) {
        return queryList(selectSql() + " WHERE p.Visitor_ID = ? ORDER BY s.Start_Time DESC",
                ps -> ps.setInt(1, visitorId));
    }
}
