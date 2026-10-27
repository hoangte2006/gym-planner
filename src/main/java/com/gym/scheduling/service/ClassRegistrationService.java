package com.gym.scheduling.service;

import com.gym.core.dao.ClassRegistrationDao;
import com.gym.core.dao.ClassSessionDao;
import com.gym.core.dao.MemberDao;
import com.gym.core.dao.MembershipDao;
import com.gym.core.model.AttendanceStatus;
import com.gym.core.model.ClassRegistration;
import com.gym.core.model.ClassRegistrationKey;
import com.gym.core.model.ClassSession;
import com.gym.core.model.Member;
import com.gym.core.model.Membership;
import com.gym.core.model.MembershipStatus;
import com.gym.scheduling.dao.SessionDao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/** Registration and attendance rules for group classes. */
public class ClassRegistrationService {

    private final ClassSessionDao classDao;
    private final ClassRegistrationDao registrationDao;
    private final MemberDao memberDao;
    private final MembershipDao membershipDao;
    private final SessionDao sessionDao;

    public ClassRegistrationService() {
        this(
                new ClassSessionDao(),
                new ClassRegistrationDao(),
                new MemberDao(),
                new MembershipDao(),
                new SessionDao()
        );
    }

    public ClassRegistrationService(
            ClassSessionDao classDao,
            ClassRegistrationDao registrationDao,
            MemberDao memberDao,
            MembershipDao membershipDao) {
        this(classDao, registrationDao, memberDao, membershipDao, new SessionDao());
    }

    public ClassRegistrationService(
            ClassSessionDao classDao,
            ClassRegistrationDao registrationDao,
            MemberDao memberDao,
            MembershipDao membershipDao,
            SessionDao sessionDao) {
        this.classDao = Objects.requireNonNull(classDao);
        this.registrationDao = Objects.requireNonNull(registrationDao);
        this.memberDao = Objects.requireNonNull(memberDao);
        this.membershipDao = Objects.requireNonNull(membershipDao);
        this.sessionDao = Objects.requireNonNull(sessionDao);
    }

    public List<Member> searchMembers(String keyword) {
        SchedulingAuthorization.requireManagementAccess();
        return memberDao.search(keyword, 1, 20).items();
    }

    public List<ClassAvailability> findUpcomingClasses() {
        SchedulingAuthorization.requireSchedulingAccess();
        LocalDateTime now = LocalDateTime.now();
        return classDao.findAll().stream()
                .filter(session -> session.getStartTime() != null
                        && session.getStartTime().isAfter(now))
                .sorted((first, second) ->
                        first.getStartTime().compareTo(second.getStartTime()))
                .map(session -> {
                    int registered = activeRegistrationCount(
                            registrationDao.findBySession(session.getSessionId()));
                    return new ClassAvailability(
                            session, registered,
                            Math.max(0, session.getMaxCapacity() - registered));
                })
                .toList();
    }

    public List<RegistrationEntry> findRegistrations(int sessionId) {
        SchedulingAuthorization.requireSchedulingAccess();
        return registrationDao.findBySession(sessionId).stream()
                .map(registration -> {
                    Member member = memberDao.findById(registration.getVisitorId())
                            .orElseThrow(() -> new IllegalStateException(
                                    "Không tìm thấy hội viên #" + registration.getVisitorId()));
                    return new RegistrationEntry(
                            registration,
                            member.getFullName(),
                            member.getPhone());
                })
                .toList();
    }

    public ClassRegistration register(int sessionId, int visitorId) {
        SchedulingAuthorization.requireManagementAccess();
        ClassSession classSession = classDao.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Lớp tập không tồn tại."));
        if (classSession.getStartTime() == null
                || !classSession.getStartTime().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Không thể đăng ký lớp đã bắt đầu hoặc đã kết thúc.");
        }

        Member member = memberDao.findById(visitorId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hội viên."));
        if (!member.hasPhoto()) {
            throw new IllegalArgumentException(
                    "Hội viên chưa có ảnh hồ sơ. Vui lòng cập nhật ảnh trước khi đăng ký lớp.");
        }

        LocalDate today = LocalDate.now();
        boolean hasValidMembership = membershipDao.findByVisitor(visitorId).stream()
                .anyMatch(membership -> isValidMembership(membership, today));
        if (!hasValidMembership) {
            throw new IllegalArgumentException(
                    "Hội viên không có gói tập đang hiệu lực hôm nay.");
        }

        sessionDao.registerClassSeat(sessionId, visitorId);
        return registrationDao.findById(new ClassRegistrationKey(sessionId, visitorId))
                .orElseThrow(() -> new IllegalStateException(
                        "Không tìm thấy đăng ký sau khi hoàn tất giao dịch."));
    }

    public void cancelRegistration(int sessionId, int visitorId) {
        SchedulingAuthorization.requireManagementAccess();
        ClassRegistration registration = registrationDao.findById(
                        new ClassRegistrationKey(sessionId, visitorId))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký lớp."));
        if (registration.getAttendanceStatus() == AttendanceStatus.ATTENDED) {
            throw new IllegalArgumentException(
                    "Không thể hủy đăng ký đã được ghi nhận tham gia.");
        }
        if (registration.getAttendanceStatus() != AttendanceStatus.CANCELLED) {
            registration.setAttendanceStatus(AttendanceStatus.CANCELLED);
            registrationDao.update(registration);
        }
    }

    public void markAttendance(int sessionId, int visitorId, AttendanceStatus status) {
        SchedulingAuthorization.requireSchedulingAccess();
        if (status != AttendanceStatus.ATTENDED
                && status != AttendanceStatus.CONFIRMED) {
            throw new IllegalArgumentException("Trạng thái điểm danh không hợp lệ.");
        }
        ClassRegistration registration = registrationDao.findById(
                        new ClassRegistrationKey(sessionId, visitorId))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký lớp."));
        if (registration.getAttendanceStatus() == AttendanceStatus.CANCELLED) {
            throw new IllegalArgumentException("Đăng ký đã hủy không thể điểm danh.");
        }
        registration.setAttendanceStatus(status);
        registrationDao.update(registration);
    }

    private static int activeRegistrationCount(List<ClassRegistration> registrations) {
        return (int) registrations.stream()
                .filter(registration ->
                        registration.getAttendanceStatus() != AttendanceStatus.CANCELLED)
                .count();
    }

    private static boolean isValidMembership(Membership membership, LocalDate today) {
        return membership.getStatus() == MembershipStatus.ACTIVE
                && membership.getStartDate() != null
                && membership.getEndDate() != null
                && !today.isBefore(membership.getStartDate())
                && !today.isAfter(membership.getEndDate());
    }

    public record ClassAvailability(
            ClassSession session,
            int registeredCount,
            int seatsRemaining) {
    }

    public record RegistrationEntry(
            ClassRegistration registration,
            String memberName,
            String phone) {
    }
}
