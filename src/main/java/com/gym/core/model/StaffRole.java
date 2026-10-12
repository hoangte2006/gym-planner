package com.gym.core.model;

/** Vai trò nhân viên, khớp CHECK constraint CK_STAFF_Role của bảng STAFF. */
public enum StaffRole implements DbEnum {
    MANAGER("Manager", "Quản lý", "#6A1B9A"),
    RECEPTIONIST("Receptionist", "Lễ tân", "#1565C0"),
    TRAINER("Trainer", "Huấn luyện viên", "#2E7D32");

    private final String dbValue;
    private final String label;
    private final String colorHex;

    StaffRole(String dbValue, String label, String colorHex) {
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

    public static StaffRole fromDb(String dbValue) {
        return DbEnum.fromDb(StaffRole.class, dbValue);
    }
}
