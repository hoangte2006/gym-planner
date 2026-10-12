package com.gym.app;

import com.gym.core.model.StaffRole;

/** Người dùng đang đăng nhập. LoginFrame sẽ gọi login(); tạm thời mặc định là Manager để chạy khung. */
public final class Session {

    private static String staffName = "Quản lý (tạm)";
    private static StaffRole role = StaffRole.MANAGER;

    private Session() {
    }

    public static void login(String name, StaffRole newRole) {
        staffName = name;
        role = newRole;
    }

    public static String getStaffName() {
        return staffName;
    }

    public static StaffRole getRole() {
        return role;
    }
}
