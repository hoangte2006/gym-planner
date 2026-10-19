package com.gym.facility.service;

import com.gym.core.dao.RoomDao;
import com.gym.core.exception.BusinessException;
import com.gym.core.model.Room;

import java.util.Objects;
import java.util.Optional;

public class RoomService {

    private final RoomDao roomDao;

    public RoomService(RoomDao roomDao) {
        this.roomDao = Objects.requireNonNull(
                roomDao, "RoomDao không được null"
        );
    }

    /**
     * Tạo phòng mới sau khi kiểm tra dữ liệu nghiệp vụ.
     */
    public Room createRoom(Room room) {
        validateRoom(room);

        Optional<Room> existing =
                roomDao.findByName(room.getRoomName().trim());

        if (existing.isPresent()) {
            throw new BusinessException("Tên phòng đã tồn tại.");
        }

        room.setRoomName(room.getRoomName().trim());
        return roomDao.insert(room);
    }

    /**
     * Cập nhật thông tin phòng.
     */
    public void updateRoom(Room room) {
        validateRoom(room);

        if (room.getRoomId() <= 0) {
            throw new BusinessException(
                    "Mã phòng không hợp lệ."
            );
        }

        Optional<Room> existing =
                roomDao.findByName(room.getRoomName().trim());

        if (existing.isPresent()
                && !Objects.equals(existing.get().getRoomId(), room.getRoomId())){
            throw new BusinessException("Tên phòng đã được sử dụng.");
        }

        room.setRoomName(room.getRoomName().trim());
        roomDao.update(room);
    }

    /**
     * Tìm phòng theo tên.
     */
    public Optional<Room> findRoomByName(String roomName) {
        if (roomName == null || roomName.isBlank()) {
            throw new BusinessException(
                    "Tên phòng tìm kiếm không được để trống."
            );
        }

        return roomDao.findByName(roomName.trim());
    }

    private void validateRoom(Room room) {
        if (room == null) {
            throw new BusinessException(
                    "Thông tin phòng không được để trống."
            );
        }

        if (room.getRoomName() == null
                || room.getRoomName().isBlank()) {
            throw new BusinessException(
                    "Tên phòng không được để trống."
            );
        }

        if (room.getCapacity() <= 0) {
            throw new BusinessException(
                    "Sức chứa phòng phải lớn hơn 0."
            );
        }
    }
}
