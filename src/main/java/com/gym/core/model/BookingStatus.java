package com.gym.core.model;

/** Enum BookingStatus, khớp CHECK constraint trong DB. */
public enum BookingStatus implements DbEnum {
    SCHEDULED("Scheduled", "Đã đặt lịch", "#1565C0"),
    COMPLETED("Completed", "Hoàn thành", "#2E7D32"),
    CANCELLED("Cancelled", "Đã hủy", "#C62828");

    private final String dbValue;
    private final String label;
    private final String colorHex;

    BookingStatus(String dbValue, String label, String colorHex) {
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

    public static BookingStatus fromDb(String dbValue) {
        return DbEnum.fromDb(BookingStatus.class, dbValue);
    }
}
