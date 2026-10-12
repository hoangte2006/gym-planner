package com.gym.core.dao;

import com.gym.core.model.SessionEquipmentUsage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** DAO bảng SESSION_EQUIPMENT_USAGE (đặt thiết bị cho một buổi tập). */
public class SessionEquipmentUsageDao extends AbstractJdbcDao<SessionEquipmentUsage, Integer> {

    @Override
    protected String tableName() {
        return "SESSION_EQUIPMENT_USAGE";
    }

    @Override
    protected String idColumn() {
        return "Usage_ID";
    }

    @Override
    protected SessionEquipmentUsage mapRow(ResultSet rs) throws SQLException {
        SessionEquipmentUsage u = new SessionEquipmentUsage();
        u.setUsageId(rs.getInt("Usage_ID"));
        u.setEquipmentId(rs.getInt("Equipment_ID"));
        u.setSessionId(rs.getInt("Session_ID"));
        u.setReservedFrom(Sql.dateTime(rs, "Reserved_From"));
        u.setReservedTo(Sql.dateTime(rs, "Reserved_To"));
        return u;
    }

    @Override
    public SessionEquipmentUsage insert(SessionEquipmentUsage u) {
        int id = insertAndGetKey(
                "INSERT INTO SESSION_EQUIPMENT_USAGE (Equipment_ID, Session_ID, Reserved_From, Reserved_To) "
                        + "VALUES (?, ?, ?, ?)",
                ps -> {
                    ps.setInt(1, u.getEquipmentId());
                    ps.setInt(2, u.getSessionId());
                    Sql.setDateTime(ps, 3, u.getReservedFrom());
                    Sql.setDateTime(ps, 4, u.getReservedTo());
                });
        u.setUsageId(id);
        return u;
    }

    @Override
    public void update(SessionEquipmentUsage u) {
        execute("UPDATE SESSION_EQUIPMENT_USAGE SET Equipment_ID = ?, Session_ID = ?, Reserved_From = ?, "
                        + "Reserved_To = ? WHERE Usage_ID = ?",
                ps -> {
                    ps.setInt(1, u.getEquipmentId());
                    ps.setInt(2, u.getSessionId());
                    Sql.setDateTime(ps, 3, u.getReservedFrom());
                    Sql.setDateTime(ps, 4, u.getReservedTo());
                    ps.setInt(5, u.getUsageId());
                });
    }

    public List<SessionEquipmentUsage> findBySession(int sessionId) {
        return queryList(selectSql() + " WHERE Session_ID = ? ORDER BY Usage_ID", ps -> ps.setInt(1, sessionId));
    }

    public List<SessionEquipmentUsage> findByEquipment(int equipmentId) {
        return queryList(selectSql() + " WHERE Equipment_ID = ? ORDER BY Reserved_From",
                ps -> ps.setInt(1, equipmentId));
    }
}
