package com.gym.core.service;

import com.gym.core.Config;
import com.gym.core.dao.StaffDao;
import com.gym.core.exception.BusinessException;
import com.gym.core.model.Staff;
import com.gym.core.util.PasswordHasher;

import java.util.Map;

/**
 * Đăng nhập bằng số điện thoại nhân viên (STAFF.Phone) và một mật khẩu chung của phòng tập.
 * Vai trò lấy từ STAFF.Role. Mật khẩu lưu dạng SHA-256 kèm muối trong config.local.properties;
 * chưa đặt thì dùng mật khẩu mặc định {@value #DEFAULT_PASSWORD}.
 *
 * Đây là hạn chế có chủ đích vì thiết kế DB không có bảng tài khoản; hướng mở rộng là thêm bảng ACCOUNT.
 */
public class AuthService {

    public static final String DEFAULT_PASSWORD = "gym123";
    public static final int MIN_PASSWORD_LENGTH = 6;

    private static final String SALT_KEY = "auth.salt";
    private static final String HASH_KEY = "auth.passwordHash";
    private static final String LOGIN_FAILED = "Số điện thoại hoặc mật khẩu không đúng.";

    private final StaffDao staffDao;
    private final Config config;

    public AuthService() {
        this(new StaffDao(), Config.get());
    }

    public AuthService(StaffDao staffDao, Config config) {
        this.staffDao = staffDao;
        this.config = config;
    }

    /** Đăng nhập. Sai thì ném BusinessException (cùng một thông điệp cho cả hai trường hợp sai). */
    public Staff login(String phone, String password) {
        if (phone == null || phone.isBlank() || password == null || password.isEmpty()) {
            throw new BusinessException("Hãy nhập số điện thoại và mật khẩu.");
        }
        Staff staff = staffDao.findByPhone(phone.trim()).orElse(null);
        if (staff == null || !passwordMatches(password)) {
            throw new BusinessException(LOGIN_FAILED);
        }
        return staff;
    }

    /** Đổi mật khẩu chung. Cần nhập đúng mật khẩu cũ. */
    public void changePassword(String oldPassword, String newPassword) {
        if (!passwordMatches(oldPassword)) {
            throw new BusinessException("Mật khẩu hiện tại không đúng.");
        }
        if (newPassword == null || newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new BusinessException("Mật khẩu mới phải có ít nhất " + MIN_PASSWORD_LENGTH + " ký tự.");
        }
        String salt = PasswordHasher.newSalt();
        config.save(Map.of(SALT_KEY, salt, HASH_KEY, PasswordHasher.hash(salt, newPassword)));
    }

    private boolean passwordMatches(String password) {
        String hash = config.get(HASH_KEY, "");
        if (hash.isEmpty()) {
            return DEFAULT_PASSWORD.equals(password);
        }
        return PasswordHasher.matches(config.get(SALT_KEY, ""), hash, password);
    }
}
