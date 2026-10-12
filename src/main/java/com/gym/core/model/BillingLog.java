package com.gym.core.model;

import com.gym.core.util.Check;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Hóa đơn (bảng BILLING_LOG). */
public class BillingLog {

    private Integer billId;
    private Integer visitorId;
    private Integer membershipId;
    private LocalDateTime invoiceDate;
    private LocalDate dueDate;
    private LocalDateTime paymentDate;
    private BigDecimal amountPaid;
    private BillingStatus billingStatus;

    /** Mã hóa đơn. */
    public Integer getBillId() {
        return billId;
    }

    public void setBillId(Integer billId) {
        this.billId = billId;
    }

    /** Mã khách. */
    public Integer getVisitorId() {
        return visitorId;
    }

    public void setVisitorId(Integer visitorId) {
        this.visitorId = visitorId;
    }

    /** Mã đăng ký gói (có thể trống). */
    public Integer getMembershipId() {
        return membershipId;
    }

    public void setMembershipId(Integer membershipId) {
        this.membershipId = membershipId;
    }

    /** Ngày lập. */
    public LocalDateTime getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(LocalDateTime invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    /** Hạn thanh toán. */
    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    /** Ngày thanh toán. */
    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    /** Số tiền. */
    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = Check.nonNegative(amountPaid, "Số tiền");
    }

    /** Trạng thái. */
    public BillingStatus getBillingStatus() {
        return billingStatus;
    }

    public void setBillingStatus(BillingStatus billingStatus) {
        this.billingStatus = billingStatus;
    }
}
