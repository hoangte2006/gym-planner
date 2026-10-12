package com.gym.core.exception;

/**
 * Lỗi truy cập dữ liệu (unchecked). Bọc SQLException để tầng trên không phải khai báo throws.
 * Thông điệp tiếng Việt cho người dùng do ErrorTranslator sinh ra từ nguyên nhân gốc.
 */
public class DataAccessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
