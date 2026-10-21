package com.gym.facility.service;

import com.gym.core.dao.EquipmentDao;
import com.gym.core.dao.MaintenanceLogDao;
import com.gym.core.dao.StaffDao;
import com.gym.core.exception.BusinessException;
import com.gym.core.model.Equipment;
import com.gym.core.model.MaintenanceLog;
import com.gym.core.model.Staff;
import com.gym.core.model.StaffRole;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** Luật nghiệp vụ phiếu bảo trì; cấp Log_no do MaintenanceLogDao xử lý trong transaction có khóa. */
public class MaintenanceLogService {

    private final MaintenanceLogDao maintenanceLogDao;
    private final EquipmentDao equipmentDao;
    private final StaffDao staffDao;

    public MaintenanceLogService(MaintenanceLogDao maintenanceLogDao,
                                 EquipmentDao equipmentDao,
                                 StaffDao staffDao) {
        this.maintenanceLogDao = Objects.requireNonNull(maintenanceLogDao, "MaintenanceLogDao không được null");
        this.equipmentDao = Objects.requireNonNull(equipmentDao, "EquipmentDao không được null");
        this.staffDao = Objects.requireNonNull(staffDao, "StaffDao không được null");
    }

    public MaintenanceLog create(MaintenanceLog log) {
        validate(log);
        if (log.getLogNo() != null) {
            throw new BusinessException("Log_no được hệ thống tự sinh, không nhập thủ công.");
        }
        Equipment equipment = equipmentDao.findById(log.getEquipmentId())
                .orElseThrow(() -> new BusinessException("Thiết bị không tồn tại."));
        if (equipment.getEquipmentId() == null) {
            throw new BusinessException("Thiết bị không có mã hợp lệ.");
        }
        Staff signer = staffDao.findById(log.getLoggedBy())
                .orElseThrow(() -> new BusinessException("Nhân viên ký phiếu không tồn tại."));
        if (signer.getRole() != StaffRole.RECEPTIONIST && signer.getRole() != StaffRole.MANAGER) {
            throw new BusinessException("Phiếu bảo trì phải do Lễ tân hoặc Quản lý ghi nhận (BR-FAC-05).");
        }
        if (log.getMaintenanceDate() == null) {
            log.setMaintenanceDate(LocalDate.now());
        }
        if (log.getCost() == null) {
            log.setCost(BigDecimal.ZERO);
        }
        // MaintenanceLogDao.insert() sinh MAX(Log_no)+1 dưới UPDLOCK,HOLDLOCK và commit cùng INSERT.
        return maintenanceLogDao.insert(log);
    }

    public List<MaintenanceLog> findByEquipment(Integer equipmentId) {
        if (equipmentId == null || equipmentId <= 0) {
            throw new BusinessException("Mã thiết bị không hợp lệ.");
        }
        if (equipmentDao.findById(equipmentId).isEmpty()) {
            throw new BusinessException("Thiết bị không tồn tại.");
        }
        return maintenanceLogDao.findByEquipment(equipmentId);
    }

    public MaintenanceLog findById(Integer equipmentId, Integer logNo) {
        if (equipmentId == null || equipmentId <= 0 || logNo == null || logNo <= 0) {
            throw new BusinessException("Khóa phiếu bảo trì không hợp lệ.");
        }
        return maintenanceLogDao.findById(new com.gym.core.model.MaintenanceLogKey(equipmentId, logNo))
                .orElseThrow(() -> new BusinessException("Không tìm thấy phiếu bảo trì."));
    }

    private void validate(MaintenanceLog log) {
        if (log == null) {
            throw new BusinessException("Thông tin phiếu bảo trì không được để trống.");
        }
        if (log.getEquipmentId() == null || log.getEquipmentId() <= 0) {
            throw new BusinessException("Thiết bị bảo trì không hợp lệ.");
        }
        if (log.getDescription() == null || log.getDescription().isBlank()) {
            throw new BusinessException("Mô tả bảo trì không được để trống.");
        }
        if (log.getDescription().trim().length() > 255) {
            throw new BusinessException("Mô tả bảo trì không được vượt quá 255 ký tự.");
        }
        if (log.getLoggedBy() == null || log.getLoggedBy() <= 0) {
            throw new BusinessException("Nhân viên ghi phiếu không hợp lệ.");
        }
        if (log.getCost() != null && log.getCost().signum() < 0) {
            throw new BusinessException("Chi phí bảo trì không được âm.");
        }
    }
}
