
package com.gym.facility.service;

import com.gym.app.Session;
import com.gym.core.dao.SessionDao;
import com.gym.core.dao.TrainerDao;
import com.gym.core.exception.BusinessException;
import com.gym.core.model.Manager;
import com.gym.core.model.StaffRole;
import com.gym.core.model.Trainer;
import com.gym.facility.dto.TrainerProfile;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TrainerProfileServiceTest {

    private FakeTrainerDao trainerDao;
    private FakeSessionDao sessionDao;
    private TrainerProfileService trainerProfileService;

    @BeforeEach
    void setUp() {
        trainerDao = new FakeTrainerDao();
        sessionDao = new FakeSessionDao();

        trainerProfileService = new TrainerProfileService(
                trainerDao,
                sessionDao
        );

        // Đăng nhập bằng Manager.
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

    private Trainer createTrainer(Integer id) {
        Trainer trainer = new Trainer();

        trainer.setStaffId(id);
        trainer.setFullName("Nguyen Van A");
        trainer.setPhone("0912345678");
        trainer.setSpecialty("Fitness");
        trainer.setRole(StaffRole.TRAINER);

        return trainer;
    }

    @Test
    void getTrainerProfile_shouldThrowWhenTrainerIdIsNull() {
        assertThrows(
                BusinessException.class,
                () -> trainerProfileService.getTrainerProfile(null)
        );

        assertEquals(0, trainerDao.findByIdCount);
    }

    @Test
    void getTrainerProfile_shouldThrowWhenTrainerIdIsZero() {
        assertThrows(
                BusinessException.class,
                () -> trainerProfileService.getTrainerProfile(0)
        );

        assertEquals(0, trainerDao.findByIdCount);
    }

    @Test
    void getTrainerProfile_shouldThrowWhenTrainerIdIsNegative() {
        assertThrows(
                BusinessException.class,
                () -> trainerProfileService.getTrainerProfile(-1)
        );

        assertEquals(0, trainerDao.findByIdCount);
    }

    @Test
    void getTrainerProfile_shouldThrowWhenTrainerDoesNotExist() {
        assertThrows(
                BusinessException.class,
                () -> trainerProfileService.getTrainerProfile(999)
        );

        // Không được đếm Session khi Trainer không tồn tại.
        assertEquals(0, sessionDao.countByTrainerCount);
        assertEquals(0, sessionDao.findUpcomingCount);
    }

    @Test
    void getUpcomingSessions_shouldThrowWhenTrainerDoesNotExist() {
        assertThrows(
                BusinessException.class,
                () -> trainerProfileService.getUpcomingSessions(999)
        );

        assertEquals(0, sessionDao.findUpcomingCount);
    }

    @Test
    void getUpcomingSessions_shouldThrowWhenTrainerIdIsInvalid() {
        assertThrows(
                BusinessException.class,
                () -> trainerProfileService.getUpcomingSessions(null)
        );

        assertEquals(0, trainerDao.findByIdCount);
        assertEquals(0, sessionDao.findUpcomingCount);
    }

    @Test
    void countSessions_shouldThrowWhenTrainerDoesNotExist() {
        assertThrows(
                BusinessException.class,
                () -> trainerProfileService.countSessions(999)
        );

        assertEquals(0, sessionDao.countByTrainerCount);
    }

    @Test
    void countSessions_shouldThrowWhenTrainerIdIsInvalid() {
        assertThrows(
                BusinessException.class,
                () -> trainerProfileService.countSessions(-1)
        );

        assertEquals(0, trainerDao.findByIdCount);
        assertEquals(0, sessionDao.countByTrainerCount);
    }

    // =====================================================
    // AUTHORIZATION
    // =====================================================

    @Test
    void getTrainerProfile_shouldThrowWhenUserIsNotManager() {
        // Đăng nhập bằng Trainer thay vì Manager.
        Trainer loggedInTrainer = createTrainer(10);
        Session.login(loggedInTrainer);

        assertThrows(
                BusinessException.class,
                () -> trainerProfileService.getTrainerProfile(1)
        );

        assertEquals(0, trainerDao.findByIdCount);
    }

    @Test
    void getUpcomingSessions_shouldThrowWhenUserIsNotManager() {
        Trainer loggedInTrainer = createTrainer(10);
        Session.login(loggedInTrainer);

        assertThrows(
                BusinessException.class,
                () -> trainerProfileService.getUpcomingSessions(1)
        );

        assertEquals(0, trainerDao.findByIdCount);
        assertEquals(0, sessionDao.findUpcomingCount);
    }

    @Test
    void countSessions_shouldThrowWhenUserIsNotManager() {
        Trainer loggedInTrainer = createTrainer(10);
        Session.login(loggedInTrainer);

        assertThrows(
                BusinessException.class,
                () -> trainerProfileService.countSessions(1)
        );

        assertEquals(0, trainerDao.findByIdCount);
        assertEquals(0, sessionDao.countByTrainerCount);
    }

    // =====================================================
    // FAKE TRAINER DAO
    // =====================================================

    private static class FakeTrainerDao extends TrainerDao {

        private final java.util.Map<Integer, Trainer> trainers =
                new java.util.HashMap<>();

        private int findByIdCount;

        @Override
        public Optional<Trainer> findById(Integer id) {
            findByIdCount++;

            return Optional.ofNullable(trainers.get(id));
        }
    }

    // =====================================================
    // FAKE SESSION DAO
    // =====================================================

    private static class FakeSessionDao extends SessionDao {

        private long totalSessions;

        private int countByTrainerCount;
        private int findUpcomingCount;

        private Integer lastTrainerId;

        private LocalDateTime lastFromTime;

        public long countByTrainer(Integer trainerId) {
            countByTrainerCount++;

            lastTrainerId = trainerId;

            return totalSessions;
        }

        public List<com.gym.core.model.Session> findUpcomingByTrainer(
                Integer trainerId,
                LocalDateTime fromTime
        ) {
            findUpcomingCount++;

            lastTrainerId = trainerId;
            lastFromTime = fromTime;

            return Collections.emptyList();
        }
    }
}
