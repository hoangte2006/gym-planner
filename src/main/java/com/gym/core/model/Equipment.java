package com.gym.core.model;

import com.gym.core.util.Check;
import java.time.LocalDate;

/** Thiết bị (bảng EQUIPMENT). */
public class Equipment {

    private Integer equipmentId;
    private String equipmentName;
    private Integer roomId;
    private EquipmentStatus operationalStatus;
    private LocalDate purchaseDate;

    /** Mã thiết bị. */
    public Integer getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(Integer equipmentId) {
        this.equipmentId = equipmentId;
    }

    /** Tên thiết bị. */
    public String getEquipmentName() {
        return equipmentName;
    }

    public void setEquipmentName(String equipmentName) {
        this.equipmentName = Check.text(equipmentName, "Tên thiết bị");
    }

    /** Mã phòng (có thể trống). */
    public Integer getRoomId() {
        return roomId;
    }

    public void setRoomId(Integer roomId) {
        this.roomId = roomId;
    }

    /** Tình trạng. */
    public EquipmentStatus getOperationalStatus() {
        return operationalStatus;
    }

    public void setOperationalStatus(EquipmentStatus operationalStatus) {
        this.operationalStatus = operationalStatus;
    }

    /** Ngày mua. */
    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }
}
