package com.gym.dashboard.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Toàn bộ số liệu của Dashboard tại một thời điểm, dùng để xuất ra tệp CSV hoặc PDF.
 * Số liệu dành riêng cho Quản lý (doanh thu, chi phí bảo trì, top huấn luyện viên, lớp đông nhất) chỉ có khi
 * "includesManagerData" là true; nếu không, các danh sách đó để trống và tệp xuất không có phần đó.
 */
public record DashboardReport(
        LocalDateTime generatedAt,
        boolean includesManagerData,
        DateRange range,                                  // khoảng lọc của các biểu đồ theo tháng
        int expiringDays,                                 // mốc ngày của chỉ số "gói sắp hết hạn"
        int activeMembers,
        int expiringMemberships,
        int classToday,
        BigDecimal monthlyRevenue,                        // doanh thu tháng này; ZERO nếu không có quyền
        List<MonthlyRevenue> revenueByMonth,
        List<MonthlyMaintenanceCost> maintenanceByMonth,
        List<TrainerSessionCount> topTrainers,
        List<BusiestClass> busiestClasses,
        List<ClassOccupancy> occupancyThisWeek,
        List<EquipmentStatusCount> equipmentByStatus) {
}
