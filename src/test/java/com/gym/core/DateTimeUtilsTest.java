package com.gym.core;

import com.gym.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateTimeUtilsTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 12, 8, 0);

    private static LocalDateTime at(int day, int hour, int minute) {
        return LocalDateTime.of(2026, 10, day, hour, minute);
    }

    @Test
    void formatAndParseRoundTrip() {
        LocalDate d = LocalDate.of(2026, 12, 25);
        assertEquals("25/12/2026", DateTimeUtils.format(d));
        assertEquals(d, DateTimeUtils.parseDate("25/12/2026"));
        assertEquals(at(13, 9, 30), DateTimeUtils.parseDateTime("13/10/2026 09:30"));
        assertEquals("", DateTimeUtils.format((LocalDate) null));
    }

    @Test
    void parseErrorsAreVietnameseBusinessExceptions() {
        BusinessException ex = assertThrows(BusinessException.class, () -> DateTimeUtils.parseDate("2026-12-25"));
        assertTrue(ex.getMessage().contains("dd/MM/yyyy"));
        assertThrows(BusinessException.class, () -> DateTimeUtils.parseDate(null));
        assertThrows(BusinessException.class, () -> DateTimeUtils.parseDateTime("31/02/2026 10:00"));
    }

    @Test
    void overlapIsHalfOpen() {
        assertTrue(DateTimeUtils.overlaps(at(13, 10, 0), at(13, 11, 0), at(13, 10, 30), at(13, 11, 30)));
        assertTrue(DateTimeUtils.overlaps(at(13, 10, 0), at(13, 12, 0), at(13, 10, 30), at(13, 11, 0)));   // chứa
        // Buổi này kết thúc đúng lúc buổi kia bắt đầu: KHÔNG chồng
        assertFalse(DateTimeUtils.overlaps(at(13, 10, 0), at(13, 11, 0), at(13, 11, 0), at(13, 12, 0)));
        assertFalse(DateTimeUtils.overlaps(at(13, 10, 0), at(13, 11, 0), at(13, 14, 0), at(13, 15, 0)));
    }

    @Test
    void weekBoundariesStartOnMonday() {
        LocalDate wednesday = LocalDate.of(2026, 10, 14);
        assertEquals(LocalDate.of(2026, 10, 12), DateTimeUtils.startOfWeek(wednesday));
        assertEquals(LocalDate.of(2026, 10, 18), DateTimeUtils.endOfWeek(wednesday));
        assertEquals(LocalDate.of(2026, 10, 12), DateTimeUtils.startOfWeek(LocalDate.of(2026, 10, 12)));
        assertEquals(LocalDate.of(2026, 10, 31), DateTimeUtils.endOfMonth(wednesday));
    }

    @Test
    void membershipDates() {
        assertEquals(LocalDate.of(2027, 1, 12), DateTimeUtils.membershipEnd(LocalDate.of(2026, 10, 12), 3));
        LocalDate today = LocalDate.of(2026, 10, 12);
        assertTrue(DateTimeUtils.isExpiringSoon(today.plusDays(7), today, 7));
        assertFalse(DateTimeUtils.isExpiringSoon(today.plusDays(8), today, 7));
        assertFalse(DateTimeUtils.isExpiringSoon(today.minusDays(1), today, 7));   // đã hết hạn
    }

    @Test
    void validSessionSlotPasses() {
        assertDoesNotThrow(() -> DateTimeUtils.validateSessionSlot(at(13, 6, 0), at(13, 7, 0), NOW));
        assertDoesNotThrow(() -> DateTimeUtils.validateSessionSlot(at(13, 20, 0), at(13, 22, 0), NOW));
        assertDoesNotThrow(() -> DateTimeUtils.validateSessionSlot(at(13, 9, 15), at(13, 9, 45), NOW));
    }

    @Test
    void invalidSessionSlotsAreRejectedWithReason() {
        assertMessage("cùng một ngày", () -> DateTimeUtils.validateSessionSlot(at(13, 21, 0), at(14, 1, 0), NOW));
        assertMessage("sau giờ bắt đầu", () -> DateTimeUtils.validateSessionSlot(at(13, 10, 0), at(13, 9, 0), NOW));
        assertMessage("06:00", () -> DateTimeUtils.validateSessionSlot(at(13, 5, 45), at(13, 6, 45), NOW));
        assertMessage("22:00", () -> DateTimeUtils.validateSessionSlot(at(13, 21, 30), at(13, 22, 30), NOW));
        assertMessage("30 đến 180", () -> DateTimeUtils.validateSessionSlot(at(13, 10, 0), at(13, 10, 15), NOW));
        assertMessage("30 đến 180", () -> DateTimeUtils.validateSessionSlot(at(13, 10, 0), at(13, 13, 30), NOW));
        assertMessage("bước 15", () -> DateTimeUtils.validateSessionSlot(at(13, 10, 10), at(13, 11, 10), NOW));
        assertMessage("sau thời điểm hiện tại", () -> DateTimeUtils.validateSessionSlot(at(12, 7, 0), at(12, 8, 0), NOW));
    }

    private static void assertMessage(String expectedPart, Runnable action) {
        BusinessException ex = assertThrows(BusinessException.class, action::run);
        assertTrue(ex.getMessage().contains(expectedPart), ex.getMessage());
    }
}
