package com.gym.scheduling.service;

import com.gym.core.dao.ClassSessionDao;
import com.gym.core.model.ClassSession;
import com.gym.core.model.Manager;
import com.gym.core.model.Session;
import com.gym.core.model.Trainer;
import com.gym.scheduling.dao.SessionDao;
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
    void availabilitySupportsTrainerAndReturnsFreeStarts() {
        Trainer trainer = new Trainer();
        trainer.setRole(com.gym.core.model.StaffRole.TRAINER);
        com.gym.app.Session.login(trainer);

        SessionDao sessionDao = new SessionDao() {
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

        assertEquals(31, starts.size());
        assertEquals(date.atTime(6, 0), starts.get(0));
        assertEquals(date.atTime(21, 0), starts.get(starts.size() - 1));
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
