
package com.gym.facility.service;

import com.gym.app.Session;
import com.gym.core.dao.EquipmentDao;
import com.gym.core.dao.PageResult;
import com.gym.core.dao.RoomDao;
import com.gym.core.dao.SessionDao;
import com.gym.core.exception.BusinessException;
import com.gym.core.model.Manager;
import com.gym.core.model.Room;
import com.gym.core.model.StaffRole;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RoomServiceTest {

    private FakeRoomDao roomDao;
    private FakeSessionDao sessionDao;
    private FakeEquipmentDao equipmentDao;

    private RoomService roomService;

    @BeforeEach
    void setUp() {
        roomDao = new FakeRoomDao();
        sessionDao = new FakeSessionDao();
        equipmentDao = new FakeEquipmentDao();

        roomService = new RoomService(
                roomDao,
                sessionDao,
                equipmentDao
        );

        // Đăng nhập bằng tài khoản MANAGER.
        Manager manager = new Manager();
        manager.setRole(StaffRole.MANAGER);
        Session.login(manager);
    }

    @AfterEach
    void tearDown() {
        Session.logout();
    }

    // =====================================================
    // HELPER
    // =====================================================

    private Room createSampleRoom(String name, int capacity) {
        Room room = new Room();
        room.setRoomName(name);
        room.setCapacity(capacity);
        room.setFloorLocation("Tầng 1");
        return room;
    }

    // =====================================================
    // CREATE ROOM
    // =====================================================

    @Test
    void createRoom_shouldCreateSuccessfully() {
        Room room = createSampleRoom("Phòng Gym", 30);

        Room result = roomService.createRoom(room);

        assertNotNull(result);
        assertNotNull(result.getRoomId());
        assertEquals("Phòng Gym", result.getRoomName());
        assertEquals(30, result.getCapacity());

        assertEquals(1, roomDao.insertCount);
    }

    @Test
    void createRoom_shouldRejectNullRoom() {
        assertThrows(
                BusinessException.class,
                () -> roomService.createRoom(null)
        );

        assertEquals(0, roomDao.insertCount);
    }

    @Test
    void createRoom_shouldRejectRoomWithExistingId() {
        Room room = createSampleRoom("Phòng Gym", 30);
        room.setRoomId(10);

        assertThrows(
                BusinessException.class,
                () -> roomService.createRoom(room)
        );

        assertEquals(0, roomDao.insertCount);
    }

    @Test
    void createRoom_shouldRejectDuplicateName() {
        Room existing = createSampleRoom("Phòng Gym", 30);
        existing.setRoomId(1);

        roomDao.rooms.add(existing);

        Room newRoom = createSampleRoom("Phòng Gym", 20);

        assertThrows(
                BusinessException.class,
                () -> roomService.createRoom(newRoom)
        );

        assertEquals(0, roomDao.insertCount);
    }

    @Test
    void createRoom_shouldTrimRoomName() {
        Room room = createSampleRoom("  Phòng Gym  ", 30);

        Room result = roomService.createRoom(room);

        assertEquals("Phòng Gym", result.getRoomName());
    }

    @Test
    void createRoom_shouldRejectNameLongerThan50Characters() {
        Room room = createSampleRoom("A".repeat(51), 30);

        assertThrows(
                BusinessException.class,
                () -> roomService.createRoom(room)
        );

        assertEquals(0, roomDao.insertCount);
    }

    @Test
    void createRoom_shouldRejectFloorLocationLongerThan30Characters() {
        Room room = createSampleRoom("Phòng Gym", 30);
        room.setFloorLocation("A".repeat(31));

        assertThrows(
                BusinessException.class,
                () -> roomService.createRoom(room)
        );

        assertEquals(0, roomDao.insertCount);
    }

    // =====================================================
    // UPDATE ROOM
    // =====================================================

    @Test
    void updateRoom_shouldUpdateSuccessfully() {
        Room existing = createSampleRoom("Phòng cũ", 30);
        existing.setRoomId(1);

        roomDao.rooms.add(existing);

        Room updated = createSampleRoom("Phòng mới", 40);
        updated.setRoomId(1);

        Room result = roomService.updateRoom(updated);

        assertNotNull(result);
        assertEquals("Phòng mới", result.getRoomName());
        assertEquals(40, result.getCapacity());

        assertEquals(1, roomDao.updateCount);
    }

    @Test
    void updateRoom_shouldRejectNullRoom() {
        assertThrows(
                BusinessException.class,
                () -> roomService.updateRoom(null)
        );

        assertEquals(0, roomDao.updateCount);
    }

    @Test
    void updateRoom_shouldRejectInvalidId() {
        Room room = createSampleRoom("Phòng Gym", 30);

        assertThrows(
                BusinessException.class,
                () -> roomService.updateRoom(room)
        );
    }

    @Test
    void updateRoom_shouldRejectRoomNotFound() {
        Room room = createSampleRoom("Phòng Gym", 30);
        room.setRoomId(100);

        assertThrows(
                BusinessException.class,
                () -> roomService.updateRoom(room)
        );

        assertEquals(0, roomDao.updateCount);
    }

    @Test
    void updateRoom_shouldRejectDuplicateName() {
        Room room1 = createSampleRoom("Phòng A", 30);
        room1.setRoomId(1);

        Room room2 = createSampleRoom("Phòng B", 30);
        room2.setRoomId(2);

        roomDao.rooms.add(room1);
        roomDao.rooms.add(room2);

        Room updated = createSampleRoom("Phòng B", 40);
        updated.setRoomId(1);

        assertThrows(
                BusinessException.class,
                () -> roomService.updateRoom(updated)
        );

        assertEquals(0, roomDao.updateCount);
    }

    @Test
    void updateRoom_shouldAllowKeepingSameName() {
        Room existing = createSampleRoom("Phòng A", 30);
        existing.setRoomId(1);

        roomDao.rooms.add(existing);

        Room updated = createSampleRoom("Phòng A", 40);
        updated.setRoomId(1);

        assertDoesNotThrow(
                () -> roomService.updateRoom(updated)
        );

        assertEquals(1, roomDao.updateCount);
    }

    @Test
    void updateRoom_shouldAllowReducingCapacityWhenStillEnough() {
        Room existing = createSampleRoom("Phòng A", 50);
        existing.setRoomId(1);

        roomDao.rooms.add(existing);

        roomDao.maxClassCapacity = 30;

        Room updated = createSampleRoom("Phòng A", 40);
        updated.setRoomId(1);

        assertDoesNotThrow(
                () -> roomService.updateRoom(updated)
        );

        assertEquals(1, roomDao.updateCount);
    }

    // =====================================================
    // FIND ROOM
    // =====================================================

    @Test
    void findById_shouldReturnRoom() {
        Room room = createSampleRoom("Phòng A", 30);
        room.setRoomId(1);

        roomDao.rooms.add(room);

        Room result = roomService.findById(1);

        assertNotNull(result);
        assertEquals(1, result.getRoomId());
        assertEquals("Phòng A", result.getRoomName());
    }

    @Test
    void findById_shouldRejectInvalidId() {
        assertThrows(
                BusinessException.class,
                () -> roomService.findById(0)
        );
    }

    @Test
    void findById_shouldRejectRoomNotFound() {
        assertThrows(
                BusinessException.class,
                () -> roomService.findById(100)
        );
    }

    @Test
    void findRoomByName_shouldReturnRoom() {
        Room room = createSampleRoom("Phòng A", 30);
        room.setRoomId(1);

        roomDao.rooms.add(room);

        Optional<Room> result =
                roomService.findRoomByName("Phòng A");

        assertTrue(result.isPresent());
        assertEquals(1, result.get().getRoomId());
    }

    @Test
    void findRoomByName_shouldRejectBlankName() {
        assertThrows(
                BusinessException.class,
                () -> roomService.findRoomByName("   ")
        );
    }

    @Test
    void findRoomByName_shouldTrimKeyword() {
        Room room = createSampleRoom("Phòng A", 30);
        room.setRoomId(1);

        roomDao.rooms.add(room);

        Optional<Room> result =
                roomService.findRoomByName("  Phòng A  ");

        assertTrue(result.isPresent());
    }

    @Test
    void findAll_shouldReturnAllRooms() {
        roomDao.rooms.add(createSampleRoom("Phòng A", 20));
        roomDao.rooms.add(createSampleRoom("Phòng B", 30));

        List<Room> result = roomService.findAll();

        assertEquals(2, result.size());
    }

    @Test
    void searchRooms_shouldReturnAllWhenKeywordIsBlank() {
        roomDao.rooms.add(createSampleRoom("Phòng A", 20));
        roomDao.rooms.add(createSampleRoom("Phòng B", 30));

        List<Room> result = roomService.searchRooms(" ");

        assertEquals(2, result.size());
        assertEquals(0, roomDao.searchCount);
    }

    @Test
    void searchRooms_shouldTrimKeyword() {
        roomDao.rooms.add(createSampleRoom("Phòng A", 20));

        List<Room> result = roomService.searchRooms("  Phòng  ");

        assertEquals(1, result.size());
        assertEquals("Phòng", roomDao.lastSearchKeyword);
    }


    @Test
    void deleteRoom_shouldRejectInvalidId() {
        assertThrows(
                BusinessException.class,
                () -> roomService.deleteRoom(0)
        );

        assertEquals(0, roomDao.deleteCount);
    }

    @Test
    void deleteRoom_shouldRejectRoomNotFound() {
        assertThrows(
                BusinessException.class,
                () -> roomService.deleteRoom(100)
        );

        assertEquals(0, roomDao.deleteCount);
    }

    @Test
    void deleteRoom_shouldRejectRoomReferencedBySession() {
        Room room = createSampleRoom("Phòng A", 30);
        room.setRoomId(1);

        roomDao.rooms.add(room);
        sessionDao.hasSessionInRoom = true;

        assertThrows(
                BusinessException.class,
                () -> roomService.deleteRoom(1)
        );

        assertEquals(0, roomDao.deleteCount);
    }

    @Test
    void deleteRoom_shouldRejectRoomReferencedByEquipment() {
        Room room = createSampleRoom("Phòng A", 30);
        room.setRoomId(1);

        roomDao.rooms.add(room);
        equipmentDao.hasEquipmentInRoom = true;

        assertThrows(
                BusinessException.class,
                () -> roomService.deleteRoom(1)
        );

        assertEquals(0, roomDao.deleteCount);
    }

    // =====================================================
    // AUTHORIZATION
    // =====================================================

    @Test
    void createRoom_shouldRejectNonManager() {
        Session.logout();

        assertThrows(
                BusinessException.class,
                () -> roomService.createRoom(
                        createSampleRoom("Phòng A", 30)
                )
        );

        assertEquals(0, roomDao.insertCount);
    }

    @Test
    void findAll_shouldRejectNonManager() {
        Session.logout();

        assertThrows(
                BusinessException.class,
                () -> roomService.findAll()
        );
    }

    // =====================================================
    // FAKE ROOM DAO
    // =====================================================

    private static class FakeRoomDao extends RoomDao {

        private final List<Room> rooms = new ArrayList<>();

        private int nextId = 1;
        private int insertCount;
        private int updateCount;
        private int deleteCount;
        private int searchCount;

        private long maxClassCapacity;
        private String lastSearchKeyword;

        @Override
        protected String tableName() {
            return "ROOM";
        }

        @Override
        protected String idColumn() {
            return "Room_ID";
        }

        @Override
        protected Room mapRow(ResultSet rs) throws SQLException {
            return null;
        }

        @Override
        public Optional<Room> findById(Integer id) {
            return rooms.stream()
                    .filter(room -> room.getRoomId().equals(id))
                    .findFirst();
        }

        @Override
        public Optional<Room> findByName(String name) {
            return rooms.stream()
                    .filter(room -> room.getRoomName().equalsIgnoreCase(name))
                    .findFirst();
        }

        @Override
        public List<Room> findAll() {
            return new ArrayList<>(rooms);
        }

        @Override
        public Room insert(Room room) {
            insertCount++;

            room.setRoomId(nextId++);
            rooms.add(room);

            return room;
        }

        @Override
        public void update(Room room) {
            updateCount++;

            for (int i = 0; i < rooms.size(); i++) {
                if (rooms.get(i).getRoomId().equals(room.getRoomId())) {
                    rooms.set(i, room);
                    return;
                }
            }
        }

        @Override
        public void deleteById(Integer id) {
            deleteCount++;
            rooms.removeIf(room -> room.getRoomId().equals(id));
        }

        @Override
        public List<Room> search(String keyword) {
            searchCount++;
            lastSearchKeyword = keyword;

            return rooms.stream()
                    .filter(room -> room.getRoomName()
                            .toLowerCase()
                            .contains(keyword.toLowerCase()))
                    .toList();
        }

        public long findMaxClassCapacityInRoom(Integer roomId) {
            return maxClassCapacity;
        }

        @Override
        public PageResult<Room> findPage(int page, int pageSize) {
            return new PageResult<>(
                    rooms,
                    page,
                    pageSize,
                    rooms.size()
            );
        }
    }

    // =====================================================
    // FAKE SESSION DAO
    // =====================================================

    private static class FakeSessionDao extends SessionDao {

        private boolean hasSessionInRoom;

        @Override
        protected String tableName() {
            return "[SESSION]";
        }

        @Override
        protected String idColumn() {
            return "Session_ID";
        }

        @Override
        protected com.gym.core.model.Session mapRow(ResultSet rs)
                throws SQLException {
            return null;
        }

        public List<com.gym.core.model.Session> findByRoom(Integer roomId) {
            return hasSessionInRoom
                    ? List.of((com.gym.core.model.Session) null)
                    : List.of();
        }
    }

    // =====================================================
    // FAKE EQUIPMENT DAO
    // =====================================================

    private static class FakeEquipmentDao extends EquipmentDao {

        private boolean hasEquipmentInRoom;

        @Override
        protected String tableName() {
            return "EQUIPMENT";
        }

        @Override
        protected String idColumn() {
            return "Equipment_ID";
        }

        @Override
        protected com.gym.core.model.Equipment mapRow(ResultSet rs)
                throws SQLException {
            return null;
        }

        public List<com.gym.core.model.Equipment> findByRoom(Integer roomId) {
            return hasEquipmentInRoom
                    ? List.of(new com.gym.core.model.Equipment())
                    : List.of();
        }
    }
}
