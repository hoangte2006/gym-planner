package com.gym.app;

import com.gym.billing.BillingModule;
import com.gym.core.model.StaffRole;
import com.gym.dashboard.DashboardModule;
import com.gym.facility.FacilityModule;
import com.gym.member.MemberModule;
import com.gym.scheduling.SchedulingModule;

import java.util.ArrayList;
import java.util.List;

/**
 * Danh sách module của ứng dụng. TV1 đã đăng ký sẵn 4 module nghiệp vụ,
 * chủ module chỉ điền nội dung bên trong lớp module của mình, không sửa file này.
 */
public final class ModuleRegistry {

    private static final List<AppModule> MODULES = List.of(
            new DashboardModule(),
            new MemberModule(),
            new BillingModule(),
            new SchedulingModule(),
            new FacilityModule());

    private ModuleRegistry() {
    }

    public static List<AppModule> all() {
        return MODULES;
    }

    /** Các module mà vai trò này được phép vào, giữ nguyên thứ tự menu. */
    public static List<AppModule> forRole(StaffRole role) {
        List<AppModule> result = new ArrayList<>();
        for (AppModule m : MODULES) {
            if (m.getAllowedRoles().contains(role)) {
                result.add(m);
            }
        }
        return result;
    }
}
