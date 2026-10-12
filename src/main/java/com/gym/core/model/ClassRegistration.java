package com.gym.core.model;

import java.time.LocalDateTime;

/** Đăng ký lớp (bảng CLASS_REGISTRATION, khóa gồm Session_ID + Visitor_ID). */
public class ClassRegistration {

    private Integer sessionId;
    private Integer visitorId;
    private LocalDateTime registrationDate;
    private AttendanceStatus attendanceStatus;

    /** Mã lớp. */
    public Integer getSessionId() {
        return sessionId;
    }

    public void setSessionId(Integer sessionId) {
        this.sessionId = sessionId;
    }

    /** Mã hội viên. */
    public Integer getVisitorId() {
        return visitorId;
    }

    public void setVisitorId(Integer visitorId) {
        this.visitorId = visitorId;
    }

    /** Ngày đăng ký. */
    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDateTime registrationDate) {
        this.registrationDate = registrationDate;
    }

    /** Trạng thái. */
    public AttendanceStatus getAttendanceStatus() {
        return attendanceStatus;
    }

    public void setAttendanceStatus(AttendanceStatus attendanceStatus) {
        this.attendanceStatus = attendanceStatus;
    }
}
