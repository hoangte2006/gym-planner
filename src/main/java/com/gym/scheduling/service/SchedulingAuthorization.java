package com.gym.scheduling.service;

import com.gym.app.Session;
import com.gym.core.model.StaffRole;

/** Authorization rules shared by scheduling services. */
final class SchedulingAuthorization {

    private SchedulingAuthorization() {
    }

    static void requireSchedulingAccess() {
        requireAnyRole(
                StaffRole.MANAGER,
                StaffRole.RECEPTIONIST,
                StaffRole.TRAINER
        );
    }

    static void requireManagementAccess() {
        requireAnyRole(StaffRole.MANAGER, StaffRole.RECEPTIONIST);
    }

    private static void requireAnyRole(StaffRole... roles) {
        if (!Session.isLoggedIn()) {
            throw new SecurityException("Bạn cần đăng nhập để sử dụng chức năng này.");
        }
        for (StaffRole role : roles) {
            if (Session.hasRole(role)) {
                return;
            }
        }
        throw new SecurityException("Bạn không có quyền sử dụng chức năng này.");
    }
}
