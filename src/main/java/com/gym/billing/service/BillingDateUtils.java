package com.gym.billing.service;

import com.gym.core.DateTimeUtils;
import com.gym.core.exception.BusinessException;

import java.time.DateTimeException;
import java.time.LocalDate;

/** Quy tắc ngày riêng của module billing; tái sử dụng DateTimeUtils của Core. */
public final class BillingDateUtils {

    private BillingDateUtils() {
    }

    /** Ngày kết thúc = ngày bắt đầu cộng số tháng của gói. */
    public static LocalDate calculateEndDate(LocalDate startDate, int durationMonths) {
        if (startDate == null) {
            throw new BusinessException("Ngày bắt đầu không được để trống.");
        }
        if (durationMonths <= 0) {
            throw new BusinessException("Thời hạn gói phải lớn hơn 0 tháng.");
        }
        try {
            return DateTimeUtils.membershipEnd(startDate, durationMonths);
        } catch (DateTimeException | ArithmeticException ex) {
            throw new BusinessException("Ngày kết thúc vượt phạm vi ngày hợp lệ. Hãy kiểm tra ngày bắt đầu và thời hạn gói.", ex);
        }
    }

    /** Gia hạn giữ ngày chưa dùng: lấy ngày muộn hơn giữa hôm nay và ngày kết thúc gói cũ. */
    public static LocalDate calculateRenewalStartDate(LocalDate today, LocalDate oldEndDate) {
        if (today == null) {
            throw new BusinessException("Ngày gia hạn không được để trống.");
        }
        return oldEndDate != null && oldEndDate.isAfter(today) ? oldEndDate : today;
    }

    public static LocalDate calculateRenewalEndDate(LocalDate today, LocalDate oldEndDate, int durationMonths) {
        LocalDate start = calculateRenewalStartDate(today, oldEndDate);
        return calculateEndDate(start, durationMonths);
    }
}
