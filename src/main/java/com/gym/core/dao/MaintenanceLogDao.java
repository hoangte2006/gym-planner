package com.gym.core.dao;

import com.gym.core.model.MaintenanceLog;
import com.gym.core.model.MaintenanceLogKey;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * DAO bảng MAINTENANCE_LOG. Khóa ghép (Equipment_ID, Log_no) nên findById và deleteById
 * nhận MaintenanceLogKey và được viết lại thay cho bản mặc định của lớp cha.
 * Log_no không do DB tự tăng: DAO tự lấy "số lớn nhất của thiết bị đó cộng 1" khi thêm.
 */
public class MaintenanceLogDao extends AbstractJdbcDao<MaintenanceLog, MaintenanceLogKey> {

    @Override
    protected String tableName() {
        return "MAINTENANCE_LOG";
    }

    @Override
    protected String idColumn() {
        return "Equipment_ID, Log_no";   // dùng để sắp xếp
    }

    @Override
    protected MaintenanceLog mapRow(ResultSet rs) throws SQLException {
        MaintenanceLog m = new MaintenanceLog();
        m.setEquipmentId(rs.getInt("Equipment_ID"));
        m.setLogNo(rs.getInt("Log_no"));
        m.setMaintenanceDate(Sql.date(rs, "Maintenance_Date"));
        m.setCost(rs.getBigDecimal("Cost"));
        m.setDescription(rs.getString("Description"));
        m.setLoggedBy(rs.getInt("Logged_By"));
        return m;
    }

    @Override
    public Optional<MaintenanceLog> findById(MaintenanceLogKey key) {
        return queryOne(selectSql() + " WHERE Equipment_ID = ? AND Log_no = ?", ps -> {
            ps.setInt(1, key.equipmentId());
            ps.setInt(2, key.logNo());
        });
    }

    @Override
    public void deleteById(MaintenanceLogKey key) {
        execute("DELETE FROM MAINTENANCE_LOG WHERE Equipment_ID = ? AND Log_no = ?", ps -> {
            ps.setInt(1, key.equipmentId());
            ps.setInt(2, key.logNo());
        });
    }

    @Override
    public MaintenanceLog insert(MaintenanceLog m) {
        return inTransaction(cn -> {
            // UPDLOCK + HOLDLOCK: hai người ghi cùng lúc không lấy trùng số
            int next = queryInt(cn,
                    "SELECT ISNULL(MAX(Log_no), 0) + 1 FROM MAINTENANCE_LOG WITH (UPDLOCK, HOLDLOCK) "
                            + "WHERE Equipment_ID = ?",
                    ps -> ps.setInt(1, m.getEquipmentId()));
            exec(cn, "INSERT INTO MAINTENANCE_LOG (Equipment_ID, Log_no, Maintenance_Date, Cost, Description, "
                            + "Logged_By) VALUES (?, ?, COALESCE(?, CAST(GETDATE() AS DATE)), COALESCE(?, 0.00), ?, ?)",
                    ps -> {
                        ps.setInt(1, m.getEquipmentId());
                        ps.setInt(2, next);
                        Sql.setDate(ps, 3, m.getMaintenanceDate());
                        Sql.setDecimal(ps, 4, m.getCost());
                        ps.setString(5, m.getDescription());
                        ps.setInt(6, m.getLoggedBy());
                    });
            m.setLogNo(next);
            return m;
        });
    }

    @Override
    public void update(MaintenanceLog m) {
        execute("UPDATE MAINTENANCE_LOG SET Maintenance_Date = ?, Cost = ?, Description = ?, Logged_By = ? "
                        + "WHERE Equipment_ID = ? AND Log_no = ?",
                ps -> {
                    Sql.setDate(ps, 1, m.getMaintenanceDate());
                    Sql.setDecimal(ps, 2, m.getCost());
                    ps.setString(3, m.getDescription());
                    ps.setInt(4, m.getLoggedBy());
                    ps.setInt(5, m.getEquipmentId());
                    ps.setInt(6, m.getLogNo());
                });
    }

    /** Nhật ký bảo trì của một thiết bị, mới nhất trước. */
    public List<MaintenanceLog> findByEquipment(int equipmentId) {
        return queryList(selectSql() + " WHERE Equipment_ID = ? ORDER BY Log_no DESC",
                ps -> ps.setInt(1, equipmentId));
    }

    /**
     * Tìm trong lịch sử bảo trì của một thiết bị.
     */
    public List<MaintenanceLog> searchByEquipment(int equipmentId, String keyword) {
        String sql = selectSql()
                + " WHERE Equipment_ID = ?"
                + " AND (CAST(Log_no AS VARCHAR(20)) LIKE ?"
                + " OR CONVERT(VARCHAR(10), Maintenance_Date, 23) LIKE ?"
                + " OR CAST(Cost AS VARCHAR(30)) LIKE ?"
                + " OR LOWER(COALESCE(Description, '')) LIKE ?"
                + " OR CAST(Logged_By AS VARCHAR(20)) LIKE ?)"
                + " ORDER BY Log_no DESC";

        String value = keyword == null ? "" : keyword.trim().toLowerCase(java.util.Locale.ROOT);
        String pattern = "%" + value + "%";

        return queryList(sql, ps -> {
            ps.setInt(1, equipmentId);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            ps.setString(4, pattern);
            ps.setString(5, pattern);
            ps.setString(6, pattern);
        });
    }

}
