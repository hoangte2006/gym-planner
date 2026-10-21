package com.gym.facility.service;

import com.gym.core.dao.PageResult;
import com.gym.core.dao.RoomDao;
import com.gym.core.exception.BusinessException;
import com.gym.core.model.Room;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Luật nghiệp vụ phòng tập; SQL chỉ nằm trong RoomDao. */
public class RoomService {

    private final RoomDao roomDao;

    public RoomService(RoomDao roomDao) {
        this.roomDao = Objects.requireNonNull(roomDao, "RoomDao không được null");
    }

    public Room createRoom(Room room) {
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
        validateRoom(room);
        requirePositiveId(room.getRoomId());
        if (roomDao.findById(room.getRoomId()).isEmpty()) {
            throw new BusinessException("Phòng cần cập nhật không tồn tại.");
        }
        Optional<Room> existing = roomDao.findByName(room.getRoomName().trim());
        if (existing.isPresent() && !Objects.equals(existing.get().getRoomId(), room.getRoomId())) {
            throw new BusinessException("Tên phòng đã được sử dụng.");
        }
        room.setRoomName(room.getRoomName().trim());
        roomDao.update(room);
        return room;
    }

    public Room findById(Integer roomId) {
        requirePositiveId(roomId);
        return roomDao.findById(roomId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy phòng."));
    }

    public Optional<Room> findRoomByName(String roomName) {
        if (roomName == null || roomName.isBlank()) {
            throw new BusinessException("Tên phòng tìm kiếm không được để trống.");
        }
        return roomDao.findByName(roomName.trim());
    }

    public List<Room> findAll() {
        return roomDao.findAll();
    }

    public PageResult<Room> findPage(int page, int pageSize) {
        return roomDao.findPage(page, pageSize);
    }

    public void deleteRoom(Integer roomId) {
        requirePositiveId(roomId);
        if (roomDao.findById(roomId).isEmpty()) {
            throw new BusinessException("Phòng không tồn tại hoặc đã bị xóa.");
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
}
