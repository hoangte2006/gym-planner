package com.gym.member;

import com.gym.app.AppModule;
import com.gym.core.model.StaffRole;
import com.gym.core.ui.PlaceholderPanel;

import javax.swing.JPanel;
import java.util.Set;

/** Module Hội viên (TV2). Chủ module thay nội dung createPanel() bằng màn hình thật. */
public class MemberModule implements AppModule {

    @Override
    public String getId() {
        return "member";
    }

    @Override
    public String getTitle() {
        return "Hội viên";
    }

    @Override
    public Set<StaffRole> getAllowedRoles() {
        return Set.of(StaffRole.MANAGER, StaffRole.RECEPTIONIST);
    }

    @Override
    public JPanel createPanel() {
        return new PlaceholderPanel(getTitle(), "TV2");
    }
}
