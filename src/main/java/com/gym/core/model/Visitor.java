package com.gym.core.model;

import com.gym.core.util.Check;

/** Khách của phòng tập (lớp cha, bảng VISITOR). Là Member hoặc DailyPassTicket. */
public abstract class Visitor {

    private Integer visitorId;
    private String firstName;
    private String lastName;
    private String phone;

    /** Mã khách. */
    public Integer getVisitorId() {
        return visitorId;
    }

    public void setVisitorId(Integer visitorId) {
        this.visitorId = visitorId;
    }

    /** Tên. */
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = Check.text(firstName, "Tên");
    }

    /** Họ. */
    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = Check.text(lastName, "Họ");
    }

    /** Số điện thoại. */
    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = Check.text(phone, "Số điện thoại");
    }

    /** Tên hiển thị trên danh sách. Lớp con ghi đè để thêm loại khách. */
    public abstract String getDisplayName();

    /** Họ và tên đầy đủ. */
    public String getFullName() {
        return lastName + " " + firstName;
    }
}
