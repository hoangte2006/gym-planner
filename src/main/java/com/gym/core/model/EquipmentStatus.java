package com.gym.core.model;

/** Enum EquipmentStatus, khớp CHECK constraint trong DB. */
public enum EquipmentStatus implements DbEnum {
    ACTIVE("Active", "Hoạt động", "#2E7D32"),
    UNDER_MAINTENANCE("Under Maintenance", "Đang bảo trì", "#EF6C00"),
    BROKEN("Broken", "Hỏng", "#C62828");

    private final String dbValue;
    private final String label;
    private final String colorHex;

    EquipmentStatus(String dbValue, String label, String colorHex) {
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

    public static EquipmentStatus fromDb(String dbValue) {
        return DbEnum.fromDb(EquipmentStatus.class, dbValue);
    }
}
