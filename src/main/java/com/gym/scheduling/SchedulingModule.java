package com.gym.scheduling;

import com.gym.app.AppModule;
import com.gym.core.model.StaffRole;
import com.gym.scheduling.ui.WeeklySchedulePanel;

import javax.swing.JPanel;
import java.util.Set;

/** Module Lịch tập (TV5). */
public class SchedulingModule implements AppModule {

    @Override
    public String getId() {
        return "scheduling";
    }

    @Override
    public String getTitle() {
        return "Lịch tập";
    }

    @Override
    public Set<StaffRole> getAllowedRoles() {
        return Set.of(StaffRole.MANAGER, StaffRole.RECEPTIONIST, StaffRole.TRAINER);
    }

    @Override
    public JPanel createPanel() {
        return new WeeklySchedulePanel();
    }
}
