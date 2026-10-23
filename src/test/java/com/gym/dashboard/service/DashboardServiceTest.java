package com.gym.dashboard.service;

import com.gym.core.exception.BusinessException;
import com.gym.core.model.StaffRole;
import com.gym.dashboard.model.ClassOccupancy;
import com.gym.dashboard.model.DateRange;
import com.gym.dashboard.model.MonthlyMaintenanceCost;
import com.gym.dashboard.model.MonthlyRevenue;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Test thuần (không cần DB): Dashboard chỉ cho Quản lý và Lễ tân xem. */
class DashboardServiceTest {

    @Test
    void managerAndReceptionistMayView() {
        assertDoesNotThrow(() -> DashboardService.requireAccess(StaffRole.MANAGER));
        assertDoesNotThrow(() -> DashboardService.requireAccess(StaffRole.RECEPTIONIST));
    }

    @Test
    void trainerMayNotView() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> DashboardService.requireAccess(StaffRole.TRAINER));
        assertTrue(ex.getMessage().contains("không có quyền"));
    }

    @Test
    void onlyManagerMaySeeRevenue() {
        assertDoesNotThrow(() -> DashboardService.requireFinanceAccess(StaffRole.MANAGER));
    }

    @Test
    void receptionistMayNotSeeRevenueEvenThoughTheyMayViewTheDashboard() {
        assertDoesNotThrow(() -> DashboardService.requireAccess(StaffRole.RECEPTIONIST));   // xem được Dashboard
        BusinessException ex = assertThrows(BusinessException.class,
                () -> DashboardService.requireFinanceAccess(StaffRole.RECEPTIONIST));       // nhưng không xem được doanh thu
        assertTrue(ex.getMessage().contains("Quản lý"), ex.getMessage());
    }

    @Test
    void trainerMayNotSeeRevenue() {
        assertThrows(BusinessException.class, () -> DashboardService.requireFinanceAccess(StaffRole.TRAINER));
    }

    @Test
    void fillMissingMonthsAlwaysGivesTheFullRangeOldestFirst() {
        // Chỉ có doanh thu tháng 8 và tháng 10/2026; yêu cầu 4 tháng kết thúc ở 10/2026 -> 7, 8, 9, 10
        List<MonthlyRevenue> rows = List.of(
                new MonthlyRevenue(YearMonth.of(2026, 8), new BigDecimal("100.00")),
                new MonthlyRevenue(YearMonth.of(2026, 10), new BigDecimal("250.50")));

        List<MonthlyRevenue> result = DashboardService.fillMissingMonths(rows, YearMonth.of(2026, 10), 4);

        assertEquals(4, result.size());
        assertEquals(YearMonth.of(2026, 7), result.get(0).month());              // từ cũ đến mới
        assertEquals(0, BigDecimal.ZERO.compareTo(result.get(0).revenue()));      // tháng 7 thiếu -> 0
        assertEquals(0, new BigDecimal("100.00").compareTo(result.get(1).revenue()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.get(2).revenue()));      // tháng 9 thiếu -> 0
        assertEquals(0, new BigDecimal("250.50").compareTo(result.get(3).revenue()));
    }

    @Test
    void fillMissingMonthsCrossesTheYearBoundaryAndIgnoresMonthsOutsideTheRange() {
        List<MonthlyRevenue> rows = List.of(
                new MonthlyRevenue(YearMonth.of(2025, 12), new BigDecimal("5")),
                new MonthlyRevenue(YearMonth.of(2020, 1), new BigDecimal("999")));   // quá cũ, không được xuất hiện

        List<MonthlyRevenue> result = DashboardService.fillMissingMonths(rows, YearMonth.of(2026, 2), 3);

        assertEquals(List.of(YearMonth.of(2025, 12), YearMonth.of(2026, 1), YearMonth.of(2026, 2)),
                result.stream().map(MonthlyRevenue::month).toList());
        assertEquals(0, new BigDecimal("5").compareTo(result.get(0).revenue()));
    }

    @Test
    void expiringRangeAcceptsOnlySevenAndThirtyDays() {
        assertDoesNotThrow(() -> DashboardService.requireValidExpiringDays(7));
        assertDoesNotThrow(() -> DashboardService.requireValidExpiringDays(30));
        for (int bad : new int[]{0, -7, 1, 8, 29, 31, 365, Integer.MAX_VALUE}) {
            assertThrows(BusinessException.class, () -> DashboardService.requireValidExpiringDays(bad), "days=" + bad);
        }
    }

    @Test
    void maintenanceCostGapsAreFilledWithZeroOldestFirst() {
        List<MonthlyMaintenanceCost> rows = List.of(
                new MonthlyMaintenanceCost(YearMonth.of(2026, 1), new BigDecimal("50.00")),
                new MonthlyMaintenanceCost(YearMonth.of(2020, 1), new BigDecimal("999")));   // quá cũ, không được xuất hiện

        List<MonthlyMaintenanceCost> result =
                DashboardService.fillMissingMaintenanceMonths(rows, YearMonth.of(2026, 3), 4);

        assertEquals(List.of(YearMonth.of(2025, 12), YearMonth.of(2026, 1), YearMonth.of(2026, 2), YearMonth.of(2026, 3)),
                result.stream().map(MonthlyMaintenanceCost::month).toList());     // qua ranh giới năm, từ cũ đến mới
        assertEquals(0, BigDecimal.ZERO.compareTo(result.get(0).cost()));
        assertEquals(0, new BigDecimal("50.00").compareTo(result.get(1).cost()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.get(3).cost()));
    }

    @Test
    void maintenanceCostIsFinancialSoOnlyManagerMaySeeIt() {
        // Biểu đồ chi phí bảo trì dùng requireFinanceAccess (cùng quy tắc với doanh thu)
        assertDoesNotThrow(() -> DashboardService.requireFinanceAccess(StaffRole.MANAGER));
        assertThrows(BusinessException.class, () -> DashboardService.requireFinanceAccess(StaffRole.RECEPTIONIST));
    }

    @Test
    void rankingsAreForManagerOnly() {
        DashboardService.requireAnalyticsAccess(StaffRole.MANAGER);
        assertThrows(BusinessException.class, () -> DashboardService.requireAnalyticsAccess(StaffRole.RECEPTIONIST));
        assertThrows(BusinessException.class, () -> DashboardService.requireAnalyticsAccess(StaffRole.TRAINER));
        assertThrows(BusinessException.class, () -> DashboardService.requireAnalyticsAccess(null));    // chưa đăng nhập
    }

    @Test
    void rangeMustExistAndBeAtMostThirtySixMonths() {
        assertThrows(BusinessException.class, () -> DashboardService.requireValidRange(null));
        DashboardService.requireValidRange(DateRange.lastMonths(LocalDate.of(2026, 10, 22), 36));      // đúng 36 tháng: được
        BusinessException ex = assertThrows(BusinessException.class,
                () -> DashboardService.requireValidRange(DateRange.lastMonths(LocalDate.of(2026, 10, 22), 37)));
        assertTrue(ex.getMessage().contains("36"), ex.getMessage());
    }

    @Test
    void chartFillWorksForAnyRangeLengthNotJustTwelveMonths() {
        DateRange range = new DateRange(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 3, 3));          // 3 tháng lịch
        List<MonthlyRevenue> filled = DashboardService.fillMissingMonths(
                List.of(new MonthlyRevenue(YearMonth.of(2026, 2), new BigDecimal("10"))),
                YearMonth.from(range.to()), range.monthCount());
        assertEquals(List.of(YearMonth.of(2026, 1), YearMonth.of(2026, 2), YearMonth.of(2026, 3)),
                filled.stream().map(MonthlyRevenue::month).toList());
    }

    @Test
    void mondayOfIsTheStartOfTheMondayToSundayWeek() {
        LocalDate monday = LocalDate.of(2026, 10, 19);                          // 19/10/2026 là thứ Hai
        assertEquals(monday, DashboardService.mondayOf(monday));                // thứ Hai -> chính nó
        assertEquals(monday, DashboardService.mondayOf(LocalDate.of(2026, 10, 22)));   // thứ Năm
        assertEquals(monday, DashboardService.mondayOf(LocalDate.of(2026, 10, 25)));   // Chủ nhật vẫn thuộc tuần này
        assertEquals(LocalDate.of(2026, 10, 26), DashboardService.mondayOf(LocalDate.of(2026, 10, 26)));   // thứ Hai tuần sau
        assertEquals(LocalDate.of(2025, 12, 29), DashboardService.mondayOf(LocalDate.of(2026, 1, 1)));    // qua ranh giới năm
    }

    @Test
    void fillMissingDaysGivesSevenDaysMondayFirstWithZeroForDaysWithoutClasses() {
        LocalDate monday = LocalDate.of(2026, 10, 19);
        List<ClassOccupancy> rows = List.of(
                new ClassOccupancy(monday.plusDays(2), 6, 10),
                new ClassOccupancy(monday.plusDays(6), 3, 4));

        List<ClassOccupancy> result = DashboardService.fillMissingDays(rows, monday);

        assertEquals(7, result.size());
        assertEquals(monday, result.get(0).date());
        assertEquals(monday.plusDays(6), result.get(6).date());
        assertEquals(0, result.get(0).capacity());                        // ngày không có lớp -> 0/0
        assertEquals(60.0, result.get(2).percent(), 0.0001);             // 6/10 = 60%
        assertEquals(75.0, result.get(6).percent(), 0.0001);             // 3/4 = 75%
    }

    @Test
    void percentIsZeroWhenThereIsNoCapacityAndHundredWhenFull() {
        LocalDate day = LocalDate.of(2026, 10, 19);
        assertEquals(0.0, new ClassOccupancy(day, 0, 0).percent());       // không chia cho 0
        assertEquals(100.0, new ClassOccupancy(day, 8, 8).percent());
        assertEquals(0.0, new ClassOccupancy(day, 0, 8).percent());
    }

    @Test
    void noDataStillGivesAllMonthsWithZero() {
        List<MonthlyRevenue> result = DashboardService.fillMissingMonths(List.of(), YearMonth.of(2026, 10), 12);
        assertEquals(12, result.size());
        assertTrue(result.stream().allMatch(r -> r.revenue().signum() == 0));
    }
}
