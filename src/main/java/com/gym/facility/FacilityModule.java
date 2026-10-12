package com.gym.facility;

import com.gym.app.AppModule;
import com.gym.core.model.StaffRole;
import com.gym.core.ui.PlaceholderPanel;

import javax.swing.JPanel;
import java.util.Set;

/** Module Nhân sự & Cơ sở vật chất (TV4). Chủ module thay nội dung createPanel() bằng màn hình thật. */
public class FacilityModule implements AppModule {

    @Override
    public String getId() {
        return "facility";
    }

    @Override
    public String getTitle() {
        return "Nhân sự & Cơ sở vật chất";
    }

    @Override
    public Set<StaffRole> getAllowedRoles() {
        return Set.of(StaffRole.MANAGER);
    }

    @Override
    public JPanel createPanel() {
        return new PlaceholderPanel(getTitle(), "TV4");
    }
}
