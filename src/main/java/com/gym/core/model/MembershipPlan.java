package com.gym.core.model;

import com.gym.core.util.Check;
import java.math.BigDecimal;

/** Gói tập (bảng MEMBERSHIP_PLAN). */
public class MembershipPlan {

    private Integer planId;
    private String planName;
    private int durationMonths;
    private BigDecimal price;
    private String description;

    /** Mã gói. */
    public Integer getPlanId() {
        return planId;
    }

    public void setPlanId(Integer planId) {
        this.planId = planId;
    }

    /** Tên gói. */
    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = Check.text(planName, "Tên gói");
    }

    /** Thời hạn (tháng). */
    public int getDurationMonths() {
        return durationMonths;
    }

    public void setDurationMonths(int durationMonths) {
        this.durationMonths = Check.positive(durationMonths, "Thời hạn (tháng)");
    }

    /** Giá. */
    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = Check.nonNegative(price, "Giá");
    }

    /** Mô tả. */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
