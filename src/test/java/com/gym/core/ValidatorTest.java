package com.gym.core;

import com.gym.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 12);

    @Test
    void phoneRules() {
        assertTrue(Validator.isPhone("0912345678"));
        assertTrue(Validator.isPhone("09123456789"));   // 11 số
        assertFalse(Validator.isPhone("912345678"));    // không bắt đầu bằng 0
        assertFalse(Validator.isPhone("091234567"));    // 9 số
        assertFalse(Validator.isPhone("09123abc78"));
        assertFalse(Validator.isPhone(null));
    }

    @Test
    void emailRules() {
        assertTrue(Validator.isEmail("an.nguyen@gmail.com"));
        assertFalse(Validator.isEmail("an@gmail"));
        assertFalse(Validator.isEmail("an gmail.com"));
        assertFalse(Validator.isEmail(""));
    }

    @Test
    void nameAcceptsVietnameseLetters() {
        assertTrue(Validator.isName("Nguyễn Văn Hoàng"));
        assertTrue(Validator.isName("O'Neil"));
        assertFalse(Validator.isName("An123"));
        assertFalse(Validator.isName("A".repeat(51)));
    }

    @Test
    void collectsAllErrorsAtOnce() {
        Validator v = new Validator()
                .name("Họ", "")
                .phone("Số điện thoại", "123")
                .email("Email", "sai")
                .positive("Giá", BigDecimal.ZERO);
        assertFalse(v.isValid());
        assertEquals(4, v.getErrors().size());
        BusinessException ex = assertThrows(BusinessException.class, v::throwIfInvalid);
        assertTrue(ex.getMessage().contains("Họ không được để trống."));
        assertTrue(ex.getMessage().contains("Số điện thoại phải gồm 10–11 chữ số"));
    }

    @Test
    void validInputPasses() {
        Validator v = new Validator()
                .name("Tên", "Hoàng")
                .phone("SĐT", "0912345678")
                .email("Email", "a@b.com")
                .nonNegative("Giá", BigDecimal.ZERO);
        assertTrue(v.isValid());
        v.throwIfInvalid();   // không ném
    }

    @Test
    void birthDateRules() {
        assertTrue(new Validator().birthDate("Ngày sinh", null, TODAY).isValid());   // không bắt buộc
        assertTrue(new Validator().birthDate("Ngày sinh", LocalDate.of(2000, 5, 1), TODAY).isValid());
        assertFalse(new Validator().birthDate("Ngày sinh", TODAY.plusDays(1), TODAY).isValid());      // tương lai
        assertFalse(new Validator().birthDate("Ngày sinh", LocalDate.of(2024, 1, 1), TODAY).isValid()); // 2 tuổi
        assertFalse(new Validator().birthDate("Ngày sinh", LocalDate.of(1900, 1, 1), TODAY).isValid()); // 126 tuổi
    }

    @Test
    void dateOrderRules() {
        assertTrue(new Validator().dateOrder("Gói", TODAY, TODAY.plusDays(1)).isValid());
        assertFalse(new Validator().dateOrder("Gói", TODAY, TODAY).isValid());
        assertFalse(new Validator().dateOrder("Gói", null, TODAY).isValid());
    }
}
