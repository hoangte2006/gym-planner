
package com.gym.facility.service;

import com.gym.app.Session;
import com.gym.core.dao.PageResult;
import com.gym.core.dao.SessionDao;
import com.gym.core.dao.StaffDao;
import com.gym.core.exception.BusinessException;
import com.gym.core.model.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StaffServiceTest {

    private FakeStaffDao staffDao;
    private FakeSessionDao sessionDao;
    private StaffService staffService;

    @BeforeEach
    void setUp() {
        staffDao = new FakeStaffDao();
        sessionDao = new FakeSessionDao();

        staffService = new StaffService(staffDao, sessionDao);

        // Đăng nhập bằng Manager để được phép thao tác.
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

    private Trainer createValidTrainer() {
        Trainer trainer = new Trainer();

        trainer.setFullName("Nguyen Van A");
        trainer.setPhone("0912345678");
        trainer.setSalary(new BigDecimal("10000000"));
        trainer.setHireDate(LocalDate.now());
        trainer.setSpecialty("Fitness");
        trainer.setCertification("ACE");

        trainer.setRole(StaffRole.TRAINER);

        return trainer;
    }

    private Manager createValidManager() {
        Manager manager = new Manager();

        manager.setFullName("Nguyen Van B");
        manager.setPhone("0987654321");
        manager.setSalary(new BigDecimal("15000000"));
        manager.setHireDate(LocalDate.now());
        manager.setRole(StaffRole.MANAGER);

        return manager;
    }

    // =====================================================
    // CREATE STAFF
    // =====================================================

    @Test
    void createStaff_shouldCreateTrainerSuccessfully() {
        Trainer trainer = createValidTrainer();

        Staff result = staffService.createStaff(trainer);

        assertNotNull(result);
        assertEquals(1, result.getStaffId());
        assertEquals("Nguyen Van A", result.getFullName());
        assertEquals("0912345678", result.getPhone());

        assertEquals(1, staffDao.insertCount);
    }

    @Test
    void createStaff_shouldNormalizePhone() {
        Trainer trainer = createValidTrainer();

        trainer.setPhone("0912-345-678");

        Staff result = staffService.createStaff(trainer);

        assertEquals("0912345678", result.getPhone());
    }

    @Test
    void createStaff_shouldThrowWhenStaffIsNull() {
        assertThrows(
                BusinessException.class,
                () -> staffService.createStaff(null)
        );

        assertEquals(0, staffDao.insertCount);
    }

    @Test
    void createStaff_shouldThrowWhenStaffIdAlreadyExists() {
        Trainer trainer = createValidTrainer();
        trainer.setStaffId(10);

        assertThrows(
                BusinessException.class,
                () -> staffService.createStaff(trainer)
        );

        assertEquals(0, staffDao.insertCount);
    }

    @Test
    void createStaff_shouldThrowWhenPhoneIsInvalid() {
        Trainer trainer = createValidTrainer();
        trainer.setPhone("abc123");

        assertThrows(
                BusinessException.class,
                () -> staffService.createStaff(trainer)
        );

        assertEquals(0, staffDao.insertCount);
    }

    @Test
    void createStaff_shouldThrowWhenPhoneIsDuplicated() {
        Trainer first = createValidTrainer();
        staffService.createStaff(first);

        Trainer second = createValidTrainer();
        second.setPhone("0912-345-678");

        assertThrows(
                BusinessException.class,
                () -> staffService.createStaff(second)
        );

        assertEquals(1, staffDao.insertCount);
    }

    @Test
    void createStaff_shouldThrowWhenHireDateIsInFuture() {
        Trainer trainer = createValidTrainer();
        trainer.setHireDate(LocalDate.now().plusDays(1));

        assertThrows(
                BusinessException.class,
                () -> staffService.createStaff(trainer)
        );

        assertEquals(0, staffDao.insertCount);
    }

    @Test
    void createStaff_shouldThrowWhenRoleDoesNotMatchStaffType() {
        Trainer trainer = createValidTrainer();
        trainer.setRole(StaffRole.MANAGER);

        assertThrows(
                BusinessException.class,
                () -> staffService.createStaff(trainer)
        );

        assertEquals(0, staffDao.insertCount);
    }

    @Test
    void createStaff_shouldThrowWhenSpecialtyIsTooLong() {
        Trainer trainer = createValidTrainer();
        trainer.setSpecialty("A".repeat(51));

        assertThrows(
                BusinessException.class,
                () -> staffService.createStaff(trainer)
        );

        assertEquals(0, staffDao.insertCount);
    }

    // =====================================================
    // UPDATE STAFF
    // =====================================================

    @Test
    void updateStaff_shouldUpdateSuccessfully() {
        Trainer trainer = createValidTrainer();
        Staff saved = staffService.createStaff(trainer);

        Trainer updated = createValidTrainer();
        updated.setStaffId(saved.getStaffId());
        updated.setFullName("Nguyen Van C");
        updated.setPhone("0909000000");

        Staff result = staffService.updateStaff(updated);

        assertEquals("Nguyen Van C", result.getFullName());
        assertEquals("0909000000", result.getPhone());

        assertEquals(1, staffDao.updateCount);
    }

    @Test
    void updateStaff_shouldThrowWhenIdIsInvalid() {
        Trainer trainer = createValidTrainer();
        trainer.setStaffId(null);

        assertThrows(
                BusinessException.class,
                () -> staffService.updateStaff(trainer)
        );

        assertEquals(0, staffDao.updateCount);
    }

    @Test
    void updateStaff_shouldThrowWhenStaffDoesNotExist() {
        Trainer trainer = createValidTrainer();
        trainer.setStaffId(999);

        assertThrows(
                BusinessException.class,
                () -> staffService.updateStaff(trainer)
        );

        assertEquals(0, staffDao.updateCount);
    }

    @Test
    void updateStaff_shouldThrowWhenChangingStaffType() {
        Manager manager = createValidManager();
        manager = (Manager) staffService.createStaff(manager);

        Trainer trainer = createValidTrainer();
        trainer.setStaffId(manager.getStaffId());

        assertThrows(
                BusinessException.class,
                () -> staffService.updateStaff(trainer)
        );

        assertEquals(0, staffDao.updateCount);
    }

    @Test
    void updateStaff_shouldAllowKeepingOwnPhone() {
        Trainer trainer = createValidTrainer();
        Staff saved = staffService.createStaff(trainer);

        Trainer updated = createValidTrainer();
        updated.setStaffId(saved.getStaffId());
        updated.setFullName("Updated Name");

        assertDoesNotThrow(
                () -> staffService.updateStaff(updated)
        );

        assertEquals(1, staffDao.updateCount);
    }

    @Test
    void updateStaff_shouldThrowWhenPhoneBelongsToAnotherStaff() {
        Trainer first = createValidTrainer();
        staffService.createStaff(first);

        Trainer second = createValidTrainer();
        second.setPhone("0900000000");
        Staff savedSecond = staffService.createStaff(second);

        Trainer updated = createValidTrainer();
        updated.setStaffId(savedSecond.getStaffId());
        updated.setPhone("0912345678");

        assertThrows(
                BusinessException.class,
                () -> staffService.updateStaff(updated)
        );

        assertEquals(0, staffDao.updateCount);
    }

    // =====================================================
    // DELETE STAFF
    // =====================================================


    @Test
    void deleteStaff_shouldThrowWhenStaffDoesNotExist() {
        assertThrows(
                BusinessException.class,
                () -> staffService.deleteStaff(999)
        );

        assertEquals(0, staffDao.deleteCount);
    }

    @Test
    void deleteStaff_shouldThrowWhenStaffIsReferencedBySession() {
        Trainer trainer = createValidTrainer();
        Staff saved = staffService.createStaff(trainer);

        sessionDao.trainerSessions.add(new ClassSession());

        // Giả lập buổi tập thuộc nhân viên đang xóa.
        sessionDao.trainerId = saved.getStaffId();

        assertThrows(
                BusinessException.class,
                () -> staffService.deleteStaff(saved.getStaffId())
        );

        assertEquals(0, staffDao.deleteCount);
    }

    @Test
    void deleteStaff_shouldThrowWhenIdIsInvalid() {
        assertThrows(
                BusinessException.class,
                () -> staffService.deleteStaff(0)
        );

        assertEquals(0, staffDao.deleteCount);
    }

    // =====================================================
    // FIND STAFF
    // =====================================================

    @Test
    void findStaffById_shouldReturnStaff() {
        Trainer trainer = createValidTrainer();
        Staff saved = staffService.createStaff(trainer);

        Staff result = staffService.findStaffById(saved.getStaffId());

        assertNotNull(result);
        assertEquals(saved.getStaffId(), result.getStaffId());
    }

    @Test
    void findStaffById_shouldThrowWhenStaffDoesNotExist() {
        assertThrows(
                BusinessException.class,
                () -> staffService.findStaffById(999)
        );
    }

    @Test
    void findAll_shouldReturnAllStaff() {
        staffService.createStaff(createValidTrainer());
        staffService.createStaff(createValidManager());

        List<Staff> result = staffService.findAll();

        assertEquals(2, result.size());
    }

    @Test
    void findStaffByRole_shouldReturnTrainers() {
        staffService.createStaff(createValidTrainer());
        staffService.createStaff(createValidManager());

        List<Staff> result =
                staffService.findStaffByRole(StaffRole.TRAINER);

        assertEquals(1, result.size());
        assertEquals(StaffRole.TRAINER, result.get(0).getRole());
    }

    @Test
    void findStaffByRole_shouldThrowWhenRoleIsNull() {
        assertThrows(
                BusinessException.class,
                () -> staffService.findStaffByRole(null)
        );
    }

    // =====================================================
    // SEARCH STAFF
    // =====================================================

    @Test
    void searchStaff_shouldReturnAllWhenKeywordIsBlank() {
        staffService.createStaff(createValidTrainer());
        staffService.createStaff(createValidManager());

        List<Staff> result = staffService.searchStaff(" ");

        assertEquals(2, result.size());
    }

    @Test
    void searchStaff_shouldTrimKeyword() {
        staffService.createStaff(createValidTrainer());

        List<Staff> result = staffService.searchStaff("  Nguyen  ");

        assertEquals("Nguyen", staffDao.lastSearchKeyword);
    }

    // =====================================================
    // AUTHORIZATION
    // =====================================================

    @Test
    void createStaff_shouldThrowWhenUserIsNotManager() {
        Trainer loggedInTrainer = createValidTrainer();
        Session.login(loggedInTrainer);

        assertThrows(
                BusinessException.class,
                () -> staffService.createStaff(createValidTrainer())
        );

        assertEquals(0, staffDao.insertCount);
    }

    @Test
    void findAll_shouldThrowWhenUserIsNotManager() {
        Trainer loggedInTrainer = createValidTrainer();
        Session.login(loggedInTrainer);

        assertThrows(
                BusinessException.class,
                () -> staffService.findAll()
        );
    }

    // =====================================================
    // FAKE STAFF DAO
    // =====================================================

    private static class FakeStaffDao extends StaffDao {

        private final Map<Integer, Staff> database = new LinkedHashMap<>();

        private int nextId = 1;

        private int insertCount;
        private int updateCount;
        private int deleteCount;

        private String lastSearchKeyword;

        @Override
        public Optional<Staff> findById(Integer id) {
            return Optional.ofNullable(database.get(id));
        }

        @Override
        public Staff insert(Staff staff) {
            insertCount++;

            staff.setStaffId(nextId++);

            database.put(staff.getStaffId(), staff);

            return staff;
        }

        @Override
        public void update(Staff staff) {
            updateCount++;

            database.put(staff.getStaffId(), staff);
        }

        @Override
        public void deleteById(Integer id) {
            deleteCount++;

            database.remove(id);
        }

        @Override
        public Optional<Staff> findDuplicateByNormalizedPhone(
                String normalizedDigits,
                Integer excludedStaffId
        ) {
            return database.values()
                    .stream()
                    .filter(staff ->
                            Objects.equals(
                                    staff.getPhone(),
                                    normalizedDigits
                            )
                    )
                    .filter(staff ->
                            !Objects.equals(
                                    staff.getStaffId(),
                                    excludedStaffId
                            )
                    )
                    .findFirst();
        }

        @Override
        public List<Staff> findAll() {
            return new ArrayList<>(database.values());
        }

        @Override
        public List<Staff> search(String keyword) {
            lastSearchKeyword = keyword;

            return database.values()
                    .stream()
                    .filter(staff ->
                            staff.getFullName()
                                    .toLowerCase()
                                    .contains(keyword.toLowerCase())
                    )
                    .toList();
        }

        @Override
        public List<Staff> findByRole(StaffRole role) {
            return database.values()
                    .stream()
                    .filter(staff -> staff.getRole() == role)
                    .toList();
        }
    }

    // =====================================================
    // FAKE SESSION DAO
    // =====================================================

    private static class FakeSessionDao extends SessionDao {

        private final List<com.gym.core.model.Session> trainerSessions =
                new ArrayList<>();

        private Integer trainerId;

        public List<com.gym.core.model.Session> findByTrainer(Integer id) {
            if (Objects.equals(id, trainerId)) {
                return trainerSessions;
            }

            return Collections.emptyList();
        }

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
        public com.gym.core.model.Session insert(
                com.gym.core.model.Session session
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void update(com.gym.core.model.Session session) {
            throw new UnsupportedOperationException();
        }
    }
}
