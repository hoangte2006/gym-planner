package com.gym.scheduling.service;

import com.gym.core.dao.ClassSessionDao;
import com.gym.core.model.ClassSession;
import com.gym.core.model.Manager;
import com.gym.core.model.Session;
import com.gym.core.model.Trainer;
import com.gym.scheduling.dao.SchedulingSessionDao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchedulingServicesTest {

    @BeforeEach
    void logOutBeforeTest() {
        com.gym.app.Session.logout();
    }

    @AfterEach
    void logOutAfterTest() {
        com.gym.app.Session.logout();
    }

    @Test
    void classSchedulingRequiresManagerOrReceptionist() {
        ClassSchedulingService service = new ClassSchedulingService();
        Trainer trainer = new Trainer();
        trainer.setRole(com.gym.core.model.StaffRole.TRAINER);
        com.gym.app.Session.login(trainer);

        assertThrows(SecurityException.class, () -> service.createClass(
                "", 1, 1, 1, LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(1)));

        Manager manager = new Manager();
        manager.setRole(com.gym.core.model.StaffRole.MANAGER);
        com.gym.app.Session.login(manager);
        assertThrows(IllegalArgumentException.class, () -> service.createClass(
                "", 1, 1, 1, LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(1)));
    }

    @Test
    void ptBookingRequiresManagerOrReceptionist() {
        PtSchedulingService service = new PtSchedulingService();
        Trainer trainer = new Trainer();
        trainer.setRole(com.gym.core.model.StaffRole.TRAINER);
        com.gym.app.Session.login(trainer);

        assertThrows(SecurityException.class,
                () -> service.bookSession(null, null, null, null, null));

        Manager manager = new Manager();
        manager.setRole(com.gym.core.model.StaffRole.MANAGER);
        com.gym.app.Session.login(manager);
        assertThrows(IllegalArgumentException.class,
                () -> service.bookSession(null, null, null, null, null));
    }

    @Test
    void classCreationRejectsSessionsOutsideOpeningHours() {
        Manager manager = new Manager();
        manager.setRole(com.gym.core.model.StaffRole.MANAGER);
        com.gym.app.Session.login(manager);
        LocalDate date = LocalDate.now().plusDays(3);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> new ClassSchedulingService().createClass(
                        "Morning class", 1, 1, 1,
                        date.atTime(5, 0), date.atTime(6, 0)));

        assertTrue(error.getMessage().contains("06:00"));
    }

    @Test
    void ptBookingRejectsSessionEndingAfterClosingTime() {
        Manager manager = new Manager();
        manager.setRole(com.gym.core.model.StaffRole.MANAGER);
        com.gym.app.Session.login(manager);
        LocalDate date = LocalDate.now().plusDays(3);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> new PtSchedulingService().bookSession(
                        1, 1, 1, date.atTime(21, 45), date.atTime(22, 15)));

        assertTrue(error.getMessage().contains("22:00"));
    }

    @Test
    void availabilitySupportsTrainerAndReturnsFreeStarts() {
        Trainer trainer = new Trainer();
        trainer.setRole(com.gym.core.model.StaffRole.TRAINER);
        com.gym.app.Session.login(trainer);

        SchedulingSessionDao sessionDao = new SchedulingSessionDao() {
            @Override
            public List<Session> findStartingBetween(
                    LocalDateTime from, LocalDateTime to) {
                return List.of();
            }
        };
        AvailabilityService service =
                new AvailabilityService(new SessionService(sessionDao));

        LocalDate date = LocalDate.now().plusDays(3);
        List<LocalDateTime> starts = service.findAvailableStarts(
                1, 1, date, Duration.ofHours(1));

        assertEquals(61, starts.size());
        assertEquals(date.atTime(6, 0), starts.get(0));
        assertEquals(date.atTime(21, 0), starts.get(starts.size() - 1));
    }

    @Test
    void availabilityRoundsBusyEndToNextQuarterHour() {
        Trainer trainer = new Trainer();
        trainer.setRole(com.gym.core.model.StaffRole.TRAINER);
        com.gym.app.Session.login(trainer);

        LocalDate date = LocalDate.now().plusDays(3);
        ClassSession busy = new ClassSession();
        busy.setRoomId(1);
        busy.setTrainerId(2);
        busy.setStartTime(date.atTime(6, 0));
        busy.setEndTime(date.atTime(6, 10));
        SchedulingSessionDao sessionDao = new SchedulingSessionDao() {
            @Override
            public List<Session> findStartingBetween(
                    LocalDateTime from, LocalDateTime to) {
                return List.of(busy);
            }
        };

        List<LocalDateTime> starts = new AvailabilityService(
                new SessionService(sessionDao)).findAvailableStarts(
                1, 2, date, Duration.ofMinutes(30));

        assertEquals(date.atTime(6, 15), starts.get(0));
        assertTrue(starts.stream().allMatch(start -> start.getMinute() % 15 == 0));
    }

    @Test
    void availabilityRejectsDurationOutsideAllowedRangeOrStep() {
        Manager manager = new Manager();
        manager.setRole(com.gym.core.model.StaffRole.MANAGER);
        com.gym.app.Session.login(manager);
        AvailabilityService service =
                new AvailabilityService(new SessionService(new SchedulingSessionDao()));
        LocalDate date = LocalDate.now().plusDays(3);

        assertThrows(IllegalArgumentException.class, () -> service.findAvailableStarts(
                1, 1, date, Duration.ofMinutes(15)));
        assertThrows(IllegalArgumentException.class, () -> service.findAvailableStarts(
                1, 1, date, Duration.ofMinutes(195)));
        assertThrows(IllegalArgumentException.class, () -> service.findAvailableStarts(
                1, 1, date, Duration.ofMinutes(35)));
    }

    @Test
    void availabilityRejectsAnonymousUsers() {
        AvailabilityService service = new AvailabilityService();

        assertThrows(SecurityException.class, () -> service.findAvailableStarts(
                1, 1, LocalDate.now().plusDays(1), Duration.ofHours(1)));
    }

    @Test
    void classRegistrationAllowsTrainerToViewButNotRegister() {
        Trainer trainer = new Trainer();
        trainer.setRole(com.gym.core.model.StaffRole.TRAINER);
        com.gym.app.Session.login(trainer);

        ClassSessionDao classDao = new ClassSessionDao() {
            @Override
            public List<ClassSession> findAll() {
                return List.of();
            }

            @Override
            public Optional<ClassSession> findById(Integer id) {
                return Optional.empty();
            }
        };
        ClassRegistrationService service = new ClassRegistrationService(
                classDao,
                new com.gym.core.dao.ClassRegistrationDao(),
                new com.gym.core.dao.MemberDao(),
                new com.gym.core.dao.MembershipDao());

        assertTrue(service.findUpcomingClasses().isEmpty());
        assertThrows(SecurityException.class, () -> service.register(1, 1));
    }
}
