package com.gym.core.model;

/** Enum SessionType, khớp CHECK constraint trong DB. */
public enum SessionType implements DbEnum {
    CLASS("Class", "Lớp tập", "#6A1B9A"),
    PT("PT", "Huấn luyện cá nhân", "#00838F");

    private final String dbValue;
    private final String label;
    private final String colorHex;

    SessionType(String dbValue, String label, String colorHex) {
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

    public static SessionType fromDb(String dbValue) {
        return DbEnum.fromDb(SessionType.class, dbValue);
    }
}
