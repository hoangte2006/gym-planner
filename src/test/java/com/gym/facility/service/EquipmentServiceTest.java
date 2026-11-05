
package com.gym.facility.service;

import com.gym.app.Session;
import com.gym.core.dao.*;
import com.gym.core.exception.BusinessException;
import com.gym.core.model.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class EquipmentServiceTest {

    private StubEquipmentDao equipmentDao;
    private StubRoomDao roomDao;
    private StubSessionDao sessionDao;
    private StubUsageDao usageDao;

    private EquipmentService equipmentService;

    @BeforeEach
    void setUp() {
        // Đăng nhập bằng tài khoản MANAGER
        Manager manager = new Manager();
        manager.setRole(StaffRole.MANAGER);
        Session.login(manager);

        // Khởi tạo các Stub DAO
        equipmentDao = new StubEquipmentDao();
        roomDao = new StubRoomDao();
        sessionDao = new StubSessionDao();
        usageDao = new StubUsageDao();

        // Inject đủ 4 DAO
        equipmentService = new EquipmentService(
                equipmentDao,
                roomDao,
                sessionDao,
                usageDao
        );
    }

    @AfterEach
    void tearDown() {
        Session.logout();
    }

    // =====================================================
    // CREATE
    // =====================================================

    @Test
    void create_shouldInsertEquipmentSuccessfully() {

        Equipment equipment = createEquipment("Máy chạy bộ");

        Equipment result = equipmentService.create(equipment);

        assertNotNull(result);
        assertEquals("Máy chạy bộ", result.getEquipmentName());
        assertEquals(EquipmentStatus.ACTIVE,
                result.getOperationalStatus());

        assertEquals(1, equipmentDao.insertCount);
    }

    @Test
    void create_shouldTrimEquipmentName() {

        Equipment equipment = createEquipment("  Máy chạy bộ  ");

        equipmentService.create(equipment);

        assertEquals("Máy chạy bộ", equipment.getEquipmentName());
    }

    @Test
    void create_shouldRejectNullEquipment() {

        assertThrows(
                BusinessException.class,
                () -> equipmentService.create(null)
        );

        assertEquals(0, equipmentDao.insertCount);
    }

    @Test
    void create_shouldRejectEquipmentWithExistingId() {

        Equipment equipment = createEquipment("Máy chạy bộ");
        equipment.setEquipmentId(10);

        assertThrows(
                BusinessException.class,
                () -> equipmentService.create(equipment)
        );

        assertEquals(0, equipmentDao.insertCount);
    }

    @Test
    void create_shouldRejectInvalidRoom() {

        Equipment equipment = createEquipment("Máy chạy bộ");
        equipment.setRoomId(99);

        assertThrows(
                BusinessException.class,
                () -> equipmentService.create(equipment)
        );

        assertEquals(0, equipmentDao.insertCount);
    }

    @Test
    void create_shouldRejectFuturePurchaseDate() {

        Equipment equipment = createEquipment("Máy chạy bộ");
        equipment.setPurchaseDate(LocalDate.now().plusDays(1));

        assertThrows(
                BusinessException.class,
                () -> equipmentService.create(equipment)
        );

        assertEquals(0, equipmentDao.insertCount);
    }

    // =====================================================
    // UPDATE
    // =====================================================

    @Test
    void update_shouldUpdateEquipmentSuccessfully() {

        Equipment equipment = createEquipment("Máy chạy bộ");
        equipment.setEquipmentId(1);
        equipment.setOperationalStatus(EquipmentStatus.ACTIVE);

        equipmentDao.save(equipment);

        equipment.setEquipmentName("Máy chạy bộ mới");

        Equipment result = equipmentService.update(equipment);

        assertEquals("Máy chạy bộ mới", result.getEquipmentName());
        assertEquals(1, equipmentDao.updateCount);
    }

    @Test
    void update_shouldRejectEquipmentNotFound() {

        Equipment equipment = createEquipment("Máy chạy bộ");
        equipment.setEquipmentId(99);
        equipment.setOperationalStatus(EquipmentStatus.ACTIVE);

        assertThrows(
                BusinessException.class,
                () -> equipmentService.update(equipment)
        );

        assertEquals(0, equipmentDao.updateCount);
    }

    @Test
    void update_shouldRejectNullStatus() {

        Equipment equipment = createEquipment("Máy chạy bộ");
        equipment.setEquipmentId(1);
        equipment.setOperationalStatus(null);

        equipmentDao.save(equipment);

        assertThrows(
                BusinessException.class,
                () -> equipmentService.update(equipment)
        );
    }

    // =====================================================
    // CHANGE STATUS
    // =====================================================

    @Test
    void changeStatus_shouldUpdateStatusSuccessfully() {

        Equipment equipment = createEquipment("Máy chạy bộ");
        equipment.setEquipmentId(1);
        equipment.setOperationalStatus(EquipmentStatus.ACTIVE);

        equipmentDao.save(equipment);

        Equipment result = equipmentService.changeStatus(
                1,
                EquipmentStatus.BROKEN
        );

        assertEquals(
                EquipmentStatus.BROKEN,
                result.getOperationalStatus()
        );

        assertEquals(1, equipmentDao.updateCount);
    }

    @Test
    void changeStatus_shouldRejectNullStatus() {

        assertThrows(
                BusinessException.class,
                () -> equipmentService.changeStatus(1, null)
        );
    }

    @Test
    void changeStatus_shouldRejectInvalidId() {

        assertThrows(
                BusinessException.class,
                () -> equipmentService.changeStatus(0, EquipmentStatus.BROKEN)
        );
    }

    // =====================================================
    // FIND
    // =====================================================

    @Test
    void findById_shouldReturnEquipment() {

        Equipment equipment = createEquipment("Máy chạy bộ");
        equipment.setEquipmentId(1);

        equipmentDao.save(equipment);

        Equipment result = equipmentService.findById(1);

        assertNotNull(result);
        assertEquals(1, result.getEquipmentId());
    }

    @Test
    void findById_shouldThrowWhenNotFound() {

        assertThrows(
                BusinessException.class,
                () -> equipmentService.findById(99)
        );
    }

    @Test
    void findAll_shouldReturnAllEquipment() {

        equipmentDao.save(createEquipmentWithId(1, "Máy chạy bộ"));
        equipmentDao.save(createEquipmentWithId(2, "Máy đạp xe"));

        List<Equipment> result = equipmentService.findAll();

        assertEquals(2, result.size());
    }

    @Test
    void findPage_shouldReturnPageResult() {

        equipmentDao.save(createEquipmentWithId(1, "Máy chạy bộ"));

        PageResult<Equipment> result =
                equipmentService.findPage(1, 10);

        assertEquals(1, result.page());
        assertEquals(10, result.pageSize());
        assertEquals(1, result.totalItems());
    }

    @Test
    void findByStatus_shouldReturnEquipmentByStatus() {

        equipmentDao.save(createEquipmentWithId(1, "Máy chạy bộ"));

        List<Equipment> result =
                equipmentService.findByStatus(EquipmentStatus.ACTIVE);

        assertEquals(1, result.size());
    }

    @Test
    void findByStatus_shouldRejectNullStatus() {

        assertThrows(
                BusinessException.class,
                () -> equipmentService.findByStatus(null)
        );
    }

    @Test
    void findByRoom_shouldReturnEquipmentInRoom() {

        Room room = new Room();
        room.setRoomId(10);
        roomDao.save(room);

        Equipment equipment = createEquipment("Máy chạy bộ");
        equipment.setEquipmentId(1);
        equipment.setRoomId(10);

        equipmentDao.save(equipment);

        List<Equipment> result = equipmentService.findByRoom(10);

        assertEquals(1, result.size());
        assertEquals(10, result.get(0).getRoomId());
    }

    @Test
    void findByRoom_shouldRejectRoomNotFound() {

        assertThrows(
                BusinessException.class,
                () -> equipmentService.findByRoom(99)
        );
    }

    // =====================================================
    // SEARCH
    // =====================================================

    @Test
    void searchEquipment_shouldReturnAllWhenKeywordBlank() {

        equipmentDao.save(createEquipmentWithId(1, "Máy chạy bộ"));
        equipmentDao.save(createEquipmentWithId(2, "Máy đạp xe"));

        List<Equipment> result =
                equipmentService.searchEquipment("   ");

        assertEquals(2, result.size());
    }

    // =====================================================
    // DELETE
    // =====================================================

    @Test
    void delete_shouldDeleteEquipmentSuccessfully() {

        equipmentDao.save(createEquipmentWithId(1, "Máy chạy bộ"));

        equipmentService.delete(1);

        assertTrue(equipmentDao.findById(1).isEmpty());
        assertEquals(1, equipmentDao.deleteCount);
    }

    @Test
    void delete_shouldRejectEquipmentNotFound() {

        assertThrows(
                BusinessException.class,
                () -> equipmentService.delete(99)
        );

        assertEquals(0, equipmentDao.deleteCount);
    }

    // =====================================================
    // HEALTH WARNINGS
    // =====================================================

    @Test
    void findHealthWarningsPage_shouldRejectInvalidPage() {

        assertThrows(
                BusinessException.class,
                () -> equipmentService.findHealthWarningsPage(0, 10)
        );
    }

    @Test
    void findHealthWarningsPage_shouldReturnPage() {

        PageResult<Map<String, Object>> result =
                equipmentService.findHealthWarningsPage(1, 10);

        assertNotNull(result);
        assertEquals(1, result.page());
        assertEquals(10, result.pageSize());
    }

    // =====================================================
    // PERMISSION
    // =====================================================

    @Test
    void create_shouldRejectNonManager() {

        Session.logout();

        assertThrows(
                BusinessException.class,
                () -> equipmentService.create(createEquipment("Máy chạy bộ"))
        );
    }

    @Test
    void findAll_shouldRejectNonManager() {

        Session.logout();

        assertThrows(
                BusinessException.class,
                () -> equipmentService.findAll()
        );
    }

    // =====================================================
    // HELPER
    // =====================================================

    private Equipment createEquipment(String name) {

        Equipment equipment = new Equipment();
        equipment.setEquipmentName(name);
        equipment.setOperationalStatus(EquipmentStatus.ACTIVE);
        equipment.setPurchaseDate(LocalDate.now());

        return equipment;
    }

    private Equipment createEquipmentWithId(Integer id, String name) {

        Equipment equipment = createEquipment(name);
        equipment.setEquipmentId(id);

        return equipment;
    }

    // =====================================================
    // STUB EQUIPMENT DAO
    // =====================================================

    private static class StubEquipmentDao extends EquipmentDao {

        private final Map<Integer, Equipment> data = new LinkedHashMap<>();

        private int nextId = 1;

        int insertCount;
        int updateCount;
        int deleteCount;

        void save(Equipment equipment) {
            data.put(equipment.getEquipmentId(), equipment);
        }

        @Override
        public Equipment insert(Equipment equipment) {

            insertCount++;

            equipment.setEquipmentId(nextId++);
            data.put(equipment.getEquipmentId(), equipment);

            return equipment;
        }

        @Override
        public void update(Equipment equipment) {

            updateCount++;

            data.put(equipment.getEquipmentId(), equipment);
        }

        @Override
        public Optional<Equipment> findById(Integer id) {
            return Optional.ofNullable(data.get(id));
        }

        @Override
        public List<Equipment> findAll() {
            return new ArrayList<>(data.values());
        }

        @Override
        public PageResult<Equipment> findPage(int page, int pageSize) {

            List<Equipment> all = findAll();

            return new PageResult<>(
                    all,
                    page,
                    pageSize,
                    all.size()
            );
        }

        @Override
        public List<Equipment> findByStatus(EquipmentStatus status) {

            return data.values().stream()
                    .filter(e -> e.getOperationalStatus() == status)
                    .toList();
        }

        @Override
        public List<Equipment> findByRoom(int roomId) {

            return data.values().stream()
                    .filter(e -> Objects.equals(e.getRoomId(), roomId))
                    .toList();
        }

        @Override
        public List<Equipment> search(String keyword) {

            return data.values().stream()
                    .filter(e -> e.getEquipmentName()
                            .toLowerCase()
                            .contains(keyword.toLowerCase()))
                    .toList();
        }

        @Override
        public void deleteById(Integer id) {

            deleteCount++;
            data.remove(id);
        }

        @Override
        public PageResult<Map<String, Object>> findHealthWarningsPage(
                int page,
                int pageSize
        ) {

            return new PageResult<>(
                    List.of(),
                    page,
                    pageSize,
                    0
            );
        }
    }

    // =====================================================
    // STUB ROOM DAO
    // =====================================================

    private static class StubRoomDao extends RoomDao {

        private final Map<Integer, Room> data = new HashMap<>();

        void save(Room room) {
            data.put(room.getRoomId(), room);
        }

        @Override
        public Optional<Room> findById(Integer id) {
            return Optional.ofNullable(data.get(id));
        }
    }

    // =====================================================
    // STUB SESSION DAO
    // =====================================================

    private static class StubSessionDao extends SessionDao {

        private final Map<Integer, com.gym.core.model.Session> data =
                new HashMap<>();

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

        @Override
        public Optional<com.gym.core.model.Session> findById(Integer id) {
            return Optional.ofNullable(data.get(id));
        }

        void save(com.gym.core.model.Session session) {
            data.put(session.getSessionId(), session);
        }
    }

    // =====================================================
    // STUB SESSION EQUIPMENT USAGE DAO
    // =====================================================

    private static class StubUsageDao extends SessionEquipmentUsageDao {

        private final List<SessionEquipmentUsage> data =
                new ArrayList<>();

        @Override
        public List<SessionEquipmentUsage> findByEquipment(int equipmentId) {

            return data.stream()
                    .filter(u -> Objects.equals(
                            u.getEquipmentId(),
                            equipmentId
                    ))
                    .toList();
        }

        void save(SessionEquipmentUsage usage) {
            data.add(usage);
        }
    }
}
