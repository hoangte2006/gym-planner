package com.gym.core.exception;

/**
 * Lỗi vi phạm luật nghiệp vụ (unchecked). Thông điệp là tiếng Việt, hiển thị thẳng cho người dùng.
 * Tầng service ném khi kiểm tra trước khi ghi xuống DB.
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
