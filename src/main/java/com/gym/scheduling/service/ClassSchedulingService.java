
package com.gym.scheduling.service;

import com.gym.core.dao.ClassSessionDao;
import com.gym.core.dao.RoomDao;
import com.gym.core.dao.TrainerDao;
import com.gym.core.model.ClassSession;
import com.gym.core.model.Room;
import com.gym.core.model.Trainer;
import com.gym.core.model.Session;

import java.time.LocalDateTime;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Xử lý nghiệp vụ tạo lớp tập nhóm.
 */
public class ClassSchedulingService {

    private final ClassSessionDao classSessionDao;
    private final RoomDao roomDao;
    private final TrainerDao trainerDao;
    private final SessionService sessionService;
    private final AvailabilityService availabilityService;

    public ClassSchedulingService() {
        this(
            new ClassSessionDao(),
            new RoomDao(),
            new TrainerDao(),
            new SessionService()
        );
    }

    public ClassSchedulingService(
            ClassSessionDao classSessionDao,
            RoomDao roomDao,
            TrainerDao trainerDao,
            SessionService sessionService) {

        this.classSessionDao = Objects.requireNonNull(classSessionDao);
        this.roomDao = Objects.requireNonNull(roomDao);
        this.trainerDao = Objects.requireNonNull(trainerDao);
        this.sessionService = Objects.requireNonNull(sessionService);
        this.availabilityService = new AvailabilityService(sessionService);
    }

    public List<LocalDateTime> findAvailableStarts(
            Integer roomId,
            Integer trainerId,
            LocalDate date,
            Duration duration) {
        return availabilityService.findAvailableStarts(
                roomId, trainerId, date, duration);
    }

    /**
     * Kiểm tra dữ liệu, phòng, huấn luyện viên và xung đột lịch,
     * sau đó tạo lớp.
     *
     * ClassSessionDao.insert() lưu SESSION và CLASS trong cùng
     * một transaction.
     */
    public ClassSession createClass(
            String className,
            int maxCapacity,
            Integer roomId,
            Integer trainerId,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        validateInput(
            className, maxCapacity, roomId, trainerId,
            startTime, endTime
        );

        Room room = roomDao.findById(roomId)
            .orElseThrow(() ->
                new IllegalArgumentException("Phòng tập không tồn tại.")
            );

        Trainer trainer = trainerDao.findById(trainerId)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Huấn luyện viên không tồn tại."
                )
            );

        if (maxCapacity > room.getCapacity()) {
            throw new IllegalArgumentException(
                "Sức chứa lớp (" + maxCapacity
                + ") vượt sức chứa phòng (" + room.getCapacity() + ")."
            );
        }

        ClassSession candidate = new ClassSession();
        candidate.setClassName(className.trim());
        candidate.setMaxCapacity(maxCapacity);
        candidate.setRoomId(roomId);
        candidate.setTrainerId(trainerId);
        candidate.setStartTime(startTime);
        candidate.setEndTime(endTime);

        var roomConflict = sessionService.findRoomConflict(candidate);

        if (roomConflict.isPresent()) {
            throw conflictMessage(
                "Phòng " + room.getRoomName(),
                roomConflict.get()
            );
        }

        var trainerConflict =
            sessionService.findTrainerConflict(candidate);

        if (trainerConflict.isPresent()) {
            throw conflictMessage(
                "Huấn luyện viên " + trainer.getFullName(),
                trainerConflict.get()
            );
        }

        // DAO con chịu trách nhiệm transaction lưu SESSION + CLASS.
        return classSessionDao.insert(candidate);
    }

    private static void validateInput(
            String className,
            int maxCapacity,
            Integer roomId,
            Integer trainerId,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        if (className == null || className.isBlank()) {
            throw new IllegalArgumentException(
                "Vui lòng nhập tên lớp."
            );
        }

        if (maxCapacity <= 0) {
            throw new IllegalArgumentException(
                "Sức chứa lớp phải lớn hơn 0."
            );
        }

        if (roomId == null || trainerId == null) {
            throw new IllegalArgumentException(
                "Vui lòng chọn phòng và huấn luyện viên."
            );
        }

        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException(
                "Vui lòng chọn giờ bắt đầu và kết thúc."
            );
        }

        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException(
                "Giờ kết thúc phải sau giờ bắt đầu."
            );
        }

        if (!startTime.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                "Không thể tạo lớp với thời gian bắt đầu trong quá khứ."
            );
        }
    }

    private static IllegalStateException conflictMessage(
            String resource,
            Session conflict) {

        return new IllegalStateException(
            resource + " đã có buổi tập #" + conflict.getSessionId()
            + " từ " + conflict.getStartTime()
            + " đến " + conflict.getEndTime() + "."
        );
    }
}
