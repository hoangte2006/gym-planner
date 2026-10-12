package com.gym.core.model;

/** Lễ tân (bảng RECEPTIONIST, con của STAFF). */
public class Receptionist extends Staff {

    private ShiftType shiftType;

    /** Ca làm. */
    public ShiftType getShiftType() {
        return shiftType;
    }

    public void setShiftType(ShiftType shiftType) {
        this.shiftType = shiftType;
    }

    @Override
    public String getRoleName() {
        return StaffRole.RECEPTIONIST.getLabel();
    }
}
