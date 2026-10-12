package com.gym.core.dao;

import com.gym.core.model.MembershipPlan;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/** DAO bảng MEMBERSHIP_PLAN (gói tập). */
public class MembershipPlanDao extends AbstractJdbcDao<MembershipPlan, Integer> {

    @Override
    protected String tableName() {
        return "MEMBERSHIP_PLAN";
    }

    @Override
    protected String idColumn() {
        return "Plan_ID";
    }

    @Override
    protected MembershipPlan mapRow(ResultSet rs) throws SQLException {
        MembershipPlan p = new MembershipPlan();
        p.setPlanId(rs.getInt("Plan_ID"));
        p.setPlanName(rs.getString("Plan_Name"));
        p.setDurationMonths(rs.getInt("Duration_Months"));
        p.setPrice(rs.getBigDecimal("Price"));
        p.setDescription(rs.getString("Description"));
        return p;
    }

    @Override
    public MembershipPlan insert(MembershipPlan plan) {
        int id = insertAndGetKey(
                "INSERT INTO MEMBERSHIP_PLAN (Plan_Name, Duration_Months, Price, Description) VALUES (?, ?, ?, ?)",
                ps -> {
                    ps.setString(1, plan.getPlanName());
                    ps.setInt(2, plan.getDurationMonths());
                    Sql.setDecimal(ps, 3, plan.getPrice());
                    ps.setString(4, plan.getDescription());
                });
        plan.setPlanId(id);
        return plan;
    }

    @Override
    public void update(MembershipPlan plan) {
        execute("UPDATE MEMBERSHIP_PLAN SET Plan_Name = ?, Duration_Months = ?, Price = ?, Description = ? "
                        + "WHERE Plan_ID = ?",
                ps -> {
                    ps.setString(1, plan.getPlanName());
                    ps.setInt(2, plan.getDurationMonths());
                    Sql.setDecimal(ps, 3, plan.getPrice());
                    ps.setString(4, plan.getDescription());
                    ps.setInt(5, plan.getPlanId());
                });
    }

    public Optional<MembershipPlan> findByName(String planName) {
        return queryOne(selectSql() + " WHERE Plan_Name = ?", ps -> ps.setString(1, planName));
    }
}
