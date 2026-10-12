package com.gym.core.dao;

import com.gym.core.model.Room;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * DAO mẫu cho bảng ROOM. Đây là khuôn để viết 16 DAO còn lại:
 * khai báo bảng, khóa, cách đọc một dòng, rồi viết INSERT và UPDATE.
 */
public class RoomDao extends AbstractJdbcDao<Room, Integer> {

    @Override
    protected String tableName() {
        return "ROOM";
    }

    @Override
    protected String idColumn() {
        return "Room_ID";
    }

    /** Đọc một dòng của ROOM thành đối tượng Room. Tên cột phải khớp bảng trong 01_create_tables.sql. */
    @Override
    protected Room mapRow(ResultSet rs) throws SQLException {
        Room r = new Room();
        r.setRoomId(rs.getInt("Room_ID"));
        r.setRoomName(rs.getString("Room_Name"));
        r.setCapacity(rs.getInt("Capacity"));
        r.setFloorLocation(rs.getString("Floor_Location"));   // cột này cho phép NULL, getString trả null được
        return r;
    }

    @Override
    public Room insert(Room room) {
        int newId = insertAndGetKey(
                "INSERT INTO ROOM (Room_Name, Capacity, Floor_Location) VALUES (?, ?, ?)",
                ps -> {
                    ps.setString(1, room.getRoomName());
                    ps.setInt(2, room.getCapacity());
                    ps.setString(3, room.getFloorLocation());
                });
        room.setRoomId(newId);
        return room;
    }

    @Override
    public void update(Room room) {
        execute("UPDATE ROOM SET Room_Name = ?, Capacity = ?, Floor_Location = ? WHERE Room_ID = ?",
                ps -> {
                    ps.setString(1, room.getRoomName());
                    ps.setInt(2, room.getCapacity());
                    ps.setString(3, room.getFloorLocation());
                    ps.setInt(4, room.getRoomId());
                });
    }

    /** Truy vấn riêng của bảng này (ngoài CRUD chung): tìm phòng theo tên. */
    public Optional<Room> findByName(String roomName) {
        return queryOne("SELECT * FROM ROOM WHERE Room_Name = ?", ps -> ps.setString(1, roomName));
    }
}
