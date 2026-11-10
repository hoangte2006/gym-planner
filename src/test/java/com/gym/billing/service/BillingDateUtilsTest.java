package com.gym.billing.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** TC10–TC15: quy tắc ngày gia hạn và cuối tháng / năm nhuận. */
class BillingDateUtilsTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 17);

    @Test
    void tc10_renewWhenOldEndInFuture_keepsUnusedDays() {
        LocalDate oldEnd = LocalDate.of(2026, 10, 20);

        LocalDate start = BillingDateUtils.calculateRenewalStartDate(TODAY, oldEnd);
        LocalDate end = BillingDateUtils.calculateRenewalEndDate(TODAY, oldEnd, 1);

        assertEquals(LocalDate.of(2026, 10, 20), start);
        assertEquals(LocalDate.of(2026, 11, 20), end);
    }

    @Test
    void tc11_renewWhenOldEndIsToday_startsToday() {
        LocalDate oldEnd = LocalDate.of(2026, 10, 17);

        LocalDate start = BillingDateUtils.calculateRenewalStartDate(TODAY, oldEnd);
        LocalDate end = BillingDateUtils.calculateRenewalEndDate(TODAY, oldEnd, 1);

        assertEquals(LocalDate.of(2026, 10, 17), start);
        assertEquals(LocalDate.of(2026, 11, 17), end);
    }

    @Test
    void tc12_renewWhenOldEndAlreadyExpired_startsToday() {
        LocalDate oldEnd = LocalDate.of(2026, 10, 7);

        LocalDate start = BillingDateUtils.calculateRenewalStartDate(TODAY, oldEnd);
        LocalDate end = BillingDateUtils.calculateRenewalEndDate(TODAY, oldEnd, 1);

        assertEquals(LocalDate.of(2026, 10, 17), start);
        assertEquals(LocalDate.of(2026, 11, 17), end);
    }

    @Test
    void tc14_oneMonthFromJan31NonLeap_endsFeb28() {
        assertEquals(LocalDate.of(2026, 2, 28),
                BillingDateUtils.calculateEndDate(LocalDate.of(2026, 1, 31), 1));
    }

    @Test
    void tc15_oneMonthFromJan31LeapYear_endsFeb29() {
        assertEquals(LocalDate.of(2024, 2, 29),
                BillingDateUtils.calculateEndDate(LocalDate.of(2024, 1, 31), 1));
    }
}
