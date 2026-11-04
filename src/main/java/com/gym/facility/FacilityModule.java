package com.gym.facility;

import com.gym.app.AppModule;
import com.gym.core.dao.EquipmentDao;
import com.gym.core.dao.MaintenanceLogDao;
import com.gym.core.dao.RoomDao;
import com.gym.core.dao.SessionDao;
import com.gym.core.dao.StaffDao;
import com.gym.core.dao.TrainerDao;
import com.gym.core.model.MaintenanceLog;
import com.gym.core.model.StaffRole;
import com.gym.facility.service.*;
import com.gym.facility.ui.FacilityDashboardPanel;

import javax.swing.JPanel;
import java.util.List;
import java.util.Set;

/** Module TV4: quản lý nhân sự, phòng, thiết bị và lịch sử bảo trì. */
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
        StaffDao staffDao = new StaffDao();
        EquipmentDao equipmentDao = new EquipmentDao();
        RoomDao roomDao = new RoomDao();
        MaintenanceLogDao maintenanceLogDao = new MaintenanceLogDao();

        // DAO phục vụ hồ sơ huấn luyện viên.
        TrainerDao trainerDao = new TrainerDao();
        SessionDao sessionDao = new SessionDao();

        StaffService staffService = new StaffService(staffDao, sessionDao);
        RoomService roomService = new RoomService(roomDao, sessionDao, equipmentDao);
        EquipmentService equipmentService = new EquipmentService(equipmentDao, roomDao);
        MaintenanceLogService maintenanceLogService =
                new MaintenanceLogService(maintenanceLogDao, equipmentDao, staffDao);
        TrainerProfileService trainerProfileService =
                new TrainerProfileService(trainerDao, sessionDao);

        return new FacilityDashboardPanel(
                staffService,
                roomService,
                equipmentService,
                maintenanceLogService,
                trainerProfileService
        );
    }
}