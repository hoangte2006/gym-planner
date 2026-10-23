package com.gym.dashboard.model;

import com.gym.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Test thuần (không cần DB) cho khoảng thời gian lọc của Dashboard. */
class DateRangeTest {

    @Test
    void lastMonthsStartsOnTheFirstDayOfTheOldestMonthAndEndsToday() {
        DateRange r = DateRange.lastMonths(LocalDate.of(2026, 10, 22), 3);
        assertEquals(LocalDate.of(2026, 8, 1), r.from());          // tháng 8, 9, 10
        assertEquals(LocalDate.of(2026, 10, 22), r.to());
        assertEquals(3, r.monthCount());
    }

    @Test
    void lastMonthsCrossesTheYearBoundary() {
        DateRange r = DateRange.lastMonths(LocalDate.of(2026, 2, 5), 12);
        assertEquals(LocalDate.of(2025, 3, 1), r.from());
        assertEquals(12, r.monthCount());
    }

    @Test
    void thisYearStartsOnJanuaryFirst() {
        DateRange r = DateRange.thisYear(LocalDate.of(2026, 10, 22));
        assertEquals(LocalDate.of(2026, 1, 1), r.from());
        assertEquals(10, r.monthCount());                          // tháng 1 đến tháng 10
    }

    @Test
    void monthCountCountsCalendarMonthsTouchedNotDays() {
        assertEquals(1, new DateRange(LocalDate.of(2026, 5, 31), LocalDate.of(2026, 5, 31)).monthCount());
        assertEquals(2, new DateRange(LocalDate.of(2026, 5, 31), LocalDate.of(2026, 6, 1)).monthCount());   // chỉ 2 ngày nhưng chạm 2 tháng
        assertEquals(3, new DateRange(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 3, 3)).monthCount());
    }

    @Test
    void rejectsMissingOrReversedDates() {
        assertThrows(BusinessException.class, () -> new DateRange(null, LocalDate.of(2026, 1, 1)));
        assertThrows(BusinessException.class, () -> new DateRange(LocalDate.of(2026, 1, 1), null));
        assertThrows(BusinessException.class,
                () -> new DateRange(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 1)));        // ngày đầu sau ngày cuối
        new DateRange(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 1));                       // cùng một ngày thì hợp lệ
    }
}
