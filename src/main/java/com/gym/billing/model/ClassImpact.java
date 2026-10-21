package com.gym.billing.model;

import java.time.LocalDateTime;

/** Lớp trong tương lai cần lễ tân xử lý thủ công khi hủy gói. */
public record ClassImpact(int sessionId, String className, LocalDateTime startTime,
                          LocalDateTime endTime, String registrationStatus) { }
