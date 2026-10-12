package com.gym.core.dao;

import com.gym.core.model.DailyPassTicket;

import java.sql.ResultSet;
import java.sql.SQLException;

/** DAO cho DailyPassTicket (vé ngày): nằm ở hai bảng VISITOR và DAILY_PASS_TICKET. */
public class DailyPassTicketDao extends AbstractJdbcDao<DailyPassTicket, Integer> {

    static final String FROM = "VISITOR v JOIN DAILY_PASS_TICKET d ON d.Visitor_ID = v.Visitor_ID";
    static final String COLUMNS = "v.Visitor_ID, v.First_Name, v.Last_Name, v.Phone, d.Visit_Date, d.Daily_Rate";

    @Override
    protected String tableName() {
        return "DAILY_PASS_TICKET";
    }

    @Override
    protected String idColumn() {
        return "v.Visitor_ID";
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
    protected DailyPassTicket mapRow(ResultSet rs) throws SQLException {
        return map(rs);
    }

    /** Dùng chung với VisitorDao (cùng tên cột). */
    static DailyPassTicket map(ResultSet rs) throws SQLException {
        DailyPassTicket t = new DailyPassTicket();
        t.setVisitorId(rs.getInt("Visitor_ID"));
        t.setFirstName(rs.getString("First_Name"));
        t.setLastName(rs.getString("Last_Name"));
        t.setPhone(rs.getString("Phone"));
        t.setVisitDate(Sql.date(rs, "Visit_Date"));
        t.setDailyRate(rs.getBigDecimal("Daily_Rate"));
        return t;
    }

    @Override
    public DailyPassTicket insert(DailyPassTicket t) {
        int id = inTransaction(cn -> {
            int newId = insertKey(cn, "INSERT INTO VISITOR (First_Name, Last_Name, Phone) VALUES (?, ?, ?)", ps -> {
                ps.setString(1, t.getFirstName());
                ps.setString(2, t.getLastName());
                ps.setString(3, t.getPhone());
            });
            exec(cn, "INSERT INTO DAILY_PASS_TICKET (Visitor_ID, Visit_Date, Daily_Rate) "
                    + "VALUES (?, COALESCE(?, CAST(GETDATE() AS DATE)), COALESCE(?, 15.00))", ps -> {
                ps.setInt(1, newId);
                Sql.setDate(ps, 2, t.getVisitDate());
                Sql.setDecimal(ps, 3, t.getDailyRate());
            });
            return newId;
        });
        t.setVisitorId(id);
        findById(id).ifPresent(saved -> {   // nhận lại giá trị mặc định do DB điền
            t.setVisitDate(saved.getVisitDate());
            t.setDailyRate(saved.getDailyRate());
        });
        return t;
    }

    @Override
    public void update(DailyPassTicket t) {
        inTransaction(cn -> {
            exec(cn, "UPDATE VISITOR SET First_Name = ?, Last_Name = ?, Phone = ? WHERE Visitor_ID = ?", ps -> {
                ps.setString(1, t.getFirstName());
                ps.setString(2, t.getLastName());
                ps.setString(3, t.getPhone());
                ps.setInt(4, t.getVisitorId());
            });
            exec(cn, "UPDATE DAILY_PASS_TICKET SET Visit_Date = COALESCE(?, Visit_Date), "
                    + "Daily_Rate = COALESCE(?, Daily_Rate) WHERE Visitor_ID = ?", ps -> {
                Sql.setDate(ps, 1, t.getVisitDate());
                Sql.setDecimal(ps, 2, t.getDailyRate());
                ps.setInt(3, t.getVisitorId());
            });
            return null;
        });
    }

    /** Xóa khách (bảng VISITOR); dòng vé bị xóa theo nhờ ON DELETE CASCADE. */
    @Override
    public void deleteById(Integer id) {
        execute("DELETE FROM VISITOR WHERE Visitor_ID = ?", ps -> ps.setInt(1, id));
    }
}
