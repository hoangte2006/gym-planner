package com.gym.core.dao;

import com.gym.core.model.Equipment;
import com.gym.core.model.EquipmentStatus;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** DAO bảng EQUIPMENT (thiết bị). */
public class EquipmentDao extends AbstractJdbcDao<Equipment, Integer> {

    @Override
    protected String tableName() {
        return "EQUIPMENT";
    }

    @Override
    protected String idColumn() {
        return "Equipment_ID";
    }

    @Override
    protected Equipment mapRow(ResultSet rs) throws SQLException {
        Equipment e = new Equipment();
        e.setEquipmentId(rs.getInt("Equipment_ID"));
        e.setEquipmentName(rs.getString("Equipment_Name"));
        e.setRoomId(Sql.integer(rs, "Room_ID"));                 // cho phép NULL
        e.setOperationalStatus(EquipmentStatus.fromDb(rs.getString("Operational_Status")));
        e.setPurchaseDate(Sql.date(rs, "Purchase_Date"));        // cho phép NULL
        return e;
    }

    @Override
    public Equipment insert(Equipment e) {
        int id = insertAndGetKey(
                "INSERT INTO EQUIPMENT (Equipment_Name, Room_ID, Operational_Status, Purchase_Date) "
                        + "VALUES (?, ?, COALESCE(?, 'Active'), ?)",
                ps -> {
                    ps.setString(1, e.getEquipmentName());
                    Sql.setInteger(ps, 2, e.getRoomId());
                    Sql.setEnum(ps, 3, e.getOperationalStatus());
                    Sql.setDate(ps, 4, e.getPurchaseDate());
                });
        e.setEquipmentId(id);
        findById(id).ifPresent(saved -> e.setOperationalStatus(saved.getOperationalStatus()));   // mặc định của DB
        return e;
    }

    @Override
    public void update(Equipment e) {
        execute("UPDATE EQUIPMENT SET Equipment_Name = ?, Room_ID = ?, Operational_Status = ?, Purchase_Date = ? "
                        + "WHERE Equipment_ID = ?",
                ps -> {
                    ps.setString(1, e.getEquipmentName());
                    Sql.setInteger(ps, 2, e.getRoomId());
                    Sql.setEnum(ps, 3, e.getOperationalStatus());
                    Sql.setDate(ps, 4, e.getPurchaseDate());
                    ps.setInt(5, e.getEquipmentId());
                });
    }

    public List<Equipment> findByStatus(EquipmentStatus status) {
        return queryList(selectSql() + " WHERE Operational_Status = ? ORDER BY Equipment_ID",
                ps -> ps.setString(1, status.getDbValue()));
    }

    public List<Equipment> findByRoom(int roomId) {
        return queryList(selectSql() + " WHERE Room_ID = ? ORDER BY Equipment_ID", ps -> ps.setInt(1, roomId));
    }

}
