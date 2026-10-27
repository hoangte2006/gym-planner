package com.gym.scheduling;

import com.gym.app.AppModule;
import com.gym.core.model.StaffRole;
import com.gym.scheduling.service.ClassSchedulingService;
import com.gym.scheduling.ui.ClassWizardPanel;
import com.gym.scheduling.ui.ClassRegistrationPanel;
import com.gym.scheduling.ui.SessionEquipmentPanel;
import com.gym.scheduling.ui.WeeklySchedulePanel;
import com.gym.scheduling.ui.PtBookingPanel;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
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
        return Set.of(
                StaffRole.MANAGER,
                StaffRole.RECEPTIONIST,
                StaffRole.TRAINER
        );
    }

    @Override
    public JPanel createPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane();

        WeeklySchedulePanel weeklySchedulePanel = new WeeklySchedulePanel();
        ClassWizardPanel classWizardPanel = new ClassWizardPanel(
                new ClassSchedulingService(),
                weeklySchedulePanel::refreshSchedule
        );

        tabs.addTab("Lịch tập theo tuần", weeklySchedulePanel);
        if (com.gym.app.Session.hasRole(StaffRole.TRAINER)) {
            tabs.addTab("Lịch của tôi", new WeeklySchedulePanel(
                    new com.gym.scheduling.service.SessionService(), true));
        }
        tabs.addTab("Tạo lớp", classWizardPanel);
        tabs.addTab("Đăng ký lớp", new ClassRegistrationPanel());
        tabs.addTab("Đặt lịch PT", new PtBookingPanel());
        tabs.addTab("Thiết bị buổi tập", new SessionEquipmentPanel());
        panel.add(tabs, BorderLayout.CENTER);
        return panel;
    }
}
