package com.gym.core.dao;

import com.gym.core.model.Room;
import com.gym.core.model.Staff;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
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

    /**
     * Tìm phòng theo mã, tên, sức chứa hoặc tầng/vị trí.
     */
    public List<Room> search(String keyword) {
        String sql = "SELECT * FROM ROOM "
                + "WHERE CAST(Room_ID AS VARCHAR(20)) LIKE ? "
                + "OR LOWER(Room_Name) LIKE ? "
                + "OR CAST(Capacity AS VARCHAR(20)) LIKE ? "
                + "OR LOWER(COALESCE(Floor_Location, '')) LIKE ? "
                + "ORDER BY Room_ID";

        String value = keyword == null ? "" : keyword.trim().toLowerCase(java.util.Locale.ROOT);
        String pattern = "%" + value + "%";

        return queryList(sql, ps -> {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            ps.setString(4, pattern);
        });
    }


    /**
     * Phân trang danh sách phòng.
     */
    public List<Room> findRoomsPaged(int page, int pageSize) {
        if (page < 1 || pageSize < 1) {
            throw new IllegalArgumentException(
                    "Trang và kích thước trang phải lớn hơn 0."
            );
        }

        int offset = (page - 1) * pageSize;

        String sql = "SELECT * FROM ROOM "
                + "ORDER BY Room_ID "
                + "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        return queryList(sql, ps -> {
            ps.setInt(1, offset);
            ps.setInt(2, pageSize);
        });
    }

}
