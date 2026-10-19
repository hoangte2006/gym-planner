package com.gym.dashboard;

import com.gym.app.AppModule;
import com.gym.core.model.StaffRole;
import com.gym.dashboard.ui.DashboardPanel;

import javax.swing.JPanel;
import java.util.Set;

/** Module Dashboard (TV1). Quản lý xem toàn bộ, Lễ tân xem số liệu cơ bản, Huấn luyện viên không thấy mục này. */
public class DashboardModule implements AppModule {

    @Override
    public String getId() {
        return "dashboard";
    }

    @Override
    public String getTitle() {
        return "Dashboard";
    }

    @Override
    public Set<StaffRole> getAllowedRoles() {
        return Set.of(StaffRole.MANAGER, StaffRole.RECEPTIONIST);
    }

    @Override
    public JPanel createPanel() {
        return new DashboardPanel();
    }
}
