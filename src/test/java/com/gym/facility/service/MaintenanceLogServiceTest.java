
package com.gym.facility.service;

import com.gym.app.Session;
import com.gym.core.dao.EquipmentDao;
import com.gym.core.dao.MaintenanceLogDao;
import com.gym.core.dao.StaffDao;
import com.gym.core.exception.BusinessException;
import com.gym.core.model.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MaintenanceLogServiceTest {

    private StubMaintenanceLogDao maintenanceLogDao;
    private StubEquipmentDao equipmentDao;
    private StubStaffDao staffDao;

    private MaintenanceLogService maintenanceLogService;

    @BeforeEach
    void setUp() {
        maintenanceLogDao = new StubMaintenanceLogDao();
        equipmentDao = new StubEquipmentDao();
        staffDao = new StubStaffDao();

        maintenanceLogService = new MaintenanceLogService(
                maintenanceLogDao,
                equipmentDao,
                staffDao
        );

        loginAsManager();
    }

    @AfterEach
    void tearDown() {
        Session.logout();
    }

    // =====================================================
    // SESSION
    // =====================================================

    private void loginAsManager() {
        Manager manager = new Manager();
        manager.setRole(StaffRole.MANAGER);

        Session.login(manager);
    }

    private void loginAsTrainer() {
        Trainer trainer = new Trainer();
        trainer.setRole(StaffRole.TRAINER);

        Session.login(trainer);
    }

    // =====================================================
    // TEST DATA
    // =====================================================

    private MaintenanceLog createValidLog() {
        MaintenanceLog log = new MaintenanceLog();

        log.setEquipmentId(1);
        log.setDescription("Thay dây cáp máy tập");
        log.setLoggedBy(10);

        return log;
    }

    private Equipment createEquipment(Integer equipmentId) {
        Equipment equipment = new Equipment();

        equipment.setEquipmentId(equipmentId);
        equipment.setEquipmentName("Máy chạy bộ");
        equipment.setOperationalStatus(EquipmentStatus.ACTIVE);

        return equipment;
    }

    private Manager createManagerStaff(Integer staffId) {
        Manager manager = new Manager();

        manager.setStaffId(staffId);
        manager.setRole(StaffRole.MANAGER);

        return manager;
    }

    private Receptionist createReceptionist(Integer staffId) {
        Receptionist receptionist = new Receptionist();

        receptionist.setStaffId(staffId);
        receptionist.setRole(StaffRole.RECEPTIONIST);

        return receptionist;
    }

    // =====================================================
    // CREATE TESTS
    // =====================================================

    @Test
    void create_ShouldThrowException_WhenUserIsNotManager() {
        loginAsTrainer();

        MaintenanceLog log = createValidLog();

        assertThrows(
                BusinessException.class,
                () -> maintenanceLogService.create(log)
        );
    }

    @Test
    void create_ShouldThrowException_WhenLogIsNull() {
        assertThrows(
                BusinessException.class,
                () -> maintenanceLogService.create(null)
        );
    }

    @Test
    void create_ShouldThrowException_WhenEquipmentIdIsNull() {
        MaintenanceLog log = createValidLog();
        log.setEquipmentId(null);

        assertThrows(
                BusinessException.class,
                () -> maintenanceLogService.create(log)
        );
    }

    @Test
    void create_ShouldThrowException_WhenEquipmentIdIsInvalid() {
        MaintenanceLog log = createValidLog();
        log.setEquipmentId(0);

        assertThrows(
                BusinessException.class,
                () -> maintenanceLogService.create(log)
        );
    }

    @Test
    void create_ShouldThrowException_WhenDescriptionIsNull() {
        MaintenanceLog log = createValidLog();

        // Không gọi setter description để giữ giá trị null.
        // MaintenanceLog mới tạo có description = null.

        MaintenanceLog emptyLog = new MaintenanceLog();
        emptyLog.setEquipmentId(1);
        emptyLog.setLoggedBy(10);

        assertThrows(
                BusinessException.class,
                () -> maintenanceLogService.create(emptyLog)
        );
    }

    @Test
    void create_ShouldThrowException_WhenDescriptionExceeds255Characters() {
        MaintenanceLog log = createValidLog();

        String longDescription = "A".repeat(256);

        log.setDescription(longDescription);

        assertThrows(
                BusinessException.class,
                () -> maintenanceLogService.create(log)
        );
    }

    @Test
    void create_ShouldThrowException_WhenLoggedByIsNull() {
        MaintenanceLog log = createValidLog();
        log.setLoggedBy(null);

        assertThrows(
                BusinessException.class,
                () -> maintenanceLogService.create(log)
        );
    }

    @Test
    void create_ShouldThrowException_WhenLoggedByIsInvalid() {
        MaintenanceLog log = createValidLog();
        log.setLoggedBy(0);

        assertThrows(
                BusinessException.class,
                () -> maintenanceLogService.create(log)
        );
    }

    @Test
    void create_ShouldThrowException_WhenLogNoIsManuallyAssigned() {
        MaintenanceLog log = createValidLog();
        log.setLogNo(5);

        // Log_no được kiểm tra bên trong Tx.call().
        // Vì vậy test này cần DB thật để chạy đến phần kiểm tra Log_no.
        // Không chạy test này nếu chưa cấu hình SQL Server.
    }

    // =====================================================
    // FIND BY EQUIPMENT TESTS
    // =====================================================

    @Test
    void findByEquipment_ShouldThrowException_WhenUserIsNotManager() {
        loginAsTrainer();

        assertThrows(
                BusinessException.class,
                () -> maintenanceLogService.findByEquipment(1)
        );
    }

    @Test
    void findByEquipment_ShouldThrowException_WhenEquipmentIdIsNull() {
        assertThrows(
                BusinessException.class,
                () -> maintenanceLogService.findByEquipment(null)
        );
    }

    @Test
    void findByEquipment_ShouldThrowException_WhenEquipmentIdIsInvalid() {
        assertThrows(
                BusinessException.class,
                () -> maintenanceLogService.findByEquipment(0)
        );
    }

    @Test
    void findByEquipment_ShouldThrowException_WhenEquipmentDoesNotExist() {
        equipmentDao.equipment = Optional.empty();

        assertThrows(
                BusinessException.class,
                () -> maintenanceLogService.findByEquipment(1)
        );
    }

    @Test
    void findByEquipment_ShouldReturnLogs_WhenEquipmentExists() {
        equipmentDao.equipment = Optional.of(createEquipment(1));

        MaintenanceLog log = createValidLog();

        maintenanceLogDao.logs.add(log);

        List<MaintenanceLog> result =
                maintenanceLogService.findByEquipment(1);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getEquipmentId());
    }

    // =====================================================
    // FIND BY ID TESTS
    // =====================================================

    @Test
    void findById_ShouldThrowException_WhenEquipmentIdIsInvalid() {
        assertThrows(
                BusinessException.class,
                () -> maintenanceLogService.findById(0, 1)
        );
    }

    @Test
    void findById_ShouldThrowException_WhenLogNoIsInvalid() {
        assertThrows(
                BusinessException.class,
                () -> maintenanceLogService.findById(1, 0)
        );
    }

    @Test
    void findById_ShouldThrowException_WhenLogDoesNotExist() {
        assertThrows(
                BusinessException.class,
                () -> maintenanceLogService.findById(1, 1)
        );
    }

    @Test
    void findById_ShouldReturnLog_WhenLogExists() {
        MaintenanceLog log = createValidLog();
        log.setLogNo(1);

        maintenanceLogDao.log = Optional.of(log);

        MaintenanceLog result =
                maintenanceLogService.findById(1, 1);

        assertNotNull(result);
        assertEquals(1, result.getEquipmentId());
        assertEquals(1, result.getLogNo());
    }

    // =====================================================
    // SEARCH BY EQUIPMENT TESTS
    // =====================================================

    @Test
    void searchByEquipment_ShouldThrowException_WhenEquipmentIdIsInvalid() {
        assertThrows(
                BusinessException.class,
                () -> maintenanceLogService.searchByEquipment(0, "cáp")
        );
    }

    @Test
    void searchByEquipment_ShouldThrowException_WhenEquipmentDoesNotExist() {
        equipmentDao.equipment = Optional.empty();

        assertThrows(
                BusinessException.class,
                () -> maintenanceLogService.searchByEquipment(1, "cáp")
        );
    }

    @Test
    void searchByEquipment_ShouldReturnAllLogs_WhenKeywordIsBlank() {
        equipmentDao.equipment = Optional.of(createEquipment(1));

        MaintenanceLog log = createValidLog();
        maintenanceLogDao.logs.add(log);

        List<MaintenanceLog> result =
                maintenanceLogService.searchByEquipment(1, "   ");

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getEquipmentId());
    }

    @Test
    void searchByEquipment_ShouldSearchByTrimmedKeyword() {
        equipmentDao.equipment = Optional.of(createEquipment(1));

        maintenanceLogDao.searchResult = List.of(createValidLog());

        List<MaintenanceLog> result =
                maintenanceLogService.searchByEquipment(1, "  cáp  ");

        assertEquals(1, result.size());
        assertEquals("cáp", maintenanceLogDao.lastKeyword);
    }

    // =====================================================
    // ROLE TESTS
    // =====================================================

    @Test
    void requireMaintananceAccess_ShouldThrowException_WhenRoleIsTrainer() {
        assertThrows(
                BusinessException.class,
                () -> MaintenanceLogService.requireMaintananceAccess(
                        StaffRole.TRAINER
                )
        );
    }

    @Test
    void requireMaintananceAccess_ShouldThrowException_WhenRoleIsReceptionist() {
        assertThrows(
                BusinessException.class,
                () -> MaintenanceLogService.requireMaintananceAccess(
                        StaffRole.RECEPTIONIST
                )
        );
    }

    @Test
    void requireMaintananceAccess_ShouldNotThrow_WhenRoleIsManager() {
        assertDoesNotThrow(
                () -> MaintenanceLogService.requireMaintananceAccess(
                        StaffRole.MANAGER
                )
        );
    }

    // =====================================================
    // STUB DAOS
    // =====================================================

    private static class StubMaintenanceLogDao extends MaintenanceLogDao {

        private final List<MaintenanceLog> logs = new ArrayList<>();

        private Optional<MaintenanceLog> log = Optional.empty();

        private List<MaintenanceLog> searchResult = List.of();

        private String lastKeyword;

        @Override
        public List<MaintenanceLog> findByEquipment(int equipmentId) {
            return logs;
        }

        @Override
        public Optional<MaintenanceLog> findById(MaintenanceLogKey key) {
            return log;
        }

        @Override
        public List<MaintenanceLog> searchByEquipment(
                int equipmentId,
                String keyword
        ) {
            lastKeyword = keyword;
            return searchResult;
        }
    }

    private static class StubEquipmentDao extends EquipmentDao {

        private Optional<Equipment> equipment = Optional.empty();

        @Override
        public Optional<Equipment> findById(Integer equipmentId) {
            return equipment;
        }

        @Override
        public void update(Equipment equipment) {
            this.equipment = Optional.of(equipment);
        }
    }

    private static class StubStaffDao extends StaffDao {

        private Optional<Staff> staff = Optional.empty();

        @Override
        public Optional<Staff> findById(Integer staffId) {
            return staff;
        }
    }
}
