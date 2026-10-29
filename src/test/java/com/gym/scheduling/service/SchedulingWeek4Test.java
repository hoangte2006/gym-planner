package com.gym.scheduling.service;

import com.gym.core.dao.ClassRegistrationDao;
import com.gym.core.dao.ClassSessionDao;
import com.gym.core.dao.MemberDao;
import com.gym.core.dao.MembershipDao;
import com.gym.core.dao.RoomDao;
import com.gym.core.dao.SessionEquipmentUsageDao;
import com.gym.core.dao.SessionDao;
import com.gym.core.model.ClassRegistration;
import com.gym.core.model.ClassSession;
import com.gym.core.model.Equipment;
import com.gym.core.model.Manager;
import com.gym.core.model.Member;
import com.gym.core.model.Membership;
import com.gym.core.model.MembershipStatus;
import com.gym.core.model.Room;
import com.gym.core.model.Session;
import com.gym.core.model.SessionEquipmentUsage;
import com.gym.core.model.Trainer;
import com.gym.scheduling.dao.SchedulingSessionDao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchedulingWeek4Test {

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
    void weeklyScheduleKeepsLargeResultAndRequestsExactMondayToMondayRange() {
        LocalDate requestedDate = LocalDate.of(2026, 11, 4);
        List<Session> sessions = new ArrayList<>(2_600);
        LocalDateTime monday = LocalDate.of(2026, 11, 2).atStartOfDay();
        for (int i = 0; i < 2_600; i++) {
            ClassSession session = new ClassSession();
            session.setSessionId(i + 1);
            session.setStartTime(monday.plusMinutes(i));
            session.setEndTime(session.getStartTime().plusHours(1));
            sessions.add(session);
        }

        SchedulingSessionDao dao = new SchedulingSessionDao() {
            @Override
            public List<Session> findStartingBetween(LocalDateTime from, LocalDateTime to) {
                assertEquals(monday, from);
                assertEquals(monday.plusWeeks(1), to);
                return sessions;
            }
        };

        List<Session> result = new SessionService(dao).findWeeklySchedule(requestedDate);

        assertEquals(2_600, result.size());
        assertEquals(1, result.get(0).getSessionId());
        assertEquals(2_600, result.get(2_599).getSessionId());
        assertThrows(UnsupportedOperationException.class, () -> result.clear());
    }

    @Test
    void expiredMembershipFromBillingModuleCannotRegisterForClass() {
        ClassSession session = futureClass();
        Member member = memberWithPhoto();
        Membership expired = new Membership();
        expired.setStatus(MembershipStatus.EXPIRED);
        expired.setStartDate(LocalDate.now().minusMonths(2));
        expired.setEndDate(LocalDate.now().minusDays(1));
        AtomicBoolean seatClaimed = new AtomicBoolean();

        ClassRegistrationService service = registrationService(
                session, member, List.of(expired), seatClaimed);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> service.register(10, 20));

        assertTrue(error.getMessage().contains("gói tập đang hiệu lực"));
        assertFalse(seatClaimed.get());
    }

    @Test
    void classConflictMessageNamesTheBlockingSessionAndTime() {
        LocalDateTime start = LocalDate.now().plusDays(4).atTime(10, 0);
        ClassSession blocker = new ClassSession();
        blocker.setSessionId(77);
        blocker.setClassName("Yoga sáng");
        blocker.setMaxCapacity(10);
        blocker.setRoomId(1);
        blocker.setTrainerId(2);
        blocker.setStartTime(start);
        blocker.setEndTime(start.plusHours(1));
        SchedulingSessionDao sessionDao = new SchedulingSessionDao() {
            @Override
            public List<Session> findRoomConflicts(
                    Integer roomId, LocalDateTime from, LocalDateTime to, Integer exceptId) {
                return List.of(blocker);
            }
        };
        Room room = new Room();
        room.setRoomId(1);
        room.setRoomName("Studio A");
        room.setCapacity(20);
        Trainer trainer = new Trainer();
        trainer.setStaffId(2);
        trainer.setFullName("Huấn luyện viên An");
        ClassSchedulingService service = new ClassSchedulingService(
                new ClassSessionDao(),
                new RoomDao() {
                    @Override
                    public Optional<Room> findById(Integer id) {
                        return Optional.of(room);
                    }
                },
                new com.gym.core.dao.TrainerDao() {
                    @Override
                    public Optional<Trainer> findById(Integer id) {
                        return Optional.of(trainer);
                    }
                },
                new SessionService(sessionDao));

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> service.createClass(
                        "Pilates", 8, 1, 2, start, start.plusHours(1)));

        assertTrue(error.getMessage().contains("Yoga sáng"));
        assertTrue(error.getMessage().contains("#77"));
        assertTrue(error.getMessage().contains(start.toString()));
        assertTrue(error.getMessage().contains(start.plusHours(1).toString()));
    }

    @Test
    void memberWithoutPhotoFromMemberModuleCannotRegisterForClass() {
        ClassSession session = futureClass();
        Member member = new Member();
        member.setVisitorId(20);
        AtomicBoolean membershipChecked = new AtomicBoolean();
        ClassRegistrationService service = new ClassRegistrationService(
                classDao(session),
                new ClassRegistrationDao(),
                memberDao(member),
                new MembershipDao() {
                    @Override
                    public List<Membership> findByVisitor(int visitorId) {
                        membershipChecked.set(true);
                        return List.of();
                    }
                },
                new SessionDao());

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> service.register(10, 20));

        assertTrue(error.getMessage().contains("chưa có ảnh"));
        assertFalse(membershipChecked.get());
    }

    @Test
    void sessionTimesMustStayWithinDailyHoursAndQuarterHourGrid() {
        LocalDateTime day = LocalDate.of(2030, 1, 1).atStartOfDay();

        SessionTimeRules.validateRange(day.withHour(6), day.withHour(6).plusMinutes(30));
        assertThrows(IllegalArgumentException.class,
                () -> SessionTimeRules.validateRange(
                        day.withHour(5).withMinute(45), day.withHour(6).withMinute(15)));
        assertThrows(IllegalArgumentException.class,
                () -> SessionTimeRules.validateRange(
                        day.withHour(21).withMinute(45), day.withHour(22).plusMinutes(15)));
        assertThrows(IllegalArgumentException.class,
                () -> SessionTimeRules.validateRange(
                        day.withHour(21).withMinute(30), day.plusDays(1).withHour(6)));
        assertThrows(IllegalArgumentException.class,
                () -> SessionTimeRules.validateRange(
                        day.withHour(6), day.withHour(6).plusMinutes(15)));
        assertThrows(IllegalArgumentException.class,
                () -> SessionTimeRules.validateRange(
                        day.withHour(6), day.withHour(9).withMinute(15)));
        assertThrows(IllegalArgumentException.class,
                () -> SessionTimeRules.validateRange(
                        day.withHour(6).withMinute(5),
                        day.withHour(6).withMinute(35)));
    }

    @Test
    void reschedulingRejectsEquipmentThatIsNoLongerAvailable() {
        ClassSession current = futureClass();
        current.setRoomId(1);
        current.setTrainerId(2);
        current.setMaxCapacity(10);
        ClassSession candidate = futureClass();
        candidate.setRoomId(1);
        candidate.setTrainerId(2);
        candidate.setMaxCapacity(10);
        LocalDateTime start = LocalDate.now().plusDays(4).atTime(10, 0);
        candidate.setStartTime(start);
        candidate.setEndTime(start.plusHours(1));
        current.setStartTime(start);
        current.setEndTime(start.plusHours(1));

        SessionEquipmentUsage usage = new SessionEquipmentUsage();
        usage.setEquipmentId(7);
        usage.setSessionId(current.getSessionId());
        usage.setReservedFrom(start);
        usage.setReservedTo(start.plusMinutes(30));
        AtomicBoolean availabilityChecked = new AtomicBoolean();
        AtomicBoolean sessionUpdated = new AtomicBoolean();

        SchedulingSessionDao sessionDao = new SchedulingSessionDao() {
            @Override
            public Optional<Session> findById(Integer id) {
                return Optional.of(current);
            }

            @Override
            public List<Session> findRoomConflicts(
                    Integer roomId, LocalDateTime from, LocalDateTime to, Integer exceptId) {
                return List.of();
            }

            @Override
            public List<Session> findTrainerConflicts(
                    Integer trainerId, LocalDateTime from, LocalDateTime to, Integer exceptId) {
                return List.of();
            }

            @Override
            public List<Equipment> findAvailableEquipment(
                    Integer roomId, LocalDateTime from, LocalDateTime to, Integer exceptId) {
                availabilityChecked.set(true);
                return List.of();
            }

            @Override
            public void update(Session session) {
                sessionUpdated.set(true);
            }
        };
        Room room = new Room();
        room.setRoomId(1);
        room.setCapacity(20);
        SessionService service = new SessionService(
                sessionDao,
                new com.gym.core.dao.RoomDao() {
                    @Override
                    public Optional<Room> findById(Integer id) {
                        return Optional.of(room);
                    }
                },
                new com.gym.core.dao.TrainerDao(),
                new ClassRegistrationDao(),
                new SessionEquipmentUsageDao() {
                    @Override
                    public List<SessionEquipmentUsage> findBySession(int sessionId) {
                        return List.of(usage);
                    }
                });

        IllegalStateException error = assertThrows(
                IllegalStateException.class, () -> service.updateSession(candidate));

        assertTrue(error.getMessage().contains("thiết bị #7"));
        assertTrue(availabilityChecked.get());
        assertFalse(sessionUpdated.get());
    }

    private static ClassRegistrationService registrationService(
            ClassSession session,
            Member member,
            List<Membership> memberships,
            AtomicBoolean seatClaimed) {
        return new ClassRegistrationService(
                classDao(session),
                new ClassRegistrationDao() {
                    @Override
                    public Optional<ClassRegistration> findById(
                            com.gym.core.model.ClassRegistrationKey key) {
                        return Optional.of(new ClassRegistration());
                    }
                },
                memberDao(member),
                new MembershipDao() {
                    @Override
                    public List<Membership> findByVisitor(int visitorId) {
                        return memberships;
                    }
                },
                new SessionDao() {
                    @Override
                    public void registerClassSeat(int sessionId, int visitorId) {
                        seatClaimed.set(true);
                    }
                });
    }

    private static ClassSessionDao classDao(ClassSession session) {
        return new ClassSessionDao() {
            @Override
            public Optional<ClassSession> findById(Integer id) {
                return Optional.of(session);
            }
        };
    }

    private static MemberDao memberDao(Member member) {
        return new MemberDao() {
            @Override
            public Optional<Member> findById(Integer id) {
                return Optional.of(member);
            }
        };
    }

    private static ClassSession futureClass() {
        ClassSession session = new ClassSession();
        session.setSessionId(10);
        session.setStartTime(LocalDateTime.now().plusDays(2));
        session.setEndTime(session.getStartTime().plusHours(1));
        return session;
    }

    private static Member memberWithPhoto() {
        Member member = new Member();
        member.setVisitorId(20);
        member.setPhotoOnFile(true);
        return member;
    }
}
