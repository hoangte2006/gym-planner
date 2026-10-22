package com.gym.billing.service;

import com.gym.core.exception.BusinessException;
import com.gym.core.model.BillingStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BillingRulesTest {
    @Test void pendingCanBeMarkedPaid() { assertDoesNotThrow(() -> BillingRules.requireCanMarkPaid(BillingStatus.PENDING)); }
    @Test void paidIsTerminalAndCannotBePaidAgain() { assertThrows(BusinessException.class, () -> BillingRules.requireCanMarkPaid(BillingStatus.PAID)); }
    @Test void failedMustBeRetriedBeforePaying() { assertThrows(BusinessException.class, () -> BillingRules.requireCanMarkPaid(BillingStatus.FAILED)); }
    @Test void pendingCanBeMarkedFailed() { assertDoesNotThrow(() -> BillingRules.requireCanMarkFailed(BillingStatus.PENDING)); }
    @Test void paidCannotBeMarkedFailed() { assertThrows(BusinessException.class, () -> BillingRules.requireCanMarkFailed(BillingStatus.PAID)); }
    @Test void failedCannotBeMarkedFailedTwice() { assertThrows(BusinessException.class, () -> BillingRules.requireCanMarkFailed(BillingStatus.FAILED)); }
    @Test void failedCanBeRetried() { assertDoesNotThrow(() -> BillingRules.requireCanRetry(BillingStatus.FAILED)); }
    @Test void pendingCannotUseRetryAction() { assertThrows(BusinessException.class, () -> BillingRules.requireCanRetry(BillingStatus.PENDING)); }
    @Test void paidCannotUseRetryAction() { assertThrows(BusinessException.class, () -> BillingRules.requireCanRetry(BillingStatus.PAID)); }
    @Test void normalizeMoneyAddsTwoDecimalPlaces() { assertEquals(new BigDecimal("1500.00"), BillingRules.normalizeMoney(new BigDecimal("1500"))); }
    @Test void normalizeMoneyRoundsHalfUp() { assertEquals(new BigDecimal("10.13"), BillingRules.normalizeMoney(new BigDecimal("10.125"))); }
    @Test void normalizeMoneyRejectsNegative() { assertThrows(BusinessException.class, () -> BillingRules.normalizeMoney(new BigDecimal("-0.01"))); }
    @Test void exactPaymentAcceptsEqualAmountsWithDifferentScales() { assertDoesNotThrow(() -> BillingRules.requireExactPayment(new BigDecimal("500"), new BigDecimal("500.00"))); }
    @Test void exactPaymentRejectsInstallmentOrShortPayment() { assertThrows(BusinessException.class, () -> BillingRules.requireExactPayment(new BigDecimal("500.00"), new BigDecimal("250.00"))); }
    @Test void dueDateCanEqualInvoiceCalendarDate() { assertDoesNotThrow(() -> BillingRules.requireDueDateNotBeforeInvoice(LocalDate.of(2026, 10, 17), LocalDateTime.of(2026, 10, 17, 23, 59))); }
    @Test void dueDateCannotPrecedeInvoiceDate() { assertThrows(BusinessException.class, () -> BillingRules.requireDueDateNotBeforeInvoice(LocalDate.of(2026, 10, 16), LocalDateTime.of(2026, 10, 17, 0, 0))); }
    @Test void daysUntilEndTodayIsZero() { assertEquals(0, BillingRules.daysUntil(LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 17))); }
    @Test void daysUntilEndInOneWeekIsSeven() { assertEquals(7, BillingRules.daysUntil(LocalDate.of(2026, 10, 24), LocalDate.of(2026, 10, 17))); }
    @Test void daysUntilRejectsNullDates() { assertThrows(BusinessException.class, () -> BillingRules.daysUntil(null, LocalDate.now())); }
}
