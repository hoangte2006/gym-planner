package com.gym.member;

import java.util.Set;

import javax.swing.JPanel;

import com.gym.app.AppModule;
import com.gym.core.model.StaffRole;
import com.gym.member.ui.MemberListPanel;

/** Module Hội viên (TV2). */
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
        return new MemberListPanel();
    }
}