package com.gym.core.model;

/** Enum MembershipStatus, khớp CHECK constraint trong DB. */
public enum MembershipStatus implements DbEnum {
    ACTIVE("Active", "Đang hiệu lực", "#2E7D32"),
    EXPIRED("Expired", "Hết hạn", "#757575"),
    CANCELLED("Cancelled", "Đã hủy", "#C62828");

    private final String dbValue;
    private final String label;
    private final String colorHex;

    MembershipStatus(String dbValue, String label, String colorHex) {
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

    public static MembershipStatus fromDb(String dbValue) {
        return DbEnum.fromDb(MembershipStatus.class, dbValue);
    }
}
