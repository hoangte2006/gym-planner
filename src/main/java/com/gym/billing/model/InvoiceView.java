package com.gym.billing.model;

import com.gym.core.model.BillingStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Hóa đơn đã ghép thêm tên khách và số tiền cần thu từ gói/vé ngày. */
public record InvoiceView(int billId, int visitorId, String customerName, String phone,
                          Integer membershipId, String planName, LocalDateTime invoiceDate,
                          LocalDate dueDate, LocalDateTime paymentDate, BigDecimal totalAmount,
                          BigDecimal amountPaid, BillingStatus status) { }
