package com.gym.facility.service;

import com.gym.core.dao.EquipmentDao;
import com.gym.core.dao.RoomDao;
import com.gym.core.exception.BusinessException;
import com.gym.core.model.Equipment;
import com.gym.core.model.EquipmentStatus;
import com.gym.core.model.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EquipmentService.
 *
 * The DAO classes are replaced with in-memory stubs so these tests do not
 * require a running SQL Server database.
 */
class EquipmentServiceTest {

    private StubEquipmentDao equipmentDao;
    private StubRoomDao roomDao;
    private EquipmentService equipmentService;

    private static final EquipmentStatus VALID_STATUS = EquipmentStatus.values()[0];

    @BeforeEach
    void setUp() {
        equipmentDao = new StubEquipmentDao();
        roomDao = new StubRoomDao();
        roomDao.addRoom(1);
        roomDao.addRoom(2);

        equipmentService = new EquipmentService(equipmentDao, roomDao);
    }

    @Test
    void create_validEquipment_savesAndReturnsEquipment() {
        Equipment equipment = validEquipment();

        Equipment created = equipmentService.create(equipment);

        assertNotNull(created);
        assertNotNull(created.getEquipmentId());
        assertTrue(created.getEquipmentId() > 0);
        assertEquals("Máy chạy bộ", created.getEquipmentName());
        assertEquals(1, created.getRoomId());
        assertTrue(equipmentDao.findById(created.getEquipmentId()).isPresent());
    }

    @Test
    void create_nullEquipment_throwsBusinessException() {
        assertThrows(BusinessException.class,
                () -> equipmentService.create(null));
    }

    @Test
    void create_equipmentWithExistingId_throwsBusinessException() {
        Equipment equipment = validEquipment();
        equipment.setEquipmentId(99);

        assertThrows(BusinessException.class,
                () -> equipmentService.create(equipment));
    }

    @Test
    void create_roomDoesNotExist_throwsBusinessException() {
        Equipment equipment = validEquipment();
        equipment.setRoomId(999);

        assertThrows(BusinessException.class,
                () -> equipmentService.create(equipment));
    }

    @Test
    void create_purchaseDateInFuture_throwsBusinessException() {
        Equipment equipment = validEquipment();
        equipment.setPurchaseDate(LocalDate.now().plusDays(1));

        assertThrows(BusinessException.class,
                () -> equipmentService.create(equipment));
    }

    @Test
    void update_existingEquipment_updatesEquipment() {
        Equipment equipment = equipmentDao.insert(validEquipment());
        equipment.setEquipmentName("Máy chạy bộ đã cập nhật");

        equipmentService.update(equipment);

        Equipment saved = equipmentDao.findById(equipment.getEquipmentId()).orElseThrow();
        assertEquals("Máy chạy bộ đã cập nhật", saved.getEquipmentName());
    }

    @Test
    void update_equipmentWithoutId_throwsBusinessException() {
        Equipment equipment = validEquipment();

        assertThrows(BusinessException.class,
                () -> equipmentService.update(equipment));
    }

    @Test
    void update_equipmentDoesNotExist_throwsBusinessException() {
        Equipment equipment = validEquipment();
        equipment.setEquipmentId(999);

        assertThrows(BusinessException.class,
                () -> equipmentService.update(equipment));
    }

    @Test
    void update_roomDoesNotExist_throwsBusinessException() {
        Equipment equipment = equipmentDao.insert(validEquipment());
        equipment.setRoomId(999);

        assertThrows(BusinessException.class,
                () -> equipmentService.update(equipment));
    }

    @Test
    void findById_existingEquipment_returnsEquipment() {
        Equipment stored = equipmentDao.insert(validEquipment());

        Equipment found = equipmentService.findById(stored.getEquipmentId());

        assertEquals(stored.getEquipmentId(), found.getEquipmentId());
        assertEquals(stored.getEquipmentName(), found.getEquipmentName());
    }

    @Test
    void findById_nonExistingEquipment_throwsBusinessException() {
        assertThrows(BusinessException.class,
                () -> equipmentService.findById(999));
    }

