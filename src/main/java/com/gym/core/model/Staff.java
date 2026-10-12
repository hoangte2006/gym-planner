package com.gym.core.model;

import com.gym.core.util.Check;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Nhân viên (lớp cha, bảng STAFF). Là Trainer, Receptionist hoặc Manager. */
public abstract class Staff {

    private Integer staffId;
    private String fullName;
    private String phone;
    private StaffRole role;
    private LocalDate hireDate;
    private BigDecimal salary;

    /** Mã nhân viên. */
    public Integer getStaffId() {
        return staffId;
    }

    public void setStaffId(Integer staffId) {
        this.staffId = staffId;
    }

    /** Họ tên. */
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = Check.text(fullName, "Họ tên");
    }

    /** Số điện thoại. */
    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = Check.text(phone, "Số điện thoại");
    }

    /** Vai trò. */
    public StaffRole getRole() {
        return role;
    }

    public void setRole(StaffRole role) {
        this.role = role;
    }

    /** Ngày vào làm. */
    public LocalDate getHireDate() {
        return hireDate;
    }

    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }

    /** Lương. */
    public BigDecimal getSalary() {
        return salary;
    }

    public void setSalary(BigDecimal salary) {
        this.salary = Check.positive(salary, "Lương");
    }

    /** Tên vai trò tiếng Việt. Lớp con ghi đè. */
    public abstract String getRoleName();
}
