package com.gym.billing.service;

import com.gym.core.exception.BusinessException;
import com.gym.core.model.BillingStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Các luật hóa đơn thuần (không truy cập DB, dễ kiểm thử đơn vị). */
public final class BillingRules {
    private BillingRules() { }

    public static void requireCanMarkPaid(BillingStatus status) {
        if (status != BillingStatus.PENDING) {
            if (status == BillingStatus.PAID) {
                throw new BusinessException("Hóa đơn đã thanh toán là trạng thái cuối, không thể thu tiền lần nữa.");
            }
            throw new BusinessException("Hóa đơn thất bại phải được chuyển về Chờ thanh toán trước khi thu tiền.");
        }
    }

    public static void requireCanMarkFailed(BillingStatus status) {
        if (status != BillingStatus.PENDING) {
            if (status == BillingStatus.PAID) {
                throw new BusinessException("Hóa đơn đã thanh toán không thể chuyển sang Thất bại.");
            }
            throw new BusinessException("Chỉ hóa đơn Chờ thanh toán mới có thể đánh dấu Thất bại.");
        }
    }

    public static void requireCanRetry(BillingStatus status) {
        if (status != BillingStatus.FAILED) {
            if (status == BillingStatus.PAID) {
                throw new BusinessException("Hóa đơn đã thanh toán không thể chuyển trạng thái.");
            }
            throw new BusinessException("Chỉ hóa đơn Thất bại mới có thể thử thanh toán lại.");
        }
    }

    public static BigDecimal normalizeMoney(BigDecimal amount) {
        if (amount == null || amount.signum() < 0) {
            throw new BusinessException("Số tiền hóa đơn không hợp lệ.");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static void requireExactPayment(BigDecimal expected, BigDecimal received) {
        BigDecimal e = normalizeMoney(expected);
        BigDecimal r = normalizeMoney(received);
        if (e.compareTo(r) != 0) {
            throw new BusinessException("Số tiền thanh toán phải đúng bằng giá gói (" + e.toPlainString() + "). Không hỗ trợ trả góp.");
        }
    }

    public static void requireDueDateNotBeforeInvoice(LocalDate dueDate, LocalDateTime invoiceDate) {
        if (dueDate == null || invoiceDate == null || dueDate.isBefore(invoiceDate.toLocalDate())) {
            throw new BusinessException("Hạn thanh toán không được sớm hơn ngày lập hóa đơn.");
        }
    }

    public static long daysUntil(LocalDate endDate, LocalDate today) {
        if (endDate == null || today == null) throw new BusinessException("Thiếu ngày để tính thời hạn gói.");
        return java.time.temporal.ChronoUnit.DAYS.between(today, endDate);
    }
}
