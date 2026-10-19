package com.gym.billing;

import com.gym.app.AppModule;
import com.gym.billing.service.BillingService;
import com.gym.billing.service.MembershipExpiryScheduler;
import com.gym.billing.ui.BillingWorkspacePanel;
import com.gym.core.model.StaffRole;

import javax.swing.JPanel;
import java.util.Set;

/** Module Gói tập & Thanh toán (TV3), chỉ dành cho Quản lý và Lễ tân. */
public class BillingModule implements AppModule {

    public BillingModule() {
        // ModuleRegistry được nạp khi ứng dụng sau đăng nhập được mở; tác vụ chạy trên daemon thread.
        MembershipExpiryScheduler.start(new BillingService());
    }

    @Override public String getId() { return "billing"; }
    @Override public String getTitle() { return "Gói tập & Thanh toán"; }
    @Override public Set<StaffRole> getAllowedRoles() { return Set.of(StaffRole.MANAGER, StaffRole.RECEPTIONIST); }
    @Override public JPanel createPanel() { return new BillingWorkspacePanel(); }
}
