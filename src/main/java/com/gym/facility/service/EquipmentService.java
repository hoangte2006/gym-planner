
package com.gym.facility.service;

import com.gym.core.dao.EquipmentDao;
import com.gym.core.dao.RoomDao;
import com.gym.core.exception.BusinessException;
import com.gym.core.model.Equipment;
import com.gym.core.model.EquipmentStatus;
import com.gym.core.model.Room;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class EquipmentService {

    private final EquipmentDao equipmentDao;
    private final RoomDao roomDao;

    public EquipmentService(
            EquipmentDao equipmentDao,
            RoomDao roomDao
    ) {
        this.equipmentDao = equipmentDao;
        this.roomDao = roomDao;
    }

    /**
     * Thêm thiết bị mới.
     */
    public Equipment create(Equipment equipment) {
        validate(equipment, false);

        if (equipment.getEquipmentId() != null) {
            throw new BusinessException(
                    "Không được tự nhập mã thiết bị khi thêm mới."
            );
        }

        validateRoom(equipment.getRoomId());

        return equipmentDao.insert(equipment);
    }

    /**
     * Cập nhật thiết bị.
     */
    public void update(Equipment equipment) {
        validate(equipment, true);

        Integer id = equipment.getEquipmentId();

        if (id == null || id <= 0) {
            throw new BusinessException(
                    "Mã thiết bị không hợp lệ."
            );
        }

        if (equipmentDao.findById(id).isEmpty()) {
            throw new BusinessException(
                    "Thiết bị không tồn tại."
            );
        }

        validateRoom(equipment.getRoomId());

        equipmentDao.update(equipment);
    }

    /**
     * Lấy thiết bị theo mã.
     */
    public Equipment findById(Integer id) {
        if (id == null || id <= 0) {
            throw new BusinessException(
                    "Mã thiết bị không hợp lệ."
            );
        }

        return equipmentDao.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "Không tìm thấy thiết bị."
                ));
    }

    /**
     * Lấy toàn bộ thiết bị.
     */
    public List<Equipment> findAll() {
        return equipmentDao.findAll();
    }

    /**
     * Lọc theo tình trạng.
     */
    public List<Equipment> findByStatus(
            EquipmentStatus status
    ) {
        if (status == null) {
            throw new BusinessException(
                    "Vui lòng chọn tình trạng thiết bị."
            );
        }

        return equipmentDao.findByStatus(status);
    }

    /**
     * Lấy thiết bị thuộc một phòng.
     */
    public List<Equipment> findByRoom(Integer roomId) {
        if (roomId == null || roomId <= 0) {
            throw new BusinessException(
                    "Mã phòng không hợp lệ."
            );
        }

        if (roomDao.findById(roomId).isEmpty()) {
            throw new BusinessException(
                    "Phòng không tồn tại."
            );
        }

        return equipmentDao.findByRoom(roomId);
    }

    /**
     * Xóa thiết bị.
     */
    public void delete(Integer id) {
        if (id == null || id <= 0) {
            throw new BusinessException(
                    "Mã thiết bị không hợp lệ."
            );
        }

        if (equipmentDao.findById(id).isEmpty()) {
            throw new BusinessException(
                    "Thiết bị không tồn tại hoặc đã bị xóa."
            );
        }

        equipmentDao.deleteById(id);
    }

    /**
     * Kiểm tra dữ liệu đầu vào.
     *
     * isUpdate = false: thêm mới.
     * isUpdate = true: cập nhật.
     */
    private void validate(
            Equipment equipment,
            boolean isUpdate
    ) {
        if (equipment == null) {
            throw new BusinessException(
                    "Thông tin thiết bị không được để trống."
            );
        }

        String name = equipment.getEquipmentName();

        if (name == null || name.isBlank()) {
            throw new BusinessException(
                    "Tên thiết bị không được để trống."
            );
        }

        equipment.setEquipmentName(name.trim());

        Integer id = equipment.getEquipmentId();

        if (id != null && id <= 0) {
            throw new BusinessException(
                    "Mã thiết bị phải lớn hơn 0."
            );
        }

        if (isUpdate && id == null) {
            throw new BusinessException(
                    "Thiếu mã thiết bị cần cập nhật."
            );
        }

        Integer roomId = equipment.getRoomId();

        if (roomId != null && roomId <= 0) {
            throw new BusinessException(
                    "Mã phòng phải lớn hơn 0."
            );
        }

        if (equipment.getPurchaseDate() != null
                && equipment.getPurchaseDate()
                .isAfter(LocalDate.now())) {
            throw new BusinessException(
                    "Ngày mua không được ở tương lai."
            );
        }

        // Khi thêm mới, cho phép trạng thái null
        // để SQL Server áp dụng DEFAULT 'Active'.
        if (isUpdate
                && equipment.getOperationalStatus() == null) {
            throw new BusinessException(
                    "Tình trạng thiết bị không được để trống."
            );
        }
    }

    /**
     * Phòng là trường tùy chọn.
     * Nếu có mã phòng thì phải tồn tại trong DB.
     */
    private void validateRoom(Integer roomId) {
        if (roomId == null) {
            return;
        }

        Optional<Room> room = roomDao.findById(roomId);

        if (room.isEmpty()) {
            throw new BusinessException(
                    "Phòng có mã " + roomId
                            + " không tồn tại."
            );
        }
    }
}
