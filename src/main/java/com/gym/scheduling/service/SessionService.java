package com.gym.scheduling.service;

import com.gym.core.DateTimeUtils;
import com.gym.core.dao.ClassRegistrationDao;
import com.gym.core.dao.RoomDao;
import com.gym.core.dao.SessionEquipmentUsageDao;
import com.gym.core.dao.TrainerDao;
import com.gym.core.model.BookingStatus;
import com.gym.core.model.ClassSession;
import com.gym.core.model.Equipment;
import com.gym.core.model.PtSession;
import com.gym.core.model.Room;
import com.gym.core.model.Session;
import com.gym.core.model.SessionEquipmentUsage;
import com.gym.core.model.StaffRole;
import com.gym.core.model.Trainer;
import com.gym.scheduling.dao.SchedulingSessionDao;

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

    private final SchedulingSessionDao sessionDao;
    private final RoomDao roomDao;
    private final TrainerDao trainerDao;
    private final ClassRegistrationDao registrationDao;
    private final SessionEquipmentUsageDao equipmentUsageDao;

    public SessionService() {
        this(new SchedulingSessionDao(), new RoomDao(), new TrainerDao(),
                new ClassRegistrationDao(), new SessionEquipmentUsageDao());
    }

    public SessionService(SchedulingSessionDao sessionDao) {
        this(sessionDao, new RoomDao(), new TrainerDao(),
                new ClassRegistrationDao(), new SessionEquipmentUsageDao());
    }

    public SessionService(
            SchedulingSessionDao sessionDao,
            RoomDao roomDao,
            TrainerDao trainerDao,
            ClassRegistrationDao registrationDao) {
        this(sessionDao, roomDao, trainerDao, registrationDao,
                new SessionEquipmentUsageDao());
    }

    public SessionService(
            SchedulingSessionDao sessionDao,
            RoomDao roomDao,
            TrainerDao trainerDao,
            ClassRegistrationDao registrationDao,
            SessionEquipmentUsageDao equipmentUsageDao) {
        this.sessionDao = Objects.requireNonNull(sessionDao, "sessionDao must not be null");
        this.roomDao = Objects.requireNonNull(roomDao, "roomDao must not be null");
        this.trainerDao = Objects.requireNonNull(trainerDao, "trainerDao must not be null");
        this.registrationDao = Objects.requireNonNull(registrationDao, "registrationDao must not be null");
        this.equipmentUsageDao = Objects.requireNonNull(
                equipmentUsageDao, "equipmentUsageDao must not be null");
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

        return List.copyOf(sessionDao.findStartingBetween(from, to));
    }

    public List<Session> findMyWeeklySchedule(LocalDate dateInWeek) {
        SchedulingAuthorization.requireSchedulingAccess();
        if (!com.gym.app.Session.hasRole(StaffRole.TRAINER)) {
            throw new SecurityException("Chức năng này chỉ dành cho huấn luyện viên.");
        }
        Objects.requireNonNull(dateInWeek, "dateInWeek must not be null");
        int trainerId = com.gym.app.Session.getStaff().getStaffId();
        LocalDate monday = dateInWeek.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return sessionDao.findTrainerStartingBetween(
                trainerId, monday.atStartOfDay(), monday.plusWeeks(1).atStartOfDay());
    }

    public List<Room> findRooms() {
        SchedulingAuthorization.requireSchedulingAccess();
        return roomDao.findAll();
    }

    public List<Trainer> findTrainers() {
        SchedulingAuthorization.requireSchedulingAccess();
        return trainerDao.findAll();
    }

    public void updateSession(Session candidate) {
        SchedulingAuthorization.requireManagementAccess();
        validateCandidate(candidate);
        if (candidate.getSessionId() == null) {
            throw new IllegalArgumentException("Không tìm thấy mã buổi tập cần sửa.");
        }
        Session current = sessionDao.findById(candidate.getSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Buổi tập không còn tồn tại."));
        if (current.getClass() != candidate.getClass()) {
            throw new IllegalArgumentException("Không thể đổi loại của buổi tập.");
        }
        if (current instanceof PtSession pt
                && pt.getBookingStatus() != BookingStatus.SCHEDULED) {
            throw new IllegalStateException("Chỉ có thể sửa buổi PT đang ở trạng thái Đã đặt lịch.");
        }
        if (current instanceof PtSession currentPt
                && candidate instanceof PtSession candidatePt
                && (!Objects.equals(currentPt.getMemberId(), candidatePt.getMemberId())
                || currentPt.getBookingStatus() != candidatePt.getBookingStatus())) {
            throw new IllegalArgumentException(
                    "Không thể đổi hội viên hoặc trạng thái PT khi dời lịch.");
        }
        if (!candidate.getStartTime().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Không thể dời buổi tập về quá khứ.");
        }
        if (candidate instanceof ClassSession classSession) {
            Room room = roomDao.findById(candidate.getRoomId())
                    .orElseThrow(() -> new IllegalArgumentException("Phòng tập không tồn tại."));
            if (classSession.getMaxCapacity() > room.getCapacity()) {
                throw new IllegalArgumentException(
                        "Sức chứa lớp vượt quá sức chứa của phòng đã chọn.");
            }
        }

        sessionDao.findRoomConflicts(candidate.getRoomId(), candidate.getStartTime(),
                        candidate.getEndTime(), candidate.getSessionId())
                .stream().findFirst().ifPresent(conflict -> {
                    throw conflictMessage("Phòng", conflict);
                });
        sessionDao.findTrainerConflicts(candidate.getTrainerId(), candidate.getStartTime(),
                        candidate.getEndTime(), candidate.getSessionId())
                .stream().findFirst().ifPresent(conflict -> {
                    throw conflictMessage("Huấn luyện viên", conflict);
                });
        validateEquipmentReservations(candidate);
        sessionDao.update(candidate);
    }

    private void validateEquipmentReservations(Session candidate) {
        for (SessionEquipmentUsage usage :
                equipmentUsageDao.findBySession(candidate.getSessionId())) {
            if (usage.getReservedFrom().isBefore(candidate.getStartTime())
                    || usage.getReservedTo().isAfter(candidate.getEndTime())) {
                throw new IllegalStateException(
                        "Không thể dời buổi tập: thời gian đặt thiết bị #"
                                + usage.getEquipmentId()
                                + " sẽ nằm ngoài thời gian mới của buổi.");
            }

            List<Equipment> available = sessionDao.findAvailableEquipment(
                    candidate.getRoomId(), usage.getReservedFrom(),
                    usage.getReservedTo(), candidate.getSessionId());
            boolean stillAvailable = available.stream().anyMatch(equipment ->
                    Objects.equals(equipment.getEquipmentId(), usage.getEquipmentId()));
            if (!stillAvailable) {
                throw new IllegalStateException(
                        "Không thể dời buổi tập: thiết bị #"
                                + usage.getEquipmentId()
                                + " không còn khả dụng trong phòng hoặc khung giờ mới.");
            }
        }
    }

    public void changePtStatus(int sessionId, BookingStatus newStatus) {
        SchedulingAuthorization.requireManagementAccess();
        Objects.requireNonNull(newStatus, "newStatus must not be null");
        Session result = sessionDao.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy buổi tập."));
        if (!(result instanceof PtSession pt)) {
            throw new IllegalArgumentException("Buổi được chọn không phải buổi PT.");
        }
        BookingStatus oldStatus = pt.getBookingStatus();
        if (oldStatus == newStatus) {
            return;
        }
        if (oldStatus == BookingStatus.COMPLETED
                || (oldStatus == BookingStatus.SCHEDULED
                && newStatus != BookingStatus.COMPLETED && newStatus != BookingStatus.CANCELLED)
                || (oldStatus == BookingStatus.CANCELLED
                && newStatus != BookingStatus.SCHEDULED)) {
            throw new IllegalStateException("Chuyển trạng thái buổi PT không hợp lệ.");
        }
        if (oldStatus == BookingStatus.CANCELLED && newStatus == BookingStatus.SCHEDULED) {
            sessionDao.findRoomConflicts(pt.getRoomId(), pt.getStartTime(), pt.getEndTime(), sessionId)
                    .stream().findFirst().ifPresent(conflict -> {
                        throw conflictMessage("Phòng", conflict);
                    });
            sessionDao.findTrainerConflicts(
                            pt.getTrainerId(), pt.getStartTime(), pt.getEndTime(), sessionId)
                    .stream().findFirst().ifPresent(conflict -> {
                        throw conflictMessage("Huấn luyện viên", conflict);
                    });
            if (sessionDao.hasConflictingEquipmentReservations(sessionId)) {
                throw new IllegalStateException(
                        "Thiết bị của buổi PT đã được buổi khác đặt trong khung giờ này.");
            }
        }
        pt.setBookingStatus(newStatus);
        sessionDao.update(pt);
    }

    public DeleteResult deleteSession(int sessionId) {
        SchedulingAuthorization.requireManagementAccess();
        Session session = sessionDao.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Buổi tập không còn tồn tại."));
        DeleteResult impact = deleteImpact(session);
        sessionDao.deleteById(sessionId);
        return impact;
    }

    public DeleteResult previewDelete(int sessionId) {
        SchedulingAuthorization.requireManagementAccess();
        Session session = sessionDao.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Buổi tập không còn tồn tại."));
        return deleteImpact(session);
    }

    private DeleteResult deleteImpact(Session session) {
        if (session instanceof PtSession pt
                && pt.getBookingStatus() == BookingStatus.COMPLETED) {
            throw new IllegalStateException("Không thể xóa buổi PT đã hoàn thành.");
        }
        int registrations = session instanceof ClassSession
                ? registrationDao.findBySession(session.getSessionId()).size() : 0;
        return new DeleteResult(session.getSessionTitle(), registrations);
    }

    public record DeleteResult(String sessionTitle, int deletedRegistrations) {
    }

    private static IllegalStateException conflictMessage(String resource, Session conflict) {
        String title = conflict.getSessionTitle();
        String description = title == null || title.isBlank()
                ? "buổi tập #" + conflict.getSessionId()
                : "\"" + title + "\" (#" + conflict.getSessionId() + ")";
        return new IllegalStateException(resource + " đã có buổi tập "
                + description + " từ " + conflict.getStartTime()
                + " đến " + conflict.getEndTime() + ".");
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
                candidate.getEndTime(),
                candidate.getSessionId()));
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
                candidate.getEndTime(),
                candidate.getSessionId()));
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

        SessionTimeRules.validateRange(
                candidate.getStartTime(), candidate.getEndTime());
        if (candidate.getRoomId() == null) {
            throw new IllegalArgumentException("Session room ID is required");
        }
        if (candidate.getTrainerId() == null) {
            throw new IllegalArgumentException("Session trainer ID is required");
        }
    }
}