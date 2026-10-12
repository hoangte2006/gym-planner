package com.gym.core.model;

/** Enum AttendanceStatus, khớp CHECK constraint trong DB. */
public enum AttendanceStatus implements DbEnum {
    CONFIRMED("Confirmed", "Đã xác nhận", "#1565C0"),
    ATTENDED("Attended", "Đã tham gia", "#2E7D32"),
    CANCELLED("Cancelled", "Đã hủy", "#C62828");

    private final String dbValue;
    private final String label;
    private final String colorHex;

    AttendanceStatus(String dbValue, String label, String colorHex) {
        this.dbValue = dbValue;
        this.label = label;
        this.colorHex = colorHex;
    }

    @Override
    public String getDbValue() {
        return dbValue;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getColorHex() {
        return colorHex;
    }

    public static AttendanceStatus fromDb(String dbValue) {
        return DbEnum.fromDb(AttendanceStatus.class, dbValue);
    }
}
