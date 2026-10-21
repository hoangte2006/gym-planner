package com.gym.facility;

import com.gym.app.AppModule;
import com.gym.core.dao.EquipmentDao;
import com.gym.core.dao.MaintenanceLogDao;
import com.gym.core.dao.RoomDao;
import com.gym.core.dao.StaffDao;
import com.gym.core.model.StaffRole;
import com.gym.facility.service.EquipmentService;
import com.gym.facility.service.MaintenanceLogService;
import com.gym.facility.service.RoomService;
import com.gym.facility.service.StaffService;
import com.gym.facility.ui.FacilityDashboardPanel;

import javax.swing.JPanel;
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
        // Mỗi màn hình chỉ biết Service; Service phụ thuộc DAO; DAO mới thực hiện SQL.
        StaffDao staffDao = new StaffDao();
        RoomDao roomDao = new RoomDao();
        EquipmentDao equipmentDao = new EquipmentDao();
        MaintenanceLogDao maintenanceLogDao = new MaintenanceLogDao();

        StaffService staffService = new StaffService(staffDao);
        RoomService roomService = new RoomService(roomDao);
        EquipmentService equipmentService = new EquipmentService(equipmentDao, roomDao);
        MaintenanceLogService maintenanceLogService =
                new MaintenanceLogService(maintenanceLogDao, equipmentDao, staffDao);

        return new FacilityDashboardPanel(staffService, roomService, equipmentService, maintenanceLogService);
    }
}
