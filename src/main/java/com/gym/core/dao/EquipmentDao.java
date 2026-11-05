package com.gym.core.dao;
import com.gym.core.exception.DataAccessException;
import com.gym.core.model.Equipment;
import com.gym.core.model.EquipmentStatus;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * DAO bảng EQUIPMENT (thiết bị).
 */
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
        e.setOperationalStatus(
                EquipmentStatus.fromDb(rs.getString("Operational_Status"))
        );
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

        findById(id).ifPresent(saved ->
                e.setOperationalStatus(saved.getOperationalStatus())
        );

        return e;
    }

    @Override
    public void update(Equipment e) {
        execute(
                "UPDATE EQUIPMENT SET Equipment_Name = ?, Room_ID = ?, "
                        + "Operational_Status = ?, Purchase_Date = ? "
                        + "WHERE Equipment_ID = ?",
                ps -> {
                    ps.setString(1, e.getEquipmentName());
                    Sql.setInteger(ps, 2, e.getRoomId());
                    Sql.setEnum(ps, 3, e.getOperationalStatus());
                    Sql.setDate(ps, 4, e.getPurchaseDate());
                    ps.setInt(5, e.getEquipmentId());
                });
    }

    /**
     * Tìm thiết bị theo mã, tên, mã phòng hoặc trạng thái.
     */
    public List<Equipment> search(String keyword) {
        String sql = selectSql()
                + " WHERE CAST(Equipment_ID AS VARCHAR(20)) LIKE ?"
                + " OR LOWER(Equipment_Name) LIKE ?"
                + " OR CAST(Room_ID AS VARCHAR(20)) LIKE ?"
                + " OR LOWER(Operational_Status) LIKE ?"
                + " ORDER BY Equipment_ID";

        String value = keyword == null
                ? ""
                : keyword.trim().toLowerCase(Locale.ROOT);

        String pattern = "%" + value + "%";

        return queryList(sql, ps -> {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            ps.setString(4, pattern);
        });
    }

    public List<Equipment> findByStatus(EquipmentStatus status) {
        return queryList(
                selectSql()
                        + " WHERE Operational_Status = ? ORDER BY Equipment_ID",
                ps -> ps.setString(1, status.getDbValue())
        );
    }

    public List<Equipment> findByRoom(int roomId) {
        return queryList(
                selectSql()
                        + " WHERE Room_ID = ? ORDER BY Equipment_ID",
                ps -> ps.setInt(1, roomId)
        );
    }

    // =========================================================
    // CÁC PHƯƠNG THỨC BỔ SUNG CHO MÀN HÌNH SỨC KHỎE THIẾT BỊ
    // =========================================================

    /**
     * Lấy danh sách thiết bị cần cảnh báo sức khỏe theo phân trang.
     *
     * Một thiết bị được đưa vào danh sách nếu thỏa mãn ít nhất
     * một trong các điều kiện:
     *
     * 1. Operational_Status = 'Broken'.
     * 2. Chưa từng có lịch sử bảo trì.
     * 3. Lần bảo trì gần nhất cách hiện tại hơn 90 ngày.
     *
     * Dữ liệu trả về là Map để không cần tạo model mới.
     *
     * Các key trong Map:
     * equipmentId
     * equipmentName
     * roomId
     * roomName
     * operationalStatus
     * lastMaintenance
     * maintenanceCount
     * totalCost
     *
     * @param page trang hiện tại, bắt đầu từ 1
     * @param pageSize số dòng mỗi trang
     * @return một trang dữ liệu sức khỏe thiết bị
     */
    public PageResult<Map<String, Object>> findHealthWarningsPage(
            int page,
            int pageSize
    ) {
        if (page < 1 || pageSize < 1) {
            throw new IllegalArgumentException(
                    "Trang và kích thước trang phải lớn hơn 0."
            );
        }

        long totalItems = countHealthWarnings();

        List<Map<String, Object>> items =
                queryHealthWarnings(page, pageSize);

        return new PageResult<>(
                items,
                page,
                pageSize,
                totalItems
        );
    }

    /**
     * Đếm tổng số thiết bị cần cảnh báo.
     *
     * GROUP BY đảm bảo mỗi thiết bị chỉ được đếm một lần,
     * kể cả khi thiết bị có nhiều bản ghi bảo trì.
     */
    private long countHealthWarnings() {
        String sql = """
                SELECT COUNT(*)
                FROM (
                    SELECT e.Equipment_ID
                    FROM dbo.EQUIPMENT e
                    LEFT JOIN dbo.MAINTENANCE_LOG ml
                        ON ml.Equipment_ID = e.Equipment_ID
                    GROUP BY
                        e.Equipment_ID,
                        e.Operational_Status
                    HAVING
                        e.Operational_Status = 'Broken'
                        OR MAX(ml.Maintenance_Date) IS NULL
                        OR DATEDIFF(
                            DAY,
                            MAX(ml.Maintenance_Date),
                            CAST(GETDATE() AS DATE)
                        ) > 90
                ) AS HealthWarnings
                """;

        return count(sql, ps -> {
            // Không có tham số đầu vào.
        });
    }

    /**
     * Truy vấn dữ liệu sức khỏe thiết bị của một trang.
     *
     * JOIN ROOM để lấy tên phòng.
     * JOIN MAINTENANCE_LOG để thống kê lịch sử bảo trì.
     */
    private List<Map<String, Object>> queryHealthWarnings(
            int page,
            int pageSize
    ) {
        String sql = """
                SELECT
                    e.Equipment_ID,
                    e.Equipment_Name,
                    e.Room_ID,
                    r.Room_Name,
                    e.Operational_Status,
                    MAX(ml.Maintenance_Date) AS Last_Maintenance,
                    COUNT(ml.Log_no) AS Maintenance_Count,
                    COALESCE(SUM(ml.Cost), 0.00) AS Total_Cost

                FROM dbo.EQUIPMENT e

                LEFT JOIN dbo.ROOM r
                    ON r.Room_ID = e.Room_ID

                LEFT JOIN dbo.MAINTENANCE_LOG ml
                    ON ml.Equipment_ID = e.Equipment_ID

                GROUP BY
                    e.Equipment_ID,
                    e.Equipment_Name,
                    e.Room_ID,
                    r.Room_Name,
                    e.Operational_Status

                HAVING
                    e.Operational_Status = 'Broken'
                    OR MAX(ml.Maintenance_Date) IS NULL
                    OR DATEDIFF(
                        DAY,
                        MAX(ml.Maintenance_Date),
                        CAST(GETDATE() AS DATE)
                    ) > 90

                ORDER BY e.Equipment_ID

                OFFSET ? ROWS
                FETCH NEXT ? ROWS ONLY
                """;

        try (Lease lease = Lease.open();
             PreparedStatement ps =
                     lease.connection().prepareStatement(sql)) {

            ps.setInt(1, (page - 1) * pageSize);
            ps.setInt(2, pageSize);

            try (ResultSet rs = ps.executeQuery()) {
                List<Map<String, Object>> result = new ArrayList<>();

                while (rs.next()) {
                    result.add(mapHealthRow(rs));
                }

                return result;
            }

        } catch (SQLException e) {
            throw new DataAccessException(
                    "Không thể tải dữ liệu sức khỏe thiết bị.",
                    e
            );
        }
    }

    /**
     * Ánh xạ một dòng dữ liệu sức khỏe sang Map.
     */
    private Map<String, Object> mapHealthRow(ResultSet rs)
            throws SQLException {

        Map<String, Object> row = new LinkedHashMap<>();

        row.put("equipmentId", rs.getInt("Equipment_ID"));

        row.put("equipmentName",
                rs.getString("Equipment_Name"));

        row.put("roomId",
                Sql.integer(rs, "Room_ID"));

        row.put("roomName",
                rs.getString("Room_Name"));

        row.put("operationalStatus",
                rs.getString("Operational_Status"));

        java.sql.Date maintenanceDate =
                rs.getDate("Last_Maintenance");

        LocalDate lastMaintenance = maintenanceDate == null
                ? null
                : maintenanceDate.toLocalDate();

        row.put("lastMaintenance", lastMaintenance);

        row.put("maintenanceCount",
                rs.getInt("Maintenance_Count"));

        row.put("totalCost",
                rs.getBigDecimal("Total_Cost"));

        return row;
    }
}
