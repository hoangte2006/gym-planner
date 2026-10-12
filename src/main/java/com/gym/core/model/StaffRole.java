package com.gym.core.model;

/** Vai trò nhân viên, khớp CHECK constraint CK_STAFF_Role của bảng STAFF. */
public enum StaffRole {
    MANAGER("Manager", "Quản lý"),
    RECEPTIONIST("Receptionist", "Lễ tân"),
    TRAINER("Trainer", "Huấn luyện viên");

    private final String dbValue;
    private final String label;

    StaffRole(String dbValue, String label) {
        this.dbValue = dbValue;
        this.label = label;
    }

    /** Giá trị lưu trong cột STAFF.Role. */
    public String getDbValue() {
        return dbValue;
    }

    /** Nhãn tiếng Việt hiển thị trên giao diện. */
    public String getLabel() {
        return label;
    }

    public static StaffRole fromDb(String value) {
        for (StaffRole r : values()) {
            if (r.dbValue.equalsIgnoreCase(value)) {
                return r;
            }
        }
        throw new IllegalArgumentException("Vai trò không hợp lệ: " + value);
    }
}
