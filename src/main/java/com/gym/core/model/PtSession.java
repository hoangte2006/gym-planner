package com.gym.core.model;

/** Buổi PT một kèm một (bảng PT_BOOKING, con của SESSION). */
public class PtSession extends Session {

    private Integer memberId;
    private BookingStatus bookingStatus;

    /** Mã hội viên. */
    public Integer getMemberId() {
        return memberId;
    }

    public void setMemberId(Integer memberId) {
        this.memberId = memberId;
    }

    /** Trạng thái. */
    public BookingStatus getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(BookingStatus bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

    @Override
    public String getSessionTitle() {
        return "Buổi PT (hội viên #" + memberId + ")";
    }
}
