package com.gym.scheduling.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

final class SessionTimeRules {

    private static final LocalTime OPENING_TIME = LocalTime.of(6, 0);
    private static final LocalTime CLOSING_TIME = LocalTime.of(22, 0);
    private static final Duration MIN_DURATION = Duration.ofMinutes(30);
    private static final Duration MAX_DURATION = Duration.ofMinutes(180);
    private static final long SLOT_SECONDS = Duration.ofMinutes(15).getSeconds();

    private SessionTimeRules() {
    }

    static void validateRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException(
                    "Vui lòng chọn giờ bắt đầu và kết thúc.");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException(
                    "Giờ kết thúc phải sau giờ bắt đầu.");
        }
        if (!start.toLocalDate().equals(end.toLocalDate())) {
            throw new IllegalArgumentException(
                    "Buổi tập phải bắt đầu và kết thúc trong cùng một ngày.");
        }
        if (start.toLocalTime().isBefore(OPENING_TIME)
                || end.toLocalTime().isAfter(CLOSING_TIME)) {
            throw new IllegalArgumentException(
                    "Buổi tập phải nằm trong giờ hoạt động từ 06:00 đến 22:00.");
        }

        validateDuration(Duration.between(start, end));
        if (!isQuarterHour(start) || !isQuarterHour(end)) {
            throw new IllegalArgumentException(
                    "Giờ bắt đầu và kết thúc phải theo bước 15 phút.");
        }
    }

    static void validateDuration(Duration duration) {
        if (duration == null
                || duration.compareTo(MIN_DURATION) < 0
                || duration.compareTo(MAX_DURATION) > 0) {
            throw new IllegalArgumentException(
                    "Thời lượng buổi tập phải từ 30 đến 180 phút.");
        }
        if (duration.getSeconds() % SLOT_SECONDS != 0
                || duration.getNano() != 0) {
            throw new IllegalArgumentException(
                    "Thời lượng buổi tập phải theo bước 15 phút.");
        }
    }

    private static boolean isQuarterHour(LocalDateTime time) {
        return time.getMinute() % 15 == 0
                && time.getSecond() == 0
                && time.getNano() == 0;
    }
}
