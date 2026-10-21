package com.gym.facility.service;

import com.gym.core.dao.EquipmentDao;
import com.gym.core.dao.PageResult;
import com.gym.core.dao.RoomDao;
import com.gym.core.exception.BusinessException;
import com.gym.core.model.Equipment;
import com.gym.core.model.EquipmentStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** Luật nghiệp vụ thiết bị. Mọi thao tác giao tiếp dữ liệu thông qua EquipmentDao/RoomDao. */
public class EquipmentService {

    private final EquipmentDao equipmentDao;
    private final RoomDao roomDao;

    public EquipmentService(EquipmentDao equipmentDao, RoomDao roomDao) {
        this.equipmentDao = Objects.requireNonNull(equipmentDao, "EquipmentDao không được null");
        this.roomDao = Objects.requireNonNull(roomDao, "RoomDao không được null");
    }

    public Equipment create(Equipment equipment) {
        validate(equipment, false);
        if (equipment.getEquipmentId() != null) {
            throw new BusinessException("Thiết bị mới không được tự gán mã thiết bị.");
        }
        validateRoom(equipment.getRoomId());
        if (equipment.getOperationalStatus() == null) {
            equipment.setOperationalStatus(EquipmentStatus.ACTIVE);
        }
        return equipmentDao.insert(equipment);
    }

    public Equipment update(Equipment equipment) {
        validate(equipment, true);
        requirePositiveId(equipment.getEquipmentId());
        if (equipmentDao.findById(equipment.getEquipmentId()).isEmpty()) {
            throw new BusinessException("Thiết bị cần cập nhật không tồn tại.");
        }
        validateRoom(equipment.getRoomId());
        equipmentDao.update(equipment);
        return equipment;
    }

    /** Chỉ đổi trạng thái; các trường còn lại được giữ nguyên từ bản ghi DB. */
    public Equipment changeStatus(Integer equipmentId, EquipmentStatus status) {
        requirePositiveId(equipmentId);
        if (status == null) {
            throw new BusinessException("Tình trạng mới không được để trống.");
        }
        Equipment equipment = findById(equipmentId);
        equipment.setOperationalStatus(status);
        equipmentDao.update(equipment);
        return equipment;
    }

    public Equipment findById(Integer equipmentId) {
        requirePositiveId(equipmentId);
        return equipmentDao.findById(equipmentId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy thiết bị."));
    }

    public List<Equipment> findAll() {
        return equipmentDao.findAll();
    }

    public PageResult<Equipment> findPage(int page, int pageSize) {
        return equipmentDao.findPage(page, pageSize);
    }

    public List<Equipment> findByStatus(EquipmentStatus status) {
        if (status == null) {
            throw new BusinessException("Tình trạng lọc không được để trống.");
        }
        return equipmentDao.findByStatus(status);
    }

    public List<Equipment> findByRoom(Integer roomId) {
        requirePositiveId(roomId);
        if (roomDao.findById(roomId).isEmpty()) {
            throw new BusinessException("Phòng được chọn không tồn tại.");
        }
        return equipmentDao.findByRoom(roomId);
    }

    public void delete(Integer equipmentId) {
        requirePositiveId(equipmentId);
        if (equipmentDao.findById(equipmentId).isEmpty()) {
            throw new BusinessException("Thiết bị không tồn tại hoặc đã bị xóa.");
        }
        // Schema ON DELETE CASCADE sẽ xóa nhật ký bảo trì và lượt sử dụng liên quan; UI yêu cầu xác nhận rõ trước khi gọi.
        equipmentDao.deleteById(equipmentId);
    }

    private void validate(Equipment equipment, boolean updating) {
        if (equipment == null) {
            throw new BusinessException("Thông tin thiết bị không được để trống.");
        }
        if (equipment.getEquipmentName() == null || equipment.getEquipmentName().isBlank()) {
            throw new BusinessException("Tên thiết bị không được để trống.");
        }
        equipment.setEquipmentName(equipment.getEquipmentName().trim());
        if (equipment.getEquipmentName().length() > 100) {
            throw new BusinessException("Tên thiết bị không được vượt quá 100 ký tự.");
        }
        if (updating && equipment.getOperationalStatus() == null) {
            throw new BusinessException("Tình trạng thiết bị không được để trống.");
        }
        if (equipment.getRoomId() != null && equipment.getRoomId() <= 0) {
            throw new BusinessException("Mã phòng không hợp lệ.");
        }
        LocalDate purchaseDate = equipment.getPurchaseDate();
        if (purchaseDate != null && purchaseDate.isAfter(LocalDate.now())) {
            throw new BusinessException("Ngày mua không được ở tương lai.");
        }
    }

    private void validateRoom(Integer roomId) {
        if (roomId != null && roomDao.findById(roomId).isEmpty()) {
            throw new BusinessException("Phòng được gán cho thiết bị không tồn tại.");
        }
    }

    private static void requirePositiveId(Integer id) {
        if (id == null || id <= 0) {
            throw new BusinessException("Mã thiết bị / phòng không hợp lệ.");
        }
    }
}
