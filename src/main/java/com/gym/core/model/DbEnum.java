package com.gym.core.model;

/**
 * Chung cho mọi enum trạng thái: có giá trị lưu trong DB, nhãn tiếng Việt và màu hiển thị.
 */
public interface DbEnum {

    /** Giá trị lưu trong cột của bảng (khớp CHECK constraint). */
    String getDbValue();

    /** Nhãn tiếng Việt hiển thị trên giao diện. */
    String getLabel();

    /** Màu hiển thị dạng #RRGGBB. */
    String getColorHex();

    /** Tìm hằng enum theo giá trị DB (không phân biệt hoa thường). */
    static <E extends Enum<E> & DbEnum> E fromDb(Class<E> type, String dbValue) {
        for (E e : type.getEnumConstants()) {
            if (e.getDbValue().equalsIgnoreCase(dbValue)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Giá trị " + type.getSimpleName() + " không hợp lệ: " + dbValue);
    }
}
