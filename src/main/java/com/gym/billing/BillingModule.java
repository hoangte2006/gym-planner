package com.gym.billing;

import com.gym.app.AppModule;
import com.gym.core.model.StaffRole;
import com.gym.core.ui.PlaceholderPanel;

import javax.swing.JPanel;
import java.util.Set;

/** Module Gói tập & Thanh toán (TV3). Chủ module thay nội dung createPanel() bằng màn hình thật. */
public class BillingModule implements AppModule {

    @Override
    public String getId() {
        return "billing";
    }

    @Override
    public String getTitle() {
        return "Gói tập & Thanh toán";
    }

    @Override
    public Set<StaffRole> getAllowedRoles() {
        return Set.of(StaffRole.MANAGER, StaffRole.RECEPTIONIST);
    }

    @Override
    public JPanel createPanel() {
        return new PlaceholderPanel(getTitle(), "TV3");
    }
}
