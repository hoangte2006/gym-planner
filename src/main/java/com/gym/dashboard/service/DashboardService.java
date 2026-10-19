package com.gym.dashboard.service;

import com.gym.app.Session;
import com.gym.core.exception.BusinessException;
import com.gym.core.model.StaffRole;
import com.gym.dashboard.dao.DashboardDao;
import com.gym.dashboard.model.EquipmentStatusCount;
import com.gym.dashboard.model.MonthlyRevenue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Luật nghiệp vụ của Dashboard. Mỗi hàm lấy số liệu đều kiểm tra quyền trước
 * (Quản lý và Lễ tân được xem,
 * Huấn luyện viên không), vì menu ẩn mục chỉ là lớp ngoài, Service phải tự chặn
 * khi bị gọi sai.
 * Không có javax.swing ở đây.
 */
public class DashboardService {
    public static final int EXPIRING_DAYS = 7; // số ngày để tính gói sắp hết hạn
    public static final int REVENUE_CHART_MONTHS = 12; // số tháng hiện trên biểu đồ doanh thu
    private final DashboardDao dao;

    public DashboardService() {
        this(new DashboardDao());
    }

    public DashboardService(DashboardDao dao) {
        this.dao = dao;
    }

    /** Thẻ 1: số hội viên đang hoạt động hôm nay. */
    public int activeMembers() {
        requireAccess(Session.getRole());
        return dao.countActiveMembers(LocalDate.now());
    }

    /** Thẻ 2: doanh thu tháng này (tổng tiền hóa đơn Paid trong tháng). Tiền nên dùng BigDecimal. */
    public BigDecimal monthlyRevenue() {
        requireFinanceAccess(Session.getRole());
        return dao.sumMonthlyRevenue(LocalDate.now());
    }

    /** Thẻ 3: số gói sắp hết hạn trong 7 ngày. */
    public int expiringMemberships() {
        requireAccess(Session.getRole());
        return dao.countExpiringMemberships(LocalDate.now(), EXPIRING_DAYS);
    }

    /** Thẻ 4: số lớp học hôm nay (không tính buổi PT). */
    public int classToday() {
        requireAccess(Session.getRole());
        return dao.countClassToday(LocalDate.now());
    }

    /** Biểu đồ 1: doanh thu 12 tháng gần nhất. Luôn đủ 12 cột liên tiếp; tháng không có doanh thu là 0. */
    public List<MonthlyRevenue> monthlyRevenueChart() {
        requireFinanceAccess(Session.getRole());
        LocalDate today = LocalDate.now();
        return fillMissingMonths(dao.sumRevenueByMonth(today, REVENUE_CHART_MONTHS),
                YearMonth.from(today), REVENUE_CHART_MONTHS);
    }

    /**
     * Bổ sung các tháng thiếu bằng 0 để luôn có đúng "months" tháng liên tiếp, kết thúc ở "last", sắp từ cũ đến mới.
     * (GROUP BY không trả dòng cho tháng không có hóa đơn nên biểu đồ sẽ bị "nhảy" tháng nếu không bổ sung.)
     * Là hàm thuần (không đụng DB) nên test được dễ.
     */
    static List<MonthlyRevenue> fillMissingMonths(List<MonthlyRevenue> rows, YearMonth last, int months) {
        Map<YearMonth, BigDecimal> byMonth = new HashMap<>();
        for (MonthlyRevenue row : rows) {
            byMonth.put(row.month(), row.revenue());
        }
        List<MonthlyRevenue> result = new ArrayList<>();
        for (int back = months - 1; back >= 0; back--) {
            YearMonth month = last.minusMonths(back);
            result.add(new MonthlyRevenue(month, byMonth.getOrDefault(month, BigDecimal.ZERO)));
        }
        return result;
    }

    /** Biểu đồ 2: số thiết bị theo trạng thái (mỗi trạng thái đang có thiết bị một dòng). */
    public List<EquipmentStatusCount> equipmentByStatus() {
        requireAccess(Session.getRole());
        return dao.countEquipmentByStatus();
    }

    /**
     * Quản lý và Lễ tân được xem Dashboard; Huấn luyện viên (hoặc chưa đăng nhập)
     * thì không.
     */
    public static void requireAccess(StaffRole role) {
        if (role != StaffRole.MANAGER && role != StaffRole.RECEPTIONIST) {
            throw new BusinessException("Bạn không có quyền xem Dashboard.");
        }
    }

    /** Số liệu tài chính (doanh thu) chỉ dành cho Quản lý; Lễ tân chỉ xem số liệu cơ bản. */
    public static void requireFinanceAccess(StaffRole role) {
        if (role != StaffRole.MANAGER) {
            throw new BusinessException("Chỉ Quản lý mới được xem doanh thu.");
        }
    }
}
