package com.gym.billing.service;

import com.gym.core.exception.BusinessException;
import com.gym.core.model.BillingStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TC16–TC20: luật trạng thái hóa đơn và thanh toán đủ. */
class BillingRulesTest {

    @Test
    void tc16_pendingToPaid_isAllowed() {
        assertDoesNotThrow(() -> BillingRules.requireCanMarkPaid(BillingStatus.PENDING));
    }

    @Test
    void tc17_paidIsFinal_rejectsSecondPayOrFail() {
        BusinessException payAgain = assertThrows(BusinessException.class,
                () -> BillingRules.requireCanMarkPaid(BillingStatus.PAID));
        BusinessException markFailed = assertThrows(BusinessException.class,
                () -> BillingRules.requireCanMarkFailed(BillingStatus.PAID));
        BusinessException retry = assertThrows(BusinessException.class,
                () -> BillingRules.requireCanRetry(BillingStatus.PAID));

        assertTrue(payAgain.getMessage().contains("trạng thái cuối")
                || payAgain.getMessage().contains("không thể thu tiền lần nữa"));
        assertTrue(markFailed.getMessage().contains("không thể chuyển sang Thất bại"));
        assertTrue(retry.getMessage().contains("không thể chuyển trạng thái"));
    }

    @Test
    void tc18_pendingToFailed_isAllowed() {
        assertDoesNotThrow(() -> BillingRules.requireCanMarkFailed(BillingStatus.PENDING));
    }

    @Test
    void tc19_failedToPending_isAllowed() {
        assertDoesNotThrow(() -> BillingRules.requireCanRetry(BillingStatus.FAILED));
    }

    @Test
    void tc20_exactPaymentRequired_withBigDecimalScale2() {
        BigDecimal expected = new BigDecimal("1000");
        BigDecimal normalized = BillingRules.normalizeMoney(expected);
        assertEquals(2, normalized.scale());
        assertEquals(0, new BigDecimal("1000.00").compareTo(normalized));

        BusinessException shortPay = assertThrows(BusinessException.class,
                () -> BillingRules.requireExactPayment(new BigDecimal("1000.00"), new BigDecimal("500.00")));
        assertTrue(shortPay.getMessage().contains("đúng bằng giá gói")
                || shortPay.getMessage().contains("Không hỗ trợ trả góp"));

        assertDoesNotThrow(() ->
                BillingRules.requireExactPayment(new BigDecimal("1000.00"), new BigDecimal("1000")));
    }
}
