package com.gym.facility.service;

import com.gym.app.Session;
import com.gym.core.dao.SessionDao;
import com.gym.core.dao.TrainerDao;
import com.gym.core.exception.BusinessException;
import com.gym.core.model.StaffRole;
import com.gym.core.model.Trainer;
import com.gym.facility.dto.TrainerProfile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Xử lý nghiệp vụ hồ sơ huấn luyện viên.
 *
 * Bao gồm:
 * - Thông tin Trainer.
 * - Tổng số Session.
 * - Lịch Session sắp tới.
 */
public class TrainerProfileService {

    private final TrainerDao trainerDao;
    private final SessionDao sessionDao;

    public TrainerProfileService(
            TrainerDao trainerDao,
            SessionDao sessionDao
    ) {
        this.trainerDao = Objects.requireNonNull(
                trainerDao, "TrainerDao không được null"
        );

        this.sessionDao = Objects.requireNonNull(
                sessionDao, "SessionDao không được null"
        );
    }

    /**
     * Lấy toàn bộ hồ sơ của một huấn luyện viên.
     */
    public TrainerProfile getTrainerProfile(Integer trainerId) {

        requireManagerAccess();
        validateTrainerId(trainerId);

        // Bước 1: Tìm Trainer.
        Trainer trainer = trainerDao.findById(trainerId)
                .orElseThrow(() ->
                        new BusinessException(
                                "Không tìm thấy huấn luyện viên có mã: " + trainerId
                        )
                );

        // Bước 2: Xác định thời điểm hiện tại.
        LocalDateTime now = LocalDateTime.now();

        // Bước 3: Đếm tổng số Session hợp lệ.
        long totalSessions =
                sessionDao.countByTrainer(trainerId);

        // Bước 4: Lấy lịch tập sắp tới.
        var upcomingSessions =
                sessionDao.findUpcomingByTrainer(trainerId, now);

        // Bước 5: Gom dữ liệu trả về cho UI.
        return new TrainerProfile(
                trainer,
                totalSessions,
                upcomingSessions
        );
    }

    /**
     * Lấy riêng lịch tập sắp tới.
     */
    public List<com.gym.core.model.Session>
    getUpcomingSessions(Integer trainerId) {

        requireManagerAccess();
        validateTrainerId(trainerId);

        if (trainerDao.findById(trainerId).isEmpty()) {
            throw new BusinessException("Huấn luyện viên không tồn tại.");
        }

        return sessionDao.findUpcomingByTrainer(
                trainerId,
                LocalDateTime.now()
        );
    }

    /**
     * Đếm tổng số Session của Trainer.
     */
    public long countSessions(Integer trainerId) {

        requireManagerAccess();
        validateTrainerId(trainerId);

        if (trainerDao.findById(trainerId).isEmpty()) {
            throw new BusinessException("Huấn luyện viên không tồn tại.");
        }

        return sessionDao.countByTrainer(trainerId);
    }

    private void validateTrainerId(Integer trainerId) {
        if (trainerId == null || trainerId <= 0) {
            throw new BusinessException("Mã huấn luyện viên không hợp lệ.");
        }
    }

    private void requireManagerAccess() {
        if (!Session.hasRole(StaffRole.MANAGER)) {
            throw new BusinessException(
                    "Chỉ Quản lý mới được xem hồ sơ huấn luyện viên."
            );
        }
    }
}