package com.gym.dashboard.service;

import com.gym.core.exception.BusinessException;
import com.gym.core.model.StaffRole;
import com.gym.dashboard.model.MonthlyRevenue;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
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
    void noDataStillGivesAllMonthsWithZero() {
        List<MonthlyRevenue> result = DashboardService.fillMissingMonths(List.of(), YearMonth.of(2026, 10), 12);
        assertEquals(12, result.size());
        assertTrue(result.stream().allMatch(r -> r.revenue().signum() == 0));
    }
}
