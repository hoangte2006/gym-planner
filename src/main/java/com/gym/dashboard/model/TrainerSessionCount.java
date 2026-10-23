package com.gym.dashboard.model;

/** Số buổi dạy của một huấn luyện viên (lớp học và buổi PT chưa hủy): một dòng của bảng "top huấn luyện viên". */
public record TrainerSessionCount(String trainerName, int sessions) {
}
