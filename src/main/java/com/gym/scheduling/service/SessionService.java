package com.gym.scheduling.service;

import com.gym.core.DateTimeUtils;
import com.gym.core.model.BookingStatus;
import com.gym.core.model.PtSession;
import com.gym.core.model.Session;
import com.gym.scheduling.dao.SessionDao;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service xử lý lịch tập và kiểm tra xung đột buổi tập.
 *
 * Các hàm kiểm tra xung đột truy vấn dữ liệu đã lọc từ database,
 * thay vì tải toàn bộ SESSION về bộ nhớ.
 */
public class SessionService {

    private final SessionDao sessionDao;

    public SessionService() {
        this(new SessionDao());
    }

    public SessionService(SessionDao sessionDao) {
        this.sessionDao = Objects.requireNonNull(sessionDao, "sessionDao must not be null");
    }

    /**
     * Lấy lịch tập trong tuần chứa ngày truyền vào.
     * Khoảng thời gian: [thứ Hai 00:00, thứ Hai tuần sau 00:00).
     */
    public List<Session> findWeeklySchedule(LocalDate dateInWeek) {
        Objects.requireNonNull(dateInWeek, "dateInWeek must not be null");

        LocalDate monday = dateInWeek.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        LocalDateTime from = monday.atStartOfDay();
        LocalDateTime to = monday.plusWeeks(1).atStartOfDay();

        return sessionDao.findStartingBetween(from, to).stream()
                .sorted(Comparator.comparing(
                        Session::getStartTime,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    /**
     * Tìm buổi tập cùng phòng và có thời gian giao nhau với candidate.
     * Buổi PT đã bị hủy không được xem là xung đột.
     */
    public Optional<Session> findRoomConflict(Session candidate) {
        validateCandidate(candidate);
        return findRoomConflict(candidate, sessionDao.findRoomConflicts(
                candidate.getRoomId(),
                candidate.getStartTime(),
                candidate.getEndTime()));
    }

    /**
     * Tìm buổi tập cùng huấn luyện viên và có thời gian giao nhau với candidate.
     * Buổi PT đã bị hủy không được xem là xung đột.
     */
    public Optional<Session> findTrainerConflict(Session candidate) {
        validateCandidate(candidate);
        return findTrainerConflict(candidate, sessionDao.findTrainerConflicts(
                candidate.getTrainerId(),
                candidate.getStartTime(),
                candidate.getEndTime()));
    }

    /**
     * Kiểm tra xung đột phòng trên danh sách đã có sẵn (hữu ích cho unit test).
     */
    public static Optional<Session> findRoomConflict(
            Session candidate, Collection<Session> existingSessions) {

        validateCandidate(candidate);
        Objects.requireNonNull(existingSessions, "existingSessions must not be null");

        return existingSessions.stream()
                .filter(Objects::nonNull)
                .filter(existing -> !isSameSession(candidate, existing))
                .filter(SessionService::isActiveBooking)
                .filter(existing -> Objects.equals(candidate.getRoomId(), existing.getRoomId()))
                .filter(existing -> overlaps(candidate, existing))
                .min(Comparator.comparing(
                        Session::getStartTime,
                        Comparator.nullsLast(Comparator.naturalOrder())));
    }

    /**
     * Kiểm tra xung đột huấn luyện viên trên danh sách đã có sẵn (hữu ích cho unit test).
     */
    public static Optional<Session> findTrainerConflict(
            Session candidate, Collection<Session> existingSessions) {

        validateCandidate(candidate);
        Objects.requireNonNull(existingSessions, "existingSessions must not be null");

        return existingSessions.stream()
                .filter(Objects::nonNull)
                .filter(existing -> !isSameSession(candidate, existing))
                .filter(SessionService::isActiveBooking)
                .filter(existing -> Objects.equals(
                        candidate.getTrainerId(), existing.getTrainerId()))
                .filter(existing -> overlaps(candidate, existing))
                .min(Comparator.comparing(
                        Session::getStartTime,
                        Comparator.nullsLast(Comparator.naturalOrder())));
    }

    private static boolean isActiveBooking(Session session) {
        if (session instanceof PtSession ptSession) {
            // Chỉ bỏ qua booking đã hủy; trạng thái null vẫn được xem là đang chiếm lịch.
            return ptSession.getBookingStatus() != BookingStatus.CANCELLED;
        }
        return true;
    }

    private static boolean overlaps(Session a, Session b) {
        return a.getStartTime() != null
                && a.getEndTime() != null
                && b.getStartTime() != null
                && b.getEndTime() != null
                && DateTimeUtils.overlaps(
                        a.getStartTime(), a.getEndTime(),
                        b.getStartTime(), b.getEndTime());
    }

    private static boolean isSameSession(Session candidate, Session existing) {
        return candidate.getSessionId() != null
                && Objects.equals(candidate.getSessionId(), existing.getSessionId());
    }

    private static void validateCandidate(Session candidate) {
        Objects.requireNonNull(candidate, "candidate must not be null");

        if (candidate.getStartTime() == null || candidate.getEndTime() == null) {
            throw new IllegalArgumentException("Session start time and end time are required");
        }
        if (!candidate.getEndTime().isAfter(candidate.getStartTime())) {
            throw new IllegalArgumentException("Session end time must be after start time");
        }
        if (candidate.getRoomId() == null) {
            throw new IllegalArgumentException("Session room ID is required");
        }
        if (candidate.getTrainerId() == null) {
            throw new IllegalArgumentException("Session trainer ID is required");
        }
    }
}