package com.gym.facility.service;

import com.gym.app.Session;
import com.gym.core.dao.EquipmentDao;
import com.gym.core.dao.PageResult;
import com.gym.core.dao.RoomDao;
import com.gym.core.dao.SessionDao;
import com.gym.core.exception.BusinessException;
import com.gym.core.model.Room;
import com.gym.core.model.StaffRole;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Luật nghiệp vụ phòng tập; SQL chỉ nằm trong RoomDao. */
public class RoomService {

    private final RoomDao roomDao;
    private final SessionDao sessionDao;
    private final EquipmentDao equipmentDao;

    public RoomService(RoomDao roomDao, SessionDao sessionDao, EquipmentDao equipmentDao) {
        this.roomDao = Objects.requireNonNull(roomDao, "RoomDao không được null");
        this.sessionDao = Objects.requireNonNull(sessionDao, "SessionDao không được null");
        this.equipmentDao = Objects.requireNonNull(equipmentDao, "equipmentDao không được null");
    }

    public Room createRoom(Room room) {
        requireRoomAccess(Session.getRole());

        validateRoom(room);
        if (room.getRoomId() != null) {
            throw new BusinessException("Phòng mới không được tự gán mã phòng.");
        }
        if (roomDao.findByName(room.getRoomName().trim()).isPresent()) {
            throw new BusinessException("Tên phòng đã tồn tại.");
        }
        room.setRoomName(room.getRoomName().trim());
        return roomDao.insert(room);
    }

    public Room updateRoom(Room room) {

        requireRoomAccess(Session.getRole());

        // Bước 1: Kiểm tra dữ liệu đầu vào.
        validateRoom(room);
        requirePositiveId(room.getRoomId());

        // Bước 2: Kiểm tra phòng có tồn tại không.
        Room existingRoom = roomDao.findById(room.getRoomId())
                .orElseThrow(() ->
                        new BusinessException("Phòng cần cập nhật không tồn tại.")
                );

        // Bước 3: Kiểm tra tên phòng có bị trùng không.
        Optional<Room> existingName =
                roomDao.findByName(room.getRoomName().trim());

        if (existingName.isPresent()
                && !Objects.equals(
                existingName.get().getRoomId(),
                room.getRoomId()
        )) {

            throw new BusinessException("Tên phòng đã được sử dụng.");
        }

        // Bước 4: Chỉ kiểm tra sức chứa khi người dùng giảm Capacity.
        if (room.getCapacity() < existingRoom.getCapacity()) {

            long maxClassCapacity =
                    roomDao.findMaxClassCapacityInRoom(room.getRoomId());

            if (room.getCapacity() < maxClassCapacity) {

                throw new BusinessException(
                        "Không thể giảm sức chứa phòng xuống "
                                + room.getCapacity()
                                + " người. "
                                + "Phòng hiện có lớp yêu cầu sức chứa tối đa "
                                + maxClassCapacity
                                + " người. "
                                + "Vui lòng điều chỉnh lớp trước."
                );
            }
        }

        // Bước 5: Chuẩn hóa dữ liệu.
        room.setRoomName(room.getRoomName().trim());

        // Bước 6: Cập nhật database.
        roomDao.update(room);

        return room;
    }

    public Room findById(Integer roomId) {
        requireRoomAccess(Session.getRole());

        requirePositiveId(roomId);
        return roomDao.findById(roomId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy phòng."));
    }

    public Optional<Room> findRoomByName(String roomName) {
        requireRoomAccess(Session.getRole());

        if (roomName == null || roomName.isBlank()) {
            throw new BusinessException("Tên phòng tìm kiếm không được để trống.");
        }
        return roomDao.findByName(roomName.trim());
    }

    public List<Room> findAll() {
        requireRoomAccess(Session.getRole());

        return roomDao.findAll();
    }

    public PageResult<Room> findPage(int page, int pageSize) {
        requireRoomAccess(Session.getRole());

        return roomDao.findPage(page, pageSize);
    }

    /**
     * Tìm phòng theo từ khóa.
     */
    public List<Room> searchRooms(String keyword) {
        requireRoomAccess(Session.getRole());

        if (keyword == null || keyword.isBlank()) {
            return roomDao.findAll();
        }

        return roomDao.search(keyword.trim());
    }

    public void deleteRoom(Integer roomId) {
        requireRoomAccess(Session.getRole());

        requirePositiveId(roomId);
        if (roomDao.findById(roomId).isEmpty()) {
            throw new BusinessException("Phòng không tồn tại hoặc đã bị xóa.");
        }

        if(!sessionDao.findByRoom(roomId).isEmpty()) {
            throw new BusinessException("Phòng này đang được tham chiếu");
        }

        if(!equipmentDao.findByRoom(roomId).isEmpty()) {
            throw new BusinessException("Thiết bị này đã được tham chiếu");
        }

        // Nếu phòng đang được SESSION tham chiếu, FK sẽ chặn xóa; lỗi được DAO dịch.
        roomDao.deleteById(roomId);
    }

    private void validateRoom(Room room) {
        if (room == null) {
            throw new BusinessException("Thông tin phòng không được để trống.");
        }
        if (room.getRoomName() == null || room.getRoomName().isBlank()) {
            throw new BusinessException("Tên phòng không được để trống.");
        }
        if (room.getRoomName().trim().length() > 50) {
            throw new BusinessException("Tên phòng không được vượt quá 50 ký tự.");
        }
        if (room.getCapacity() <= 0) {
            throw new BusinessException("Sức chứa phòng phải lớn hơn 0.");
        }
        if (room.getFloorLocation() != null && room.getFloorLocation().length() > 30) {
            throw new BusinessException("Tầng / vị trí không được vượt quá 30 ký tự.");
        }
    }

    private static void requirePositiveId(Integer roomId) {
        if (roomId == null || roomId <= 0) {
            throw new BusinessException("Mã phòng không hợp lệ.");
        }
    }

    public static void requireRoomAccess(StaffRole role) {
        if (role != StaffRole.MANAGER) {
            throw new BusinessException("Chỉ Quản lý mới dùng được chức năng này.");
        }
    }
}
