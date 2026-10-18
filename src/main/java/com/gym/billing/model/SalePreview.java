package com.gym.billing.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Bản xem trước trước khi tạo giao dịch; DAO vẫn tính và kiểm tra lại trong transaction. */
public record SalePreview(LocalDate startDate, LocalDate endDate, BigDecimal price,
                          String planName, int durationMonths) { }
