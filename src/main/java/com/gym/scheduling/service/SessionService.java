package com.gym.scheduling.service;

import com.gym.core.DateTimeUtils;
import com.gym.core.dao.SessionDao;
import com.gym.core.model.Session;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Nghiệp vụ truy vấn lịch tuần và kiểm tra xung đột lịch tập. */
public class SessionService {

    private final SessionDao sessionDao;

    public SessionService() {
        this(new SessionDao());
    }

    /** Constructor nhận DAO để có thể thay thế trong kiểm thử. */
    public SessionService(SessionDao sessionDao) {
        this.sessionDao = Objects.requireNonNull(sessionDao, "sessionDao không được null");
    }

    /**
     * Lấy các buổi bắt đầu trong tuần chứa ngày được truyền vào.
     * Tuần bắt đầu thứ Hai 00:00 và kết thúc trước thứ Hai tuần kế tiếp.
     */
    public List<Session> findWeeklySchedule(LocalDate dateInWeek) {
        Objects.requireNonNull(dateInWeek, "Ngày xem lịch không được null");
        LocalDate monday = DateTimeUtils.startOfWeek(dateInWeek);
        LocalDateTime from = monday.atStartOfDay();
        LocalDateTime to = monday.plusWeeks(1).atStartOfDay();
        return sessionDao.findStartingBetween(from, to).stream()
                .sorted(Comparator.comparing(Session::getStartTime))
                .toList();
    }

    /** Trả về buổi đang chiếm phòng nếu candidate bị trùng; không bao gồm chính nó khi cập nhật. */
    public Optional<Session> findRoomConflict(Session candidate) {
        validateCandidate(candidate);
        return findRoomConflict(candidate, sessionDao.findAll());
    }

    /** Trả về buổi đang chiếm huấn luyện viên nếu candidate bị trùng. */
    public Optional<Session> findTrainerConflict(Session candidate) {
        validateCandidate(candidate);
        return findTrainerConflict(candidate, sessionDao.findAll());
    }

    /** Phiên bản thuần dữ liệu để kiểm thử quy tắc mà không cần kết nối cơ sở dữ liệu. */
    public static Optional<Session> findRoomConflict(Session candidate, Collection<Session> existingSessions) {
        validateCandidate(candidate);
        Objects.requireNonNull(existingSessions, "Danh sách buổi tập không được null");
        return existingSessions.stream()
                .filter(Objects::nonNull)
                .filter(existing -> !isSameSession(candidate, existing))
                .filter(existing -> Objects.equals(candidate.getRoomId(), existing.getRoomId()))
                .filter(existing -> overlaps(candidate, existing))
                .min(Comparator.comparing(Session::getStartTime));
    }

    /** Phiên bản thuần dữ liệu để kiểm thử quy tắc mà không cần kết nối cơ sở dữ liệu. */
    public static Optional<Session> findTrainerConflict(Session candidate, Collection<Session> existingSessions) {
        validateCandidate(candidate);
        Objects.requireNonNull(existingSessions, "Danh sách buổi tập không được null");
        return existingSessions.stream()
                .filter(Objects::nonNull)
                .filter(existing -> !isSameSession(candidate, existing))
                .filter(existing -> Objects.equals(candidate.getTrainerId(), existing.getTrainerId()))
                .filter(existing -> overlaps(candidate, existing))
                .min(Comparator.comparing(Session::getStartTime));
    }

    private static boolean overlaps(Session a, Session b) {
        return a.getStartTime() != null && a.getEndTime() != null
                && b.getStartTime() != null && b.getEndTime() != null
                && DateTimeUtils.overlaps(a.getStartTime(), a.getEndTime(), b.getStartTime(), b.getEndTime());
    }

    private static boolean isSameSession(Session candidate, Session existing) {
        return candidate.getSessionId() != null
                && Objects.equals(candidate.getSessionId(), existing.getSessionId());
    }

    private static void validateCandidate(Session candidate) {
        Objects.requireNonNull(candidate, "Buổi tập cần kiểm tra không được null");
        if (candidate.getStartTime() == null || candidate.getEndTime() == null) {
            throw new IllegalArgumentException("Buổi tập phải có giờ bắt đầu và giờ kết thúc.");
        }
        if (!candidate.getEndTime().isAfter(candidate.getStartTime())) {
            throw new IllegalArgumentException("Giờ kết thúc phải sau giờ bắt đầu.");
        }
        if (candidate.getRoomId() == null) {
            throw new IllegalArgumentException("Buổi tập phải có mã phòng.");
        }
        if (candidate.getTrainerId() == null) {
            throw new IllegalArgumentException("Buổi tập phải có mã huấn luyện viên.");
        }
    }
}
