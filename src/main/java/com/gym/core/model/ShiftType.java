package com.gym.core.model;

/** Enum ShiftType, khớp CHECK constraint trong DB. */
public enum ShiftType implements DbEnum {
    MORNING("Morning", "Ca sáng", "#F9A825"),
    EVENING("Evening", "Ca chiều", "#EF6C00"),
    NIGHT("Night", "Ca tối", "#283593");

    private final String dbValue;
    private final String label;
    private final String colorHex;

    ShiftType(String dbValue, String label, String colorHex) {
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

    public static ShiftType fromDb(String dbValue) {
        return DbEnum.fromDb(ShiftType.class, dbValue);
    }
}
