package com.gym.dashboard.dao;

import com.gym.core.DBConnection;
import com.gym.core.dao.Sql;
import com.gym.core.dao.StatementBinder;
import com.gym.core.exception.DataAccessException;
import com.gym.core.model.EquipmentStatus;
import com.gym.dashboard.model.EquipmentStatusCount;
import com.gym.dashboard.model.MonthlyRevenue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Truy vấn thống kê cho Dashboard. Chỉ ĐỌC dữ liệu, không ghi.
 * Mỗi chỉ số là MỘT câu truy vấn tổng hợp riêng (COUNT/SUM/GROUP BY), dùng dấu ? cho mọi giá trị.
 *
 * Cách làm một chỉ số mới (làm theo thứ tự):
 *   1. Gõ và chạy thử câu SQL trong SSMS cho ra đúng số.
 *   2. Chép vào đây, thay giá trị cụ thể (ngày, trạng thái...) bằng ?.
 *   3. Viết một hàm như countActiveMembers bên dưới và gọi queryNumber.
 */
public class DashboardDao {

    /** Thẻ 1: số hội viên đang có gói Active còn hạn (mỗi hội viên chỉ đếm một lần). */
    public int countActiveMembers(LocalDate today) {
        String sql = "SELECT COUNT(DISTINCT Visitor_ID) FROM MEMBERSHIP "
                + "WHERE Status = 'Active' AND End_Date >= ?";
        return (int) queryNumber(sql, ps -> Sql.setDate(ps, 1, today));
    }

    /**
     * Thẻ 2: doanh thu tháng này = tổng tiền các hóa đơn đã thanh toán (Paid) có NGÀY THANH TOÁN trong tháng
     * của "today" (từ ngày 1 đến hết tháng). Không có hóa đơn nào thì trả về 0.
     */
    public BigDecimal sumMonthlyRevenue(LocalDate today) {
        LocalDate firstDay = today.withDayOfMonth(1);
        String sql = "SELECT SUM(Amount_Paid) FROM BILLING_LOG "
                + "WHERE Billing_Status = 'Paid' AND Payment_Date >= ? AND Payment_Date < ?";
        return queryDecimal(sql, ps -> {
            Sql.setDate(ps, 1, firstDay);                    // đầu tháng này
            Sql.setDate(ps, 2, firstDay.plusMonths(1));      // đầu tháng sau (không gồm)
        });
    }

    /** Thẻ 3: số gói Active hết hạn từ hôm nay đến hôm nay + days (gồm cả ngày cuối). */
    public int countExpiringMemberships(LocalDate today, int days) {
        String sql = "SELECT COUNT(*) FROM MEMBERSHIP "
                + "WHERE Status = 'Active' AND End_Date >= ? AND End_Date <= ?";
        return (int) queryNumber(sql, ps -> {
            Sql.setDate(ps, 1, today);
            Sql.setDate(ps, 2, today.plusDays(days));
        });
    }

    /**
     * Thẻ 4: số lớp học (Session_Type = 'Class') bắt đầu trong ngày "today". Buổi PT không tính.
     * Lọc theo khoảng [đầu ngày, đầu ngày hôm sau) để dùng được chỉ mục theo thời gian.
     */
    public int countClassToday(LocalDate today) {
        String sql = "SELECT COUNT(*) FROM SESSION "
                + "WHERE Session_Type = 'Class' AND Start_Time >= ? AND Start_Time < ?";
        return (int) queryNumber(sql, ps -> {
            Sql.setDateTime(ps, 1, today.atStartOfDay());
            Sql.setDateTime(ps, 2, today.plusDays(1).atStartOfDay());
        });
    }

    /**
     * Biểu đồ 1: doanh thu từng tháng (tổng Amount_Paid của hóa đơn Paid, theo NGÀY THANH TOÁN) trong "months"
     * tháng gần nhất tính đến tháng của "today" (gồm cả tháng này). Sắp xếp từ cũ đến mới.
     * Tháng không có hóa đơn Paid nào thì KHÔNG có dòng (Service bổ sung số 0 cho đủ tháng).
     */
    public List<MonthlyRevenue> sumRevenueByMonth(LocalDate today, int months) {
        LocalDate from = today.withDayOfMonth(1).minusMonths(months - 1L);   // đầu tháng của tháng cũ nhất
        String sql = "SELECT YEAR(Payment_Date), MONTH(Payment_Date), SUM(Amount_Paid) FROM BILLING_LOG "
                + "WHERE Billing_Status = 'Paid' AND Payment_Date >= ? "
                + "GROUP BY YEAR(Payment_Date), MONTH(Payment_Date) "
                + "ORDER BY YEAR(Payment_Date), MONTH(Payment_Date)";
        return queryList(sql, ps -> Sql.setDate(ps, 1, from),
                rs -> new MonthlyRevenue(YearMonth.of(rs.getInt(1), rs.getInt(2)), rs.getBigDecimal(3)));
    }

    /**
     * Biểu đồ 2: số thiết bị theo trạng thái. Trả về NHIỀU DÒNG (mỗi trạng thái đang có thiết bị một dòng);
     * trạng thái không có thiết bị nào thì không xuất hiện.
     */
    public List<EquipmentStatusCount> countEquipmentByStatus() {
        String sql = "SELECT Operational_Status, COUNT(*) FROM EQUIPMENT "
                + "GROUP BY Operational_Status ORDER BY Operational_Status";
        return queryList(sql, StatementBinder.NONE,
                rs -> new EquipmentStatusCount(EquipmentStatus.fromDb(rs.getString(1)), rs.getInt(2)));
    }

    // ---------- Công cụ chạy truy vấn (dùng chung cho các chỉ số) ----------

    /** Chạy câu SELECT cho đúng một con số (COUNT, SUM...). NULL (ví dụ SUM của tập rỗng) tính là 0. */
    private long queryNumber(String sql, StatementBinder binder) {
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;     // getLong trả 0 khi cột NULL
            }
        } catch (SQLException e) {
            throw new DataAccessException("Không đọc được số liệu thống kê", e);
        }
    }

    /** Chạy câu SELECT cho đúng một số TIỀN (SUM...). NULL (tập rỗng) tính là 0. Dùng BigDecimal, không dùng double. */
    private BigDecimal queryDecimal(String sql, StatementBinder binder) {
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                BigDecimal value = rs.next() ? rs.getBigDecimal(1) : null;
                return value == null ? BigDecimal.ZERO : value;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Không đọc được số liệu thống kê", e);
        }
    }

    /** Đổi MỘT dòng kết quả của câu SELECT thành một đối tượng (dùng cho các chỉ số trả về nhiều dòng). */
    @FunctionalInterface
    private interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    /** Chạy câu SELECT trả về NHIỀU DÒNG, mỗi dòng đổi bằng mapper. Dùng cho biểu đồ (GROUP BY). */
    private <T> List<T> queryList(String sql, StatementBinder binder, RowMapper<T> mapper) {
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(mapper.map(rs));
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Không đọc được số liệu thống kê", e);
        }
    }
}
