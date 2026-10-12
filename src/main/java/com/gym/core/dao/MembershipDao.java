package com.gym.core.dao;

import com.gym.core.model.Membership;
import com.gym.core.model.MembershipStatus;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** DAO bảng MEMBERSHIP (một lần đăng ký gói tập). */
public class MembershipDao extends AbstractJdbcDao<Membership, Integer> {

    @Override
    protected String tableName() {
        return "MEMBERSHIP";
    }

    @Override
    protected String idColumn() {
        return "Membership_ID";
    }

    @Override
    protected Membership mapRow(ResultSet rs) throws SQLException {
        Membership m = new Membership();
        m.setMembershipId(rs.getInt("Membership_ID"));
        m.setVisitorId(rs.getInt("Visitor_ID"));
        m.setPlanId(rs.getInt("Plan_ID"));
        m.setStartDate(Sql.date(rs, "Start_Date"));
        m.setEndDate(Sql.date(rs, "End_Date"));
        m.setStatus(MembershipStatus.fromDb(rs.getString("Status")));
        return m;
    }

    @Override
    public Membership insert(Membership m) {
        int id = insertAndGetKey(
                "INSERT INTO MEMBERSHIP (Visitor_ID, Plan_ID, Start_Date, End_Date, Status) "
                        + "VALUES (?, ?, ?, ?, COALESCE(?, 'Active'))",
                ps -> {
                    ps.setInt(1, m.getVisitorId());
                    ps.setInt(2, m.getPlanId());
                    Sql.setDate(ps, 3, m.getStartDate());
                    Sql.setDate(ps, 4, m.getEndDate());
                    Sql.setEnum(ps, 5, m.getStatus());
                });
        m.setMembershipId(id);
        findById(id).ifPresent(saved -> m.setStatus(saved.getStatus()));   // trạng thái mặc định của DB
        return m;
    }

    @Override
    public void update(Membership m) {
        execute("UPDATE MEMBERSHIP SET Visitor_ID = ?, Plan_ID = ?, Start_Date = ?, End_Date = ?, Status = ? "
                        + "WHERE Membership_ID = ?",
                ps -> {
                    ps.setInt(1, m.getVisitorId());
                    ps.setInt(2, m.getPlanId());
                    Sql.setDate(ps, 3, m.getStartDate());
                    Sql.setDate(ps, 4, m.getEndDate());
                    Sql.setEnum(ps, 5, m.getStatus());
                    ps.setInt(6, m.getMembershipId());
                });
    }

    /** Lịch sử gói của một hội viên, mới nhất trước. */
    public List<Membership> findByVisitor(int visitorId) {
        return queryList(selectSql() + " WHERE Visitor_ID = ? ORDER BY Start_Date DESC, Membership_ID DESC",
                ps -> ps.setInt(1, visitorId));
    }

    /** Gói còn hiệu lực (Active) có ngày kết thúc trong khoảng [from, to]. Dùng cho "gói sắp hết hạn". */
    public List<Membership> findActiveEndingBetween(LocalDate from, LocalDate to) {
        return queryList(selectSql() + " WHERE Status = 'Active' AND End_Date BETWEEN ? AND ? ORDER BY End_Date",
                ps -> {
                    Sql.setDate(ps, 1, from);
                    Sql.setDate(ps, 2, to);
                });
    }
}
