package com.gym.dashboard.model;

import java.time.LocalDateTime;

/** Một buổi lớp học và số chỗ đã đăng ký (không tính đăng ký đã hủy): một dòng của bảng "lớp đông nhất". */
public record BusiestClass(String className, LocalDateTime start, int registrations, int capacity) {
}
