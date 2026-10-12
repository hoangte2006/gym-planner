package com.gym.core;

import com.gym.core.exception.BusinessException;
import com.gym.core.exception.DataAccessException;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErrorTranslatorTest {

    private static SQLException sql(String message, int code) {
        return new SQLException(message, "S0001", code);
    }

    @Test
    void triggerCodeIsTranslated() {
        String msg = ErrorTranslator.toMessage(sql("BR-SCH-05: ...", 50017));
        assertEquals("Hội viên không có gói tập còn hiệu lực nên không thể đăng ký lớp.", msg);
    }

    @Test
    void allTriggerCodesHaveVietnameseMessage() {
        for (int code = 50001; code <= 50029; code++) {
            String msg = ErrorTranslator.toMessage(sql("english", code));
            assertFalse(msg.contains("english"), "Thiếu thông điệp cho mã " + code);
        }
    }

    @Test
    void uniquePhoneViolation() {
        String raw = "Violation of UNIQUE KEY constraint 'UQ_VISITOR_Phone'. Cannot insert duplicate key.";
        assertEquals("Số điện thoại này đã được dùng cho một khách khác.",
                ErrorTranslator.toMessage(sql(raw, 2627)));
    }

    @Test
    void uniqueIndexViolationWithUnknownName() {
        String raw = "Cannot insert duplicate key row in object with unique index 'IX_SOMETHING'.";
        assertEquals("Dữ liệu bị trùng với bản ghi đã có.", ErrorTranslator.toMessage(sql(raw, 2601)));
    }

    @Test
    void checkConstraintViolation() {
        String raw = "The INSERT statement conflicted with the CHECK constraint \"CK_ROOM_Capacity\".";
        assertEquals("Sức chứa phòng phải lớn hơn 0.", ErrorTranslator.toMessage(sql(raw, 547)));
    }

    @Test
    void deleteBlockedByForeignKey() {
        String raw = "The DELETE statement conflicted with the REFERENCE constraint \"FK_SESSION_ROOM\".";
        assertEquals("Không thể xóa vì dữ liệu này đang được sử dụng ở nơi khác.",
                ErrorTranslator.toMessage(sql(raw, 547)));
    }

    @Test
    void sqlExceptionInsideDataAccessExceptionIsFound() {
        DataAccessException wrapped = new DataAccessException("x", sql("e", 50012));
        assertEquals("Lớp đã đủ người, không thể đăng ký thêm.", ErrorTranslator.toMessage(wrapped));
    }

    @Test
    void businessAndIllegalArgumentKeepOwnMessage() {
        assertEquals("Thiếu ảnh", ErrorTranslator.toMessage(new BusinessException("Thiếu ảnh")));
        assertEquals("Tên không được để trống.",
                ErrorTranslator.toMessage(new IllegalArgumentException("Tên không được để trống.")));
    }

    @Test
    void nullAndUnknownErrorsNeverReturnNull() {
        assertFalse(ErrorTranslator.toMessage(null).isBlank());
        assertTrue(ErrorTranslator.toMessage(new RuntimeException("boom")).contains("boom"));
    }
}
