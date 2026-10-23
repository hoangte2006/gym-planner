package com.gym.dashboard.service;

import com.gym.app.Session;
import com.gym.core.exception.BusinessException;
import com.gym.core.model.StaffRole;
import com.gym.dashboard.dao.DashboardDao;
import com.gym.dashboard.model.BusiestClass;
import com.gym.dashboard.model.ClassOccupancy;
import com.gym.dashboard.model.DashboardReport;
import com.gym.dashboard.model.DateRange;
import com.gym.dashboard.model.EquipmentStatusCount;
import com.gym.dashboard.model.MonthlyMaintenanceCost;
import com.gym.dashboard.model.MonthlyRevenue;
import com.gym.dashboard.model.TrainerSessionCount;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    public static final int EXPIRING_DAYS = 7; // số ngày mặc định để tính gói sắp hết hạn
    public static final List<Integer> EXPIRING_DAY_OPTIONS = List.of(7, 30); // các mốc cho phép chọn trên Dashboard
    public static final int DEFAULT_RANGE_MONTHS = 12; // khoảng lọc mặc định: 12 tháng gần nhất
    public static final int MAX_RANGE_MONTHS = 36; // khoảng lọc tối đa (để biểu đồ cột còn đọc được)
    public static final int TOP_LIMIT = 5; // số dòng của các bảng xếp hạng (top huấn luyện viên, lớp đông nhất)
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

    /** Thẻ 3: số gói sắp hết hạn trong "days" ngày tới. Chỉ nhận 7 hoặc 30 ngày (xem EXPIRING_DAY_OPTIONS). */
    public int expiringMemberships(int days) {
        requireAccess(Session.getRole());
        requireValidExpiringDays(days);
        return dao.countExpiringMemberships(LocalDate.now(), days);
    }

    /** Chỉ cho phép đúng các mốc ngày đã định, tránh truyền số bất kỳ (ví dụ âm hoặc quá lớn) xuống truy vấn. */
    static void requireValidExpiringDays(int days) {
        if (!EXPIRING_DAY_OPTIONS.contains(days)) {
            throw new BusinessException("Chỉ hỗ trợ xem gói sắp hết hạn trong " + EXPIRING_DAY_OPTIONS + " ngày.");
        }
    }

    /** Thẻ 4: số lớp học hôm nay (không tính buổi PT). */
    public int classToday() {
        requireAccess(Session.getRole());
        return dao.countClassToday(LocalDate.now());
    }

    /**
     * Biểu đồ 1: doanh thu từng tháng trong khoảng lọc. Luôn đủ các tháng liên tiếp từ tháng đầu đến tháng cuối
     * của khoảng; tháng không có doanh thu là 0.
     */
    public List<MonthlyRevenue> monthlyRevenueChart(DateRange range) {
        requireFinanceAccess(Session.getRole());
        requireValidRange(range);
        return fillMissingMonths(dao.sumRevenueByMonth(range), YearMonth.from(range.to()), range.monthCount());
    }

    /** Khoảng lọc phải có và không dài quá MAX_RANGE_MONTHS tháng (từ hơn đó biểu đồ cột quá dày để đọc). */
    static void requireValidRange(DateRange range) {
        if (range == null) {
            throw new BusinessException("Hãy chọn khoảng thời gian.");
        }
        if (range.monthCount() > MAX_RANGE_MONTHS) {
            throw new BusinessException("Khoảng thời gian tối đa " + MAX_RANGE_MONTHS + " tháng. Hãy chọn khoảng ngắn hơn.");
        }
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

    /** Biểu đồ 4: chi phí bảo trì từng tháng trong khoảng lọc. Là số liệu tài chính nên chỉ Quản lý xem. Luôn đủ các tháng. */
    public List<MonthlyMaintenanceCost> monthlyMaintenanceCostChart(DateRange range) {
        requireFinanceAccess(Session.getRole());
        requireValidRange(range);
        return fillMissingMaintenanceMonths(dao.sumMaintenanceCostByMonth(range),
                YearMonth.from(range.to()), range.monthCount());
    }

    /** Chỉ số 5: top huấn luyện viên theo số buổi dạy trong khoảng lọc. Bảng xếp hạng dành cho Quản lý. */
    public List<TrainerSessionCount> topTrainers(DateRange range) {
        requireAnalyticsAccess(Session.getRole());
        requireValidRange(range);
        return dao.topTrainersBySessions(range, TOP_LIMIT);
    }

    /** Chỉ số 6: các buổi lớp học đông nhất (nhiều người đăng ký nhất) trong khoảng lọc. Dành cho Quản lý. */
    public List<BusiestClass> busiestClasses(DateRange range) {
        requireAnalyticsAccess(Session.getRole());
        requireValidRange(range);
        return dao.busiestClasses(range, TOP_LIMIT);
    }

    /**
     * Gom toàn bộ số liệu Dashboard thành một báo cáo để xuất tệp. Quản lý được kèm doanh thu, chi phí bảo trì và
     * hai bảng xếp hạng; Lễ tân chỉ có số liệu cơ bản (giống màn hình Dashboard).
     */
    public DashboardReport buildReport(DateRange range, int expiringDays) {
        requireAccess(Session.getRole());
        requireValidExpiringDays(expiringDays);
        boolean manager = Session.hasRole(StaffRole.MANAGER);
        if (manager) {
            requireValidRange(range);
        }
        return new DashboardReport(
                LocalDateTime.now(),
                manager,
                range,
                expiringDays,
                activeMembers(),
                expiringMemberships(expiringDays),
                classToday(),
                manager ? monthlyRevenue() : BigDecimal.ZERO,
                manager ? monthlyRevenueChart(range) : List.of(),
                manager ? monthlyMaintenanceCostChart(range) : List.of(),
                manager ? topTrainers(range) : List.of(),
                manager ? busiestClasses(range) : List.of(),
                classOccupancyThisWeek(),
                equipmentByStatus());
    }

    /** Xuất báo cáo Dashboard ra tệp CSV (UTF-8 có BOM để Excel đọc đúng tiếng Việt). */
    public void exportCsv(DateRange range, int expiringDays, Path output) {
        DashboardReport report = buildReport(range, expiringDays);
        writeFile(output, () -> DashboardCsvWriter.write(report, output));
    }

    /** Xuất báo cáo Dashboard ra tệp PDF. */
    public void exportPdf(DateRange range, int expiringDays, Path output) {
        DashboardReport report = buildReport(range, expiringDays);
        writeFile(output, () -> DashboardPdfWriter.write(report, output));
    }

    private interface FileWork {
        void run() throws Exception;
    }

    /** Tạo thư mục cha nếu cần, ghi tệp, và đổi mọi lỗi ghi tệp thành thông báo dễ hiểu. */
    private static void writeFile(Path output, FileWork work) {
        if (output == null) {
            throw new BusinessException("Hãy chọn nơi lưu tệp.");
        }
        try {
            Path parent = output.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            work.run();
        } catch (Exception e) {
            throw new BusinessException("Không ghi được tệp. Hãy kiểm tra quyền ghi của thư mục, "
                    + "và đóng tệp nếu đang mở bằng chương trình khác (ví dụ Excel).", e);
        }
    }

    /** Giống fillMissingMonths của doanh thu: bổ sung tháng không có phiếu bảo trì bằng 0, từ cũ đến mới. */
    static List<MonthlyMaintenanceCost> fillMissingMaintenanceMonths(List<MonthlyMaintenanceCost> rows,
                                                                      YearMonth last, int months) {
        Map<YearMonth, BigDecimal> byMonth = new HashMap<>();
        for (MonthlyMaintenanceCost row : rows) {
            byMonth.put(row.month(), row.cost());
        }
        List<MonthlyMaintenanceCost> result = new ArrayList<>();
        for (int back = months - 1; back >= 0; back--) {
            YearMonth month = last.minusMonths(back);
            result.add(new MonthlyMaintenanceCost(month, byMonth.getOrDefault(month, BigDecimal.ZERO)));
        }
        return result;
    }

    /** Biểu đồ 3: mức lấp đầy các lớp của tuần này, luôn đủ 7 ngày từ thứ Hai đến Chủ nhật. */
    public List<ClassOccupancy> classOccupancyThisWeek() {
        requireAccess(Session.getRole());
        LocalDate monday = mondayOf(LocalDate.now());
        return fillMissingDays(dao.classOccupancyByDay(monday), monday);
    }

    /** Ngày thứ Hai của tuần chứa "day" (tuần tính từ thứ Hai đến Chủ nhật). */
    public static LocalDate mondayOf(LocalDate day) {
        return day.minusDays(day.getDayOfWeek().getValue() - 1);   // getValue(): Thứ Hai = 1 ... Chủ nhật = 7
    }

    /** Bổ sung ngày không có lớp bằng 0 chỗ / 0 sức chứa để luôn có đúng 7 ngày liên tiếp bắt đầu từ "monday". */
    static List<ClassOccupancy> fillMissingDays(List<ClassOccupancy> rows, LocalDate monday) {
        Map<LocalDate, ClassOccupancy> byDay = new HashMap<>();
        for (ClassOccupancy row : rows) {
            byDay.put(row.date(), row);
        }
        List<ClassOccupancy> result = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate day = monday.plusDays(i);
            result.add(byDay.getOrDefault(day, new ClassOccupancy(day, 0, 0)));
        }
        return result;
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

    /** Bảng xếp hạng (top huấn luyện viên, lớp đông nhất) là số liệu phân tích nên chỉ Quản lý xem. */
    public static void requireAnalyticsAccess(StaffRole role) {
        if (role != StaffRole.MANAGER) {
            throw new BusinessException("Chỉ Quản lý mới được xem bảng xếp hạng.");
        }
    }

    /** Số liệu tài chính (doanh thu) chỉ dành cho Quản lý; Lễ tân chỉ xem số liệu cơ bản. */
    public static void requireFinanceAccess(StaffRole role) {
        if (role != StaffRole.MANAGER) {
            throw new BusinessException("Chỉ Quản lý mới được xem doanh thu.");
        }
    }
}
