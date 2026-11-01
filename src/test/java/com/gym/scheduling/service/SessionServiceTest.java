package com.gym.scheduling.service;

import com.gym.core.model.ClassSession;
import com.gym.core.model.Manager;
import com.gym.core.model.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SessionServiceTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 10, 12, 9, 0);

    @AfterEach
    void logout() {
        com.gym.app.Session.logout();
    }

    @Test
    void findsRoomConflictAndReturnsBlockingSession() {
        Session existing = session(11, 1, 2, BASE, BASE.plusHours(1));
        Session candidate = session(null, 1, 3, BASE.plusMinutes(30), BASE.plusMinutes(90));

        var conflict = SessionService.findRoomConflict(candidate, List.of(existing));

        assertSame(existing, conflict.orElseThrow());
    }

    @Test
    void doesNotReportRoomConflictForDifferentRooms() {
        Session existing = session(11, 1, 2, BASE, BASE.plusHours(1));
        Session candidate = session(null, 2, 3, BASE.plusMinutes(30), BASE.plusMinutes(90));

        assertTrue(SessionService.findRoomConflict(candidate, List.of(existing)).isEmpty());
    }

    @Test
    void findsTrainerConflictAndReturnsBlockingSession() {
        Session existing = session(11, 1, 2, BASE, BASE.plusHours(1));
        Session candidate = session(null, 3, 2, BASE.plusMinutes(30), BASE.plusMinutes(90));

        assertSame(existing, SessionService.findTrainerConflict(candidate, List.of(existing)).orElseThrow());
    }

    @Test
    void doesNotReportTrainerConflictForDifferentTrainers() {
        Session existing = session(11, 1, 2, BASE, BASE.plusHours(1));
        Session candidate = session(null, 3, 4, BASE.plusMinutes(30), BASE.plusMinutes(90));

        assertTrue(SessionService.findTrainerConflict(candidate, List.of(existing)).isEmpty());
    }

    @Test
    void adjacentSessionsDoNotConflict() {
        Session existing = session(11, 1, 2, BASE, BASE.plusHours(1));
        Session candidate = session(null, 1, 2, BASE.plusHours(1), BASE.plusHours(2));

        assertTrue(SessionService.findRoomConflict(candidate, List.of(existing)).isEmpty());
        assertTrue(SessionService.findTrainerConflict(candidate, List.of(existing)).isEmpty());
    }

    @Test
    void ignoresSameSessionWhenCheckingUpdate() {
        Session existing = session(11, 1, 2, BASE, BASE.plusHours(1));
        Session candidate = session(11, 1, 2, BASE, BASE.plusHours(1));

        assertTrue(SessionService.findRoomConflict(candidate, List.of(existing)).isEmpty());
        assertTrue(SessionService.findTrainerConflict(candidate, List.of(existing)).isEmpty());
    }

    @Test
    void reportsConflictWhenCandidateContainsExistingSession() {
        Session existing = session(11, 1, 2, BASE.plusMinutes(15), BASE.plusMinutes(30));
        Session candidate = session(null, 1, 2, BASE, BASE.plusHours(1));

        assertSame(existing, SessionService.findRoomConflict(candidate, List.of(existing)).orElseThrow());
        assertSame(existing, SessionService.findTrainerConflict(candidate, List.of(existing)).orElseThrow());
    }

    @Test
    void rejectsCandidateWithoutTimeRange() {
        Session candidate = session(null, 1, 2, null, BASE);
        assertThrows(IllegalArgumentException.class,
                () -> SessionService.findRoomConflict(candidate, List.of()));
    }

    @Test
    void reschedulingRejectsTimesOutsideQuarterHourGridBeforeDatabaseAccess() {
        Manager manager = new Manager();
        manager.setRole(com.gym.core.model.StaffRole.MANAGER);
        com.gym.app.Session.login(manager);
        ClassSession candidate = (ClassSession) session(
                11, 1, 2, BASE.withMinute(5), BASE.withMinute(5).plusHours(1));
        SessionService service = new SessionService();

        assertThrows(IllegalArgumentException.class,
                () -> service.updateSession(candidate));
    }
    private static Session session(Integer id, Integer roomId, Integer trainerId,
                                   LocalDateTime start, LocalDateTime end) {
        ClassSession session = new ClassSession();
        session.setSessionId(id);
        session.setRoomId(roomId);
        session.setTrainerId(trainerId);
        session.setStartTime(start);
        session.setEndTime(end);
        return session;
    }
}