    @Test
    void findAll_returnsAllEquipment() {
        equipmentDao.insert(validEquipment());
        Equipment second = validEquipment();
        second.setEquipmentName("Xe đạp tập");
        equipmentDao.insert(second);

        List<Equipment> result = equipmentService.findAll();

        assertEquals(2, result.size());
    }

    @Test
    void findByStatus_returnsEquipmentWithRequestedStatus() {
        Equipment equipment = validEquipment();
        equipment.setOperationalStatus(VALID_STATUS);
        equipmentDao.insert(equipment);

        List<Equipment> result = equipmentService.findByStatus(VALID_STATUS);

        assertEquals(1, result.size());
        assertEquals(VALID_STATUS, result.get(0).getOperationalStatus());
    }

    @Test
    void findByStatus_nullStatus_throwsBusinessException() {
        assertThrows(BusinessException.class,
                () -> equipmentService.findByStatus(null));
    }

    @Test
    void findByRoom_existingRoom_returnsEquipmentForRoom() {
        Equipment equipment = validEquipment();
        equipment.setRoomId(2);
        equipmentDao.insert(equipment);

        List<Equipment> result = equipmentService.findByRoom(2);

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getRoomId());
    }

    @Test
    void findByRoom_invalidRoomId_throwsBusinessException() {
        assertThrows(BusinessException.class,
                () -> equipmentService.findByRoom(0));
    }

    @Test
    void findByRoom_roomDoesNotExist_throwsBusinessException() {
        assertThrows(BusinessException.class,
                () -> equipmentService.findByRoom(999));
    }

    @Test
    void delete_existingEquipment_removesEquipment() {
        Equipment equipment = equipmentDao.insert(validEquipment());
        Integer id = equipment.getEquipmentId();

        equipmentService.delete(id);

        assertTrue(equipmentDao.findById(id).isEmpty());
    }

    @Test
    void delete_nonExistingEquipment_throwsBusinessException() {
        assertThrows(BusinessException.class,
                () -> equipmentService.delete(999));
    }

    @Test
    void delete_invalidId_throwsBusinessException() {
        assertThrows(BusinessException.class,
                () -> equipmentService.delete(0));
    }

    private Equipment validEquipment() {
        Equipment equipment = new Equipment();
        equipment.setEquipmentName("Máy chạy bộ");
        equipment.setRoomId(1);
        equipment.setOperationalStatus(VALID_STATUS);
        equipment.setPurchaseDate(LocalDate.now().minusDays(10));
        return equipment;
    }

    /** In-memory replacement for EquipmentDao. */
    private static class StubEquipmentDao extends EquipmentDao {
        private final Map<Integer, Equipment> rows = new LinkedHashMap<>();
        private int nextId = 1;

        @Override
        public Equipment insert(Equipment equipment) {
            if (equipment.getEquipmentId() == null) {
                equipment.setEquipmentId(nextId++);
            }
            rows.put(equipment.getEquipmentId(), equipment);
            return equipment;
        }

        @Override
        public void update(Equipment equipment) {
            rows.put(equipment.getEquipmentId(), equipment);
        }

        @Override
        public Optional<Equipment> findById(Integer id) {
            return Optional.ofNullable(rows.get(id));
        }

        @Override
        public List<Equipment> findAll() {
            return new ArrayList<>(rows.values());
        }

        @Override
        public List<Equipment> findByStatus(EquipmentStatus status) {
            return rows.values().stream()
                    .filter(item -> item.getOperationalStatus() == status)
                    .collect(Collectors.toList());
        }

        @Override
        public List<Equipment> findByRoom(int roomId) {
            return rows.values().stream()
                    .filter(item -> item.getRoomId() != null
                            && item.getRoomId() == roomId)
                    .collect(Collectors.toList());
        }

        @Override
        public void deleteById(Integer id) {
            rows.remove(id);
        }
    }

    /** In-memory replacement for RoomDao. */
    private static class StubRoomDao extends RoomDao {
        private final Set<Integer> roomIds = new HashSet<>();

        void addRoom(int roomId) {
            roomIds.add(roomId);
        }

        @Override
        public Optional<Room> findById(Integer id) {
            if (id == null || !roomIds.contains(id)) {
                return Optional.empty();
            }

            Room room = new Room();
            room.setRoomId(id);
            return Optional.of(room);
        }
    }
}
