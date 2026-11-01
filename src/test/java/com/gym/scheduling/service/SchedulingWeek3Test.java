package com.gym.scheduling.service;

import com.gym.core.dao.ClassRegistrationDao;
import com.gym.core.dao.ClassSessionDao;
import com.gym.core.dao.MemberDao;
import com.gym.core.dao.MembershipDao;
import com.gym.core.dao.SessionDao;
import com.gym.core.dao.RoomDao;
import com.gym.core.dao.TrainerDao;
import com.gym.core.model.AttendanceStatus;
import com.gym.core.model.BookingStatus;
import com.gym.core.model.ClassRegistration;
import com.gym.core.model.ClassRegistrationKey;
import com.gym.core.model.ClassSession;
import com.gym.core.model.Manager;
import com.gym.core.model.Member;
import com.gym.core.model.Membership;
import com.gym.core.model.MembershipStatus;
import com.gym.core.model.PtSession;
import com.gym.core.model.Session;
import com.gym.core.model.SessionType;
import com.gym.scheduling.dao.SchedulingSessionDao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchedulingWeek3Test {

    @BeforeEach
    void loginAsManager() {
        Manager manager = new Manager();
        manager.setRole(com.gym.core.model.StaffRole.MANAGER);
        com.gym.app.Session.login(manager);
    }

    @AfterEach
    void logout() {
        com.gym.app.Session.logout();
    }

    @Test
    void reschedulingChecksConflictsAndDoesNotSaveWhenBlocked() {
        PtSession existing = ptSession(42, BookingStatus.SCHEDULED);
        Session blocker = classSession(77, existing.getStartTime(), existing.getEndTime());
        FakeSessionDao dao = new FakeSessionDao(existing);
        dao.roomConflicts = List.of(blocker);
        SessionService service = service(dao);

        assertThrows(IllegalStateException.class, () -> service.updateSession(
                ptSession(42, BookingStatus.SCHEDULED)));
        assertFalse(dao.updated);
    }

    @Test
    void restoresCancelledPtOnlyAfterRoomTrainerAndEquipmentChecks() {
        PtSession cancelled = ptSession(42, BookingStatus.CANCELLED);
        FakeSessionDao dao = new FakeSessionDao(cancelled);
        dao.equipmentConflict = true;
        SessionService service = service(dao);

        assertThrows(IllegalStateException.class,
                () -> service.changePtStatus(42, BookingStatus.SCHEDULED));
        assertFalse(dao.updated);
    }

    @Test
    void restoresCancelledPtWhenScheduleAndEquipmentRemainAvailable() {
        PtSession cancelled = ptSession(42, BookingStatus.CANCELLED);
        FakeSessionDao dao = new FakeSessionDao(cancelled);
        SessionService service = service(dao);

        service.changePtStatus(42, BookingStatus.SCHEDULED);

        assertTrue(dao.updated);
        assertEquals(BookingStatus.SCHEDULED, dao.saved.getBookingStatus());
    }

    @Test
    void classRegistrationDelegatesSeatClaimToAtomicDaoOperation() {
        ClassSession classSession = new ClassSession();
        classSession.setSessionId(8);
        classSession.setClassName("Yoga");
        classSession.setMaxCapacity(2);
        classSession.setStartTime(LocalDateTime.now().plusDays(1));
        classSession.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));

        Member member = new Member();
        member.setVisitorId(12);
        member.setFirstName("Test");
        member.setLastName("Member");
        member.setPhotoOnFile(true);

        Membership membership = new Membership();
        membership.setStatus(MembershipStatus.ACTIVE);
        membership.setStartDate(LocalDate.now().minusDays(1));
        membership.setEndDate(LocalDate.now().plusDays(1));

        ClassRegistration registration = new ClassRegistration();
        registration.setSessionId(8);
        registration.setVisitorId(12);
        registration.setAttendanceStatus(AttendanceStatus.CONFIRMED);
        AtomicBoolean seatClaimed = new AtomicBoolean();

        ClassRegistrationService service = new ClassRegistrationService(
                new ClassSessionDao() {
                    @Override
                    public Optional<ClassSession> findById(Integer id) {
                        return Optional.of(classSession);
                    }
                },
                new ClassRegistrationDao() {
                    @Override
                    public Optional<ClassRegistration> findById(ClassRegistrationKey key) {
                        return Optional.of(registration);
                    }
                },
                new MemberDao() {
                    @Override
                    public Optional<Member> findById(Integer id) {
                        return Optional.of(member);
                    }
                },
                new MembershipDao() {
                    @Override
                    public List<Membership> findByVisitor(int visitorId) {
                        return List.of(membership);
                    }
                },
                new SessionDao() {
                    @Override
                    public void registerClassSeat(int sessionId, int visitorId) {
                        seatClaimed.set(true);
                    }
                });

        assertEquals(registration, service.register(8, 12));
        assertTrue(seatClaimed.get());
    }

    private static SessionService service(SchedulingSessionDao dao) {
        return new SessionService(dao, new RoomDao(), new TrainerDao(),
                new ClassRegistrationDao());
    }

    private static PtSession ptSession(int id, BookingStatus status) {
        PtSession session = new PtSession();
        session.setSessionId(id);
        session.setSessionType(SessionType.PT);
        session.setMemberId(12);
        session.setRoomId(3);
        session.setTrainerId(5);
        session.setStartTime(LocalDateTime.now().plusDays(3).withHour(10)
                .withMinute(0).withSecond(0).withNano(0));
        session.setEndTime(session.getStartTime().plusHours(1));
        session.setBookingStatus(status);
        return session;
    }

    private static ClassSession classSession(
            int id, LocalDateTime start, LocalDateTime end) {
        ClassSession session = new ClassSession();
        session.setSessionId(id);
        session.setSessionType(SessionType.CLASS);
        session.setClassName("Blocker");
        session.setMaxCapacity(10);
        session.setRoomId(3);
        session.setTrainerId(6);
        session.setStartTime(start);
        session.setEndTime(end);
        return session;
    }

    private static final class FakeSessionDao extends SchedulingSessionDao {
        private final PtSession current;
        private List<Session> roomConflicts = List.of();
        private List<Session> trainerConflicts = List.of();
        private boolean equipmentConflict;
        private boolean updated;
        private PtSession saved;

        private FakeSessionDao(PtSession current) {
            this.current = current;
        }

        @Override
        public Optional<Session> findById(Integer id) {
            return Optional.of(current);
        }

        @Override
        public List<Session> findRoomConflicts(
                Integer roomId, LocalDateTime start, LocalDateTime end, Integer exceptId) {
            return roomConflicts;
        }

        @Override
        public List<Session> findTrainerConflicts(
                Integer trainerId, LocalDateTime start, LocalDateTime end, Integer exceptId) {
            return trainerConflicts;
        }

        @Override
        public boolean hasConflictingEquipmentReservations(int sessionId) {
            return equipmentConflict;
        }

        @Override
        public void update(Session session) {
            updated = true;
            saved = (PtSession) session;
        }
    }
}
