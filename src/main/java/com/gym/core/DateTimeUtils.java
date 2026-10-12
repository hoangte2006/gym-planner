package com.gym.core;

import com.gym.core.exception.BusinessException;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/** Hàm tiện ích ngày giờ dùng chung: định dạng, đọc chuỗi, khoảng thời gian, luật khung giờ buổi tập. */
public final class DateTimeUtils {

    // STRICT + "uuuu": ngày không tồn tại (31/02) bị từ chối thay vì âm thầm đổi thành 28/02
    public static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm").withResolverStyle(ResolverStyle.STRICT);

    /** Luật khung giờ buổi tập (kế hoạch ALG, mục lịch tập). */
    public static final LocalTime OPEN_TIME = LocalTime.of(6, 0);
    public static final LocalTime CLOSE_TIME = LocalTime.of(22, 0);
    public static final int MIN_SESSION_MINUTES = 30;
    public static final int MAX_SESSION_MINUTES = 180;
    public static final int SESSION_STEP_MINUTES = 15;

    private DateTimeUtils() {
    }

    // ---------- Định dạng để hiển thị (null thì ra chuỗi rỗng) ----------

    public static String format(LocalDate date) {
        return date == null ? "" : DATE.format(date);
    }

    public static String format(LocalDateTime dateTime) {
        return dateTime == null ? "" : DATE_TIME.format(dateTime);
    }

    public static String formatTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : TIME.format(dateTime);
    }

    // ---------- Đọc chuỗi người dùng nhập ----------

    /** Đọc "dd/MM/yyyy". Sai định dạng thì ném BusinessException tiếng Việt. */
    public static LocalDate parseDate(String text) {
        try {
            return LocalDate.parse(text.trim(), DATE);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new BusinessException("Ngày không đúng định dạng dd/MM/yyyy (ví dụ: 25/12/2026).", e);
        }
    }

    /** Đọc "dd/MM/yyyy HH:mm". */
    public static LocalDateTime parseDateTime(String text) {
        try {
            return LocalDateTime.parse(text.trim(), DATE_TIME);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new BusinessException("Ngày giờ không đúng định dạng dd/MM/yyyy HH:mm (ví dụ: 25/12/2026 09:30).", e);
        }
    }

    // ---------- Khoảng thời gian ----------

    /**
     * Hai khoảng [aStart, aEnd) và [bStart, bEnd) có chồng lấn không.
     * Chạm đầu mút (buổi này kết thúc đúng lúc buổi kia bắt đầu) KHÔNG tính là chồng.
     */
    public static boolean overlaps(LocalDateTime aStart, LocalDateTime aEnd, LocalDateTime bStart, LocalDateTime bEnd) {
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }

    public static long minutesBetween(LocalDateTime start, LocalDateTime end) {
        return Duration.between(start, end).toMinutes();
    }

    /** Thứ Hai của tuần chứa ngày này (tuần bắt đầu từ thứ Hai). */
    public static LocalDate startOfWeek(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /** Chủ Nhật của tuần chứa ngày này. */
    public static LocalDate endOfWeek(LocalDate date) {
        return date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
    }

    public static LocalDate startOfMonth(LocalDate date) {
        return date.withDayOfMonth(1);
    }

    public static LocalDate endOfMonth(LocalDate date) {
        return date.with(TemporalAdjusters.lastDayOfMonth());
    }

    // ---------- Gói tập ----------

    /** Ngày kết thúc gói: ngày bắt đầu cộng số tháng. */
    public static LocalDate membershipEnd(LocalDate start, int durationMonths) {
        return start.plusMonths(durationMonths);
    }

    /** Gói còn từ 0 đến days ngày thì coi là sắp hết hạn (đã quá hạn thì không). */
    public static boolean isExpiringSoon(LocalDate endDate, LocalDate today, int days) {
        long left = ChronoUnit.DAYS.between(today, endDate);
        return left >= 0 && left <= days;
    }

    // ---------- Luật khung giờ buổi tập ----------

    /**
     * Kiểm tra khung giờ một buổi tập theo luật: cùng một ngày, từ 06:00 đến 22:00,
     * dài 30–180 phút, bước 15 phút, và bắt đầu sau thời điểm hiện tại.
     * Vi phạm thì ném BusinessException nói rõ lý do.
     */
    public static void validateSessionSlot(LocalDateTime start, LocalDateTime end, LocalDateTime now) {
        if (start == null || end == null) {
            throw new BusinessException("Phải chọn giờ bắt đầu và giờ kết thúc.");
        }
        if (!start.toLocalDate().equals(end.toLocalDate())) {
            throw new BusinessException("Buổi tập phải bắt đầu và kết thúc trong cùng một ngày.");
        }
        if (!end.isAfter(start)) {
            throw new BusinessException("Giờ kết thúc phải sau giờ bắt đầu.");
        }
        if (start.toLocalTime().isBefore(OPEN_TIME) || end.toLocalTime().isAfter(CLOSE_TIME)) {
            throw new BusinessException("Buổi tập chỉ được xếp trong khung giờ 06:00 – 22:00.");
        }
        long minutes = minutesBetween(start, end);
        if (minutes < MIN_SESSION_MINUTES || minutes > MAX_SESSION_MINUTES) {
            throw new BusinessException("Buổi tập phải dài từ " + MIN_SESSION_MINUTES + " đến "
                    + MAX_SESSION_MINUTES + " phút.");
        }
        if (start.getMinute() % SESSION_STEP_MINUTES != 0 || end.getMinute() % SESSION_STEP_MINUTES != 0
                || minutes % SESSION_STEP_MINUTES != 0) {
            throw new BusinessException("Giờ bắt đầu và kết thúc phải theo bước " + SESSION_STEP_MINUTES + " phút.");
        }
        if (!start.isAfter(now)) {
            throw new BusinessException("Buổi tập mới phải bắt đầu sau thời điểm hiện tại.");
        }
    }
}
