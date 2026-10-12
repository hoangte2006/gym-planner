package com.gym.core.model;

/** Enum BillingStatus, khớp CHECK constraint trong DB. */
public enum BillingStatus implements DbEnum {
    PENDING("Pending", "Chờ thanh toán", "#EF6C00"),
    PAID("Paid", "Đã thanh toán", "#2E7D32"),
    FAILED("Failed", "Thất bại", "#C62828");

    private final String dbValue;
    private final String label;
    private final String colorHex;

    BillingStatus(String dbValue, String label, String colorHex) {
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

    public static BillingStatus fromDb(String dbValue) {
        return DbEnum.fromDb(BillingStatus.class, dbValue);
    }
}
