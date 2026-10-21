package com.gym.billing.model;

import java.math.BigDecimal;

/** Tổng doanh thu đã thu theo gói (nhóm theo kỳ thanh toán). */
public record RevenueByPlan(String planName, long paidInvoiceCount, BigDecimal revenue) { }
