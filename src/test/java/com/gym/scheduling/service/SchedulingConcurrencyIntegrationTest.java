package com.gym.scheduling.service;

import com.gym.core.DBConnection;
import com.gym.core.dao.ClassSessionDao;
import com.gym.core.dao.RoomDao;
import com.gym.core.dao.TrainerDao;
import com.gym.core.model.ClassSession;
import com.gym.core.model.Manager;
import com.gym.core.model.Session;
import com.gym.core.model.Trainer;
import com.gym.scheduling.dao.SchedulingSessionDao;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class SchedulingConcurrencyIntegrationTest {

    @BeforeAll
    static void requireDatabase() {
        try {
            DBConnection.test();
        } catch (RuntimeException e) {
            assumeTrue(false, "Không kết nối được DB, bỏ qua kiểm thử đặt lịch đồng thời.");
        }
    }

    @Test
    void simultaneousBookingsForSameRoomAndTrainerCannotBothSucceed() throws Exception {
        Integer roomId = new RoomDao().findAll().get(0).getRoomId();
        Trainer trainer = new TrainerDao().findAll().get(0);
        int trainerId = trainer.getStaffId();
        LocalDateTime start = LocalDate.now().plusYears(3)
                .withMonth(1).withDayOfMonth(1).atTime(6, 0)
                .plusMinutes(Math.floorMod(System.nanoTime(), 40) * 15);
        LocalDateTime end = start.plusHours(1);
        CyclicBarrier roomChecks = new CyclicBarrier(2);
        CyclicBarrier trainerChecks = new CyclicBarrier(2);
        SchedulingSessionDao sessionDao = new SchedulingSessionDao() {
            @Override
            public List<Session> findRoomConflicts(
                    Integer requestedRoom,
                    LocalDateTime requestedStart,
                    LocalDateTime requestedEnd,
                    Integer exceptSessionId) {
                List<Session> conflicts = super.findRoomConflicts(
                        requestedRoom, requestedStart, requestedEnd, exceptSessionId);
                awaitBothChecks(roomChecks);
                return conflicts;
            }

            @Override
            public List<Session> findTrainerConflicts(
                    Integer requestedTrainer,
                    LocalDateTime requestedStart,
                    LocalDateTime requestedEnd,
                    Integer exceptSessionId) {
                List<Session> conflicts = super.findTrainerConflicts(
                        requestedTrainer, requestedStart, requestedEnd, exceptSessionId);
                awaitBothChecks(trainerChecks);
                return conflicts;
            }
        };
        ClassSchedulingService service = new ClassSchedulingService(
                new ClassSessionDao(), new RoomDao(), new TrainerDao(),
                new SessionService(sessionDao));
        String className = "Concurrency-" + UUID.randomUUID().toString().substring(0, 16);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Future<BookingAttempt>> attempts = new ArrayList<>();
        List<BookingAttempt> outcomes = new ArrayList<>();
        List<Integer> createdSessionIds = new ArrayList<>();

        try {
            for (int i = 0; i < 2; i++) {
                attempts.add(executor.submit(bookClass(
                        service, className, roomId, trainerId, start, end)));
            }

            for (Future<BookingAttempt> future : attempts) {
                BookingAttempt attempt = future.get(30, TimeUnit.SECONDS);
                outcomes.add(attempt);
                if (attempt.sessionId() != null) {
                    createdSessionIds.add(attempt.sessionId());
                }
            }
        } finally {
            executor.shutdownNow();
            for (Integer sessionId : createdSessionIds) {
                new ClassSessionDao().deleteById(sessionId);
            }
        }

        long successCount = createdSessionIds.size();
        assertEquals(1, successCount,
                "Chỉ một trong hai lượt đặt cùng phòng/HLV/khung giờ được thành công. "
                        + "Kết quả: " + outcomes);
        assertEquals(1, outcomes.stream().filter(attempt -> attempt.error() != null).count(),
                "Lượt đặt bị từ chối phải có lỗi để truy nguyên nguyên nhân.");
        BookingAttempt rejected = outcomes.stream()
                .filter(attempt -> attempt.error() != null)
                .findFirst()
                .orElseThrow();
        String message = rejected.error().toLowerCase();
        assertTrue(message.contains("deadlocked")
                        || message.contains("room schedule conflict")
                        || message.contains("trainer schedule conflict"),
                "Lượt đặt bị từ chối phải do tranh chấp lịch. Lỗi: "
                                + rejected.error());
    }

    private static Callable<BookingAttempt> bookClass(
            ClassSchedulingService service,
            String className,
            Integer roomId,
            int trainerId,
            LocalDateTime start,
            LocalDateTime end) {
        return () -> {
            Manager manager = new Manager();
            manager.setRole(com.gym.core.model.StaffRole.MANAGER);
            com.gym.app.Session.login(manager);
            try {
                ClassSession created = service.createClass(
                        className, 1, roomId, trainerId, start, end);
                return new BookingAttempt(created.getSessionId(), null);
            } catch (RuntimeException e) {
                return new BookingAttempt(null, rootCauseMessage(e));
            } finally {
                com.gym.app.Session.logout();
            }
        };
    }

    private static void awaitBothChecks(CyclicBarrier barrier) {
        try {
            barrier.await(20, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Hai lượt đặt lịch không hoàn tất được bước kiểm tra đồng thời.", e);
        }
    }

    private static String rootCauseMessage(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.toString();
    }

    private record BookingAttempt(Integer sessionId, String error) {
    }
}
