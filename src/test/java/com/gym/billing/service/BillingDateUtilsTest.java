package com.gym.billing.service;

import com.gym.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class BillingDateUtilsTest {

    @Test
    void oneMonthFromMidMonth() {
        assertEquals(LocalDate.of(2026, 11, 15),
                BillingDateUtils.calculateEndDate(LocalDate.of(2026, 10, 15), 1));
    }

    @Test
    void oneMonthFromJanuary31ClampsToFebruaryEnd() {
        assertEquals(LocalDate.of(2026, 2, 28),
                BillingDateUtils.calculateEndDate(LocalDate.of(2026, 1, 31), 1));
    }

    @Test
    void oneMonthFromJanuary31InLeapYear() {
        assertEquals(LocalDate.of(2024, 2, 29),
                BillingDateUtils.calculateEndDate(LocalDate.of(2024, 1, 31), 1));
    }

    @Test
    void twelveMonthsPreservesLeapDayUsingPlusMonthsRule() {
        assertEquals(LocalDate.of(2025, 2, 28),
                BillingDateUtils.calculateEndDate(LocalDate.of(2024, 2, 29), 12));
    }

    @Test
    void renewalKeepsUnusedDaysWhenOldMembershipEndsInFuture() {
        assertEquals(LocalDate.of(2026, 11, 20), BillingDateUtils.calculateRenewalEndDate(
                LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 20), 1));
    }

    @Test
    void renewalStartsTodayWhenOldMembershipEndsToday() {
        assertEquals(LocalDate.of(2026, 11, 17), BillingDateUtils.calculateRenewalEndDate(
                LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 17), 1));
    }

    @Test
    void renewalStartsTodayWhenOldMembershipExpiredEarlier() {
        assertEquals(LocalDate.of(2026, 11, 17), BillingDateUtils.calculateRenewalEndDate(
                LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 7), 1));
    }

    @Test
    void rejectsZeroDuration() {
        assertThrows(BusinessException.class,
                () -> BillingDateUtils.calculateEndDate(LocalDate.of(2026, 10, 17), 0));
    }

    @Test
    void rejectsNullStartDate() {
        assertThrows(BusinessException.class, () -> BillingDateUtils.calculateEndDate(null, 1));
    }

    @Test
    void renewalRejectsNullToday() {
        assertThrows(BusinessException.class, () ->
                BillingDateUtils.calculateRenewalStartDate(null, LocalDate.of(2026, 10, 20)));
    }

    @Test
    void rejectsDateOverflow() {
        assertThrows(BusinessException.class, () ->
                BillingDateUtils.calculateEndDate(LocalDate.MAX, 1));
    }
}
