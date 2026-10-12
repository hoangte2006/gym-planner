package com.gym.core.model;

import java.time.Duration;
import java.time.LocalDateTime;

/** Buổi tập (lớp cha, bảng SESSION). Là ClassSession hoặc PtSession. */
public abstract class Session {

    private Integer sessionId;
    private SessionType sessionType;
    private Integer roomId;
    private Integer trainerId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    /** Mã buổi. */
    public Integer getSessionId() {
        return sessionId;
    }

    public void setSessionId(Integer sessionId) {
        this.sessionId = sessionId;
    }

    /** Loại buổi. */
    public SessionType getSessionType() {
        return sessionType;
    }

    public void setSessionType(SessionType sessionType) {
        this.sessionType = sessionType;
    }

    /** Mã phòng. */
    public Integer getRoomId() {
        return roomId;
    }

    public void setRoomId(Integer roomId) {
        this.roomId = roomId;
    }

    /** Mã huấn luyện viên. */
    public Integer getTrainerId() {
        return trainerId;
    }

    public void setTrainerId(Integer trainerId) {
        this.trainerId = trainerId;
    }

    /** Giờ bắt đầu. */
    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    /** Giờ kết thúc. */
    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    /** Tiêu đề buổi tập hiển thị trên lịch. Lớp con ghi đè. */
    public abstract String getSessionTitle();

    /** Thời lượng tính bằng phút. */
    public long getDurationMinutes() {
        return Duration.between(startTime, endTime).toMinutes();
    }
}
