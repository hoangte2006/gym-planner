package com.gym.facility.dto;

import com.gym.core.model.Session;
import com.gym.core.model.Trainer;

import java.util.List;

/**
 * Dữ liệu tổng hợp hiển thị trên hồ sơ huấn luyện viên.
 */
public record TrainerProfile(
        Trainer trainer,
        long totalSessions,
        List<Session> upcomingSessions
) {
    public TrainerProfile {
        upcomingSessions = List.copyOf(upcomingSessions);
    }

    public int upcomingSessionCount() {
        return upcomingSessions.size();
    }
}