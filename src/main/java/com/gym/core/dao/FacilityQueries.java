package com.gym.core.dao;

import com.gym.core.model.Equipment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Các truy vấn đọc chuyên biệt cho module cơ sở vật chất (T4-T5).
 *
 * <p>Lớp tái sử dụng ánh xạ ResultSet và các helper JDBC của EquipmentDao/
 * AbstractJdbcDao, không tạo thêm một cách kết nối database riêng.</p>
 */
public class FacilityQueries extends EquipmentDao {

    /**
     * Lấy danh sách thiết bị có phân trang (trang bắt đầu từ 1).
     */
    public PageResult<Equipment> findEquipmentPage(int page, int pageSize) {
        validatePage(page, pageSize);

        long total = count("SELECT COUNT(*) FROM EQUIPMENT", StatementBinder.NONE);
        int offset = Math.multiplyExact(page - 1, pageSize);
        List<Equipment> items = queryList(
                selectSql() + " ORDER BY Equipment_ID OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                ps -> {
                    ps.setInt(1, offset);
                    ps.setInt(2, pageSize);
                });
        return new PageResult<>(items, page, pageSize, total);
    }

    /** Lấy các thiết bị đang được gán cho một phòng, sắp xếp theo mã thiết bị. */
    public List<Equipment> findEquipmentByRoom(int roomId) {
        if (roomId <= 0) {
            throw new IllegalArgumentException("Mã phòng phải lớn hơn 0.");
        }
        return queryList(
                selectSql() + " WHERE Room_ID = ? ORDER BY Equipment_ID",
                ps -> ps.setInt(1, roomId));
    }

    /**
     * Tìm thiết bị khả dụng trong khoảng [from, to) theo quy ước khoảng thời gian.
     * Chỉ thiết bị có Operational_Status = 'Active' được trả về.
     * Một lượt đặt chồng lấn nếu Reserved_From < to AND Reserved_To > from.
     * Hai lượt đặt chỉ chạm biên (ví dụ lượt trước kết thúc đúng lúc lượt sau bắt đầu)
     * không bị coi là chồng lấn.
     */
    public List<Equipment> findAvailableEquipment(LocalDateTime from, LocalDateTime to) {
        validateTimeRange(from, to);

        String sql = selectSql()
                + " WHERE Operational_Status = 'Active'"
                + " AND NOT EXISTS ("
                + " SELECT 1 FROM SESSION_EQUIPMENT_USAGE seu"
                + " WHERE seu.Equipment_ID = EQUIPMENT.Equipment_ID"
                + " AND seu.Reserved_From < ?"
                + " AND seu.Reserved_To > ?"
                + " ) ORDER BY Equipment_ID";

        return queryList(sql, ps -> {
            Sql.setDateTime(ps, 1, to);
            Sql.setDateTime(ps, 2, from);
        });
    }

    private static void validatePage(int page, int pageSize) {
        if (page < 1 || pageSize < 1) {
            throw new IllegalArgumentException("Trang và kích thước trang phải từ 1 trở lên.");
        }
        try {
            Math.multiplyExact(page - 1, pageSize);
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("Trang yêu cầu vượt quá giới hạn.", ex);
        }
    }

    private static void validateTimeRange(LocalDateTime from, LocalDateTime to) {
        Objects.requireNonNull(from, "Thời điểm bắt đầu không được null.");
        Objects.requireNonNull(to, "Thời điểm kết thúc không được null.");
        if (!to.isAfter(from)) {
            throw new IllegalArgumentException("Thời điểm kết thúc phải sau thời điểm bắt đầu.");
        }
    }
}
