package com.gym.core.model;

import com.gym.core.util.Check;

/** Phòng tập (bảng ROOM). */
public class Room {

    private Integer roomId;
    private String roomName;
    private int capacity;
    private String floorLocation;

    /** Mã phòng. */
    public Integer getRoomId() {
        return roomId;
    }

    public void setRoomId(Integer roomId) {
        this.roomId = roomId;
    }

    /** Tên phòng. */
    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = Check.text(roomName, "Tên phòng");
    }

    /** Sức chứa. */
    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = Check.positive(capacity, "Sức chứa");
    }

    /** Tầng. */
    public String getFloorLocation() {
        return floorLocation;
    }

    public void setFloorLocation(String floorLocation) {
        this.floorLocation = floorLocation;
    }
}
