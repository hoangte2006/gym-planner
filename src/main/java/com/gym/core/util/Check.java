package com.gym.core.util;

import java.math.BigDecimal;

/**
 * Kiểm tra giá trị trong setter của model. Sai thì ném IllegalArgumentException
 * với thông điệp tiếng Việt, tầng trên đổi thành thông báo cho người dùng.
 */
public final class Check {

    private Check() {
    }

    /** Chuỗi không được rỗng; trả về chuỗi đã cắt khoảng trắng hai đầu. */
    public static String text(String value, String fieldLabel) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldLabel + " không được để trống.");
        }
        return value.trim();
    }

    public static String email(String value, String fieldLabel) {
        String v = text(value, fieldLabel);
        if (!v.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException(fieldLabel + " không đúng định dạng (ví dụ: ten@gmail.com).");
        }
        return v;
    }

    public static int positive(int value, String fieldLabel) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldLabel + " phải lớn hơn 0.");
        }
        return value;
    }

    public static BigDecimal positive(BigDecimal value, String fieldLabel) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException(fieldLabel + " phải lớn hơn 0.");
        }
        return value;
    }

    public static BigDecimal nonNegative(BigDecimal value, String fieldLabel) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(fieldLabel + " không được âm.");
        }
        return value;
    }
}
