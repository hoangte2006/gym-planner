package com.gym.core.model;

import java.time.LocalDateTime;

/** Đặt thiết bị cho một buổi tập (bảng SESSION_EQUIPMENT_USAGE). */
public class SessionEquipmentUsage {

    private Integer usageId;
    private Integer equipmentId;
    private Integer sessionId;
    private LocalDateTime reservedFrom;
    private LocalDateTime reservedTo;

    /** Mã đặt. */
    public Integer getUsageId() {
        return usageId;
    }

    public void setUsageId(Integer usageId) {
        this.usageId = usageId;
    }

    /** Mã thiết bị. */
    public Integer getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(Integer equipmentId) {
        this.equipmentId = equipmentId;
    }

    /** Mã buổi. */
    public Integer getSessionId() {
        return sessionId;
    }

    public void setSessionId(Integer sessionId) {
        this.sessionId = sessionId;
    }

    /** Từ giờ. */
    public LocalDateTime getReservedFrom() {
        return reservedFrom;
    }

    public void setReservedFrom(LocalDateTime reservedFrom) {
        this.reservedFrom = reservedFrom;
    }

    /** Đến giờ. */
    public LocalDateTime getReservedTo() {
        return reservedTo;
    }

    public void setReservedTo(LocalDateTime reservedTo) {
        this.reservedTo = reservedTo;
    }
}
