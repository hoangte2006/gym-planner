package com.gym.facility.service;

import com.gym.app.Session;
import com.gym.core.dao.PageResult;
import com.gym.core.dao.StaffDao;
import com.gym.core.exception.BusinessException;
import com.gym.core.model.Manager;
import com.gym.core.model.Receptionist;
import com.gym.core.model.Staff;
import com.gym.core.model.StaffRole;
import com.gym.core.model.Trainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Luật nghiệp vụ nhân sự. Không chứa SQL và không phụ thuộc Swing. */
public class StaffService {

    private final StaffDao staffDao;

    public StaffService(StaffDao staffDao) {
        this.staffDao = Objects.requireNonNull(staffDao, "StaffDao không được null");
    }

    /** Thêm một STAFF và bảng con tương ứng; transaction nhiều bảng được DAO xử lý. */
    public Staff createStaff(Staff staff) {

        requireStaffAccess(Session.getRole());

        validateStaff(staff);
        if (staff.getStaffId() != null) {
            throw new BusinessException("Nhân sự mới không được tự gán mã nhân viên.");
        }

        normalizeAndCheckPhone(staff, null);
        return staffDao.insert(staff);
    }

    /** Cập nhật nhân sự. Không cho đổi loại lớp vì việc đó cần chuyển bảng con có kiểm soát. */
    public Staff updateStaff(Staff staff) {

        requireStaffAccess(Session.getRole());

        validateStaff(staff);
        if (staff.getStaffId() == null || staff.getStaffId() <= 0) {
            throw new BusinessException("Mã nhân viên không hợp lệ.");
        }
        Staff current = staffDao.findById(staff.getStaffId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy nhân sự cần cập nhật."));
        if (current.getClass() != staff.getClass()) {
            throw new BusinessException("Không thể đổi vai trò khi sửa hồ sơ. Hãy tạo hồ sơ mới đúng vai trò.");
        }
        normalizeAndCheckPhone(staff, staff.getStaffId());
        staffDao.update(staff);
        return staff;
    }

    public void deleteStaff(Integer staffId) {

        requireStaffAccess(Session.getRole());

        requirePositiveId(staffId, "Mã nhân viên");
        if (staffDao.findById(staffId).isEmpty()) {
            throw new BusinessException("Nhân sự không tồn tại hoặc đã bị xóa.");
        }
        // FK MAINTENANCE_LOG.Logged_By cố ý ngăn xóa người đã ký phiếu bảo trì.
        staffDao.deleteById(staffId);
    }

    public Staff findStaffById(Integer staffId) {

        requireStaffAccess(Session.getRole());

        requirePositiveId(staffId, "Mã nhân viên");
        return staffDao.findById(staffId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy nhân sự."));
    }

    public List<Staff> findAll() {

        requireStaffAccess(Session.getRole());

        return staffDao.findAll();
    }

    /**
     * Tìm nhân sự theo từ khóa.
     */
    public List<Staff> searchStaff(String keyword) {
        requireStaffAccess(Session.getRole());

        if (keyword == null || keyword.isBlank()) {
            return staffDao.findAll();
        }

        return staffDao.search(keyword.trim());
    }

    public PageResult<Staff> findPage(int page, int pageSize) {

        requireStaffAccess(Session.getRole());

        return staffDao.findPage(page, pageSize);
    }

    public List<Staff> findStaffByRole(StaffRole role) {

        requireStaffAccess(Session.getRole());

        if (role == null) {
            throw new BusinessException("Vai trò nhân sự không được để trống.");
        }
        return staffDao.findByRole(role);
    }

    private void normalizeAndCheckPhone(Staff staff, Integer currentStaffId) {

        requireStaffAccess(Session.getRole());

        String raw = staff.getPhone();
        if (raw == null || raw.isBlank()) {
            throw new BusinessException("Số điện thoại không được để trống.");
        }
        String phone = raw.trim().replaceAll("[\\s().-]", "");
        if (!phone.matches("(?:\\+\\d{8,14}|\\d{8,15})")) {
            throw new BusinessException("Số điện thoại phải có 8–15 chữ số; chỉ chấp nhận dấu + ở đầu và dấu phân cách thông dụng.");
        }
        // Lưu dạng chữ số chuẩn hóa. Điều này giúp UNIQUE(Phone) cũng chặn được
        // hai request đồng thời nhập cùng số nhưng khác dấu cách/dấu gạch/dấu +.
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() < 8 || digits.length() > 15) {
            throw new BusinessException("Số điện thoại phải có từ 8 đến 15 chữ số.");
        }
        staff.setPhone(digits);
        Optional<Staff> duplicate = staffDao.findDuplicateByNormalizedPhone(digits, currentStaffId);
        if (duplicate.isPresent()) {
            throw new BusinessException("Số điện thoại này đã được sử dụng bởi nhân sự khác.");
        }
    }

    private void validateStaff(Staff staff) {

        if (staff == null) {
            throw new BusinessException("Thông tin nhân sự không được để trống.");
        }
        if (staff.getFullName() == null || staff.getFullName().isBlank()) {
            throw new BusinessException("Họ tên nhân sự không được để trống.");
        }
        if (staff.getFullName().trim().length() > 100) {
            throw new BusinessException("Họ tên không được vượt quá 100 ký tự.");
        }
        if (staff.getPhone() == null || staff.getPhone().isBlank()) {
            throw new BusinessException("Số điện thoại không được để trống.");
        }
        BigDecimal salary = staff.getSalary();
        if (salary == null || salary.signum() <= 0) {
            throw new BusinessException("Lương phải lớn hơn 0.");
        }
        LocalDate hireDate = staff.getHireDate();
        if (hireDate != null && hireDate.isAfter(LocalDate.now())) {
            throw new BusinessException("Ngày vào làm không được ở tương lai.");
        }
        StaffRole expectedRole;
        if (staff instanceof Trainer trainer) {
            expectedRole = StaffRole.TRAINER;
            if (trainer.getSpecialty() == null || trainer.getSpecialty().isBlank()) {
                throw new BusinessException("Chuyên môn của huấn luyện viên không được để trống.");
            }
            if (trainer.getSpecialty().length() > 50) {
                throw new BusinessException("Chuyên môn không được vượt quá 50 ký tự.");
            }
            if (trainer.getCertification() != null && trainer.getCertification().length() > 100) {
                throw new BusinessException("Chứng chỉ không được vượt quá 100 ký tự.");
            }
        } else if (staff instanceof Receptionist receptionist) {
            expectedRole = StaffRole.RECEPTIONIST;
            if (receptionist.getShiftType() == null) {
                throw new BusinessException("Ca làm của lễ tân không được để trống.");
            }
        } else if (staff instanceof Manager) {
            expectedRole = StaffRole.MANAGER;
        } else {
            throw new BusinessException("Loại nhân sự không được hỗ trợ.");
        }
        if (staff.getRole() != null && staff.getRole() != expectedRole) {
            throw new BusinessException("Vai trò không khớp với loại hồ sơ nhân sự.");
        }
    }

    private static void requirePositiveId(Integer id, String label) {
        if (id == null || id <= 0) {
            throw new BusinessException(label + " không hợp lệ.");
        }
    }

    public static void requireStaffAccess(StaffRole role) {
        if (role != StaffRole.MANAGER) {
            throw new BusinessException("Chỉ Quản lý mới được xem được Staff.");
        }
    }
}
