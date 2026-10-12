package com.gym.app;

import com.gym.core.model.Staff;
import com.gym.core.model.StaffRole;

/** Người dùng đang đăng nhập. LoginFrame gọi login(); các module đọc vai trò và tên từ đây. */
public final class Session {

    private static Staff staff;

    private Session() {
    }

    public static void login(Staff loggedIn) {
        staff = loggedIn;
    }

    public static void logout() {
        staff = null;
    }

    public static boolean isLoggedIn() {
        return staff != null;
    }

    /** Nhân viên đang đăng nhập; null nếu chưa đăng nhập. */
    public static Staff getStaff() {
        return staff;
    }

    public static String getStaffName() {
        return staff == null ? "(chưa đăng nhập)" : staff.getFullName();
    }

    /** Vai trò hiện tại. Chưa đăng nhập thì trả về vai trò thấp nhất (Trainer) để không lộ chức năng. */
    public static StaffRole getRole() {
        return staff == null ? StaffRole.TRAINER : staff.getRole();
    }

    public static boolean hasRole(StaffRole role) {
        return staff != null && staff.getRole() == role;
    }
}
