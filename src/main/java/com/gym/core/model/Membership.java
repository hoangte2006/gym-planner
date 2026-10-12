package com.gym.core.model;

import java.time.LocalDate;

/** Một lần đăng ký gói tập của hội viên (bảng MEMBERSHIP). */
public class Membership {

    private Integer membershipId;
    private Integer visitorId;
    private Integer planId;
    private LocalDate startDate;
    private LocalDate endDate;
    private MembershipStatus status;

    /** Mã đăng ký. */
    public Integer getMembershipId() {
        return membershipId;
    }

    public void setMembershipId(Integer membershipId) {
        this.membershipId = membershipId;
    }

    /** Mã hội viên. */
    public Integer getVisitorId() {
        return visitorId;
    }

    public void setVisitorId(Integer visitorId) {
        this.visitorId = visitorId;
    }

    /** Mã gói. */
    public Integer getPlanId() {
        return planId;
    }

    public void setPlanId(Integer planId) {
        this.planId = planId;
    }

    /** Ngày bắt đầu. */
    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    /** Ngày kết thúc. */
    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    /** Trạng thái. */
    public MembershipStatus getStatus() {
        return status;
    }

    public void setStatus(MembershipStatus status) {
        this.status = status;
    }
}
