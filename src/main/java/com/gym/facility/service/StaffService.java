package com.gym.facility.service;

import com.gym.core.dao.StaffDao;
import com.gym.core.exception.BusinessException;
import com.gym.core.model.Staff;
import com.gym.core.model.StaffRole;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class StaffService {

    private final StaffDao staffDao;

    public StaffService(StaffDao staffDao) {
        this.staffDao = Objects.requireNonNull(
                staffDao, "StaffDao không được null"
        );
    }

    /**
     * Tạo nhân sự.
     *
     * Trainer:
     *   INSERT STAFF -> INSERT TRAINER
     *
     * Receptionist:
     *   INSERT STAFF -> INSERT RECEPTIONIST
     *
     * Manager:
     *   Chỉ INSERT STAFF
     *
     * Các thao tác ghi nhiều bảng phải được StaffDao
     * thực hiện trong cùng một transaction.
     */
    public Staff createStaff(Staff staff) {
        validateStaff(staff);

        // Kiểm tra trùng số điện thoại trước khi tạo.
        if (staffDao.findByPhone(staff.getPhone()).isPresent()) {
            throw new BusinessException(
                    "Số điện thoại nhân sự đã tồn tại."
            );
        }

        // StaffDao.insert() sẽ định tuyến đến DAO phù hợp
        // theo loại nhân sự và trả về đối tượng đã tạo.
        return staffDao.insert(staff);
    }

    public Optional<Staff> findStaffByPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new BusinessException(
                    "Số điện  tìm kiếm không được để trống."
            );
        }
        return staffDao.findByPhone(phone.trim());
    }

    public List<Staff> findStaffByRole(StaffRole role) {
        if(role == null) {
            throw new BusinessException("role không được null");
        }
        return staffDao.findByRole(role);
    }

    private void validateStaff(Staff staff) {
        if (staff == null) {
            throw new BusinessException(
                    "Thông tin nhân sự không được để trống."
            );
        }

        if (staff.getFullName() == null
                || staff.getFullName().isBlank()) {
            throw new BusinessException(
                    "Họ tên nhân sự không được để trống."
            );
        }

        if (staff.getPhone() == null
                || staff.getPhone().isBlank()) {
            throw new BusinessException(
                    "Số điện thoại không được để trống."
            );
        }

        BigDecimal salary = staff.getSalary();

        if (salary == null
                || salary.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(
                    "Lương phải lớn hơn 0."
            );
        }

        LocalDate hireDate = staff.getHireDate();

        if (hireDate != null && hireDate.isAfter(LocalDate.now())) {
            throw new BusinessException(
                    "Ngày vào làm không được ở tương lai."
            );
        }
    }
}
