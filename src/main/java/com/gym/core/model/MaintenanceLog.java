package com.gym.core.model;

import com.gym.core.util.Check;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Nhật ký bảo trì (bảng MAINTENANCE_LOG, khóa gồm Equipment_ID + Log_no). */
public class MaintenanceLog {

    private Integer equipmentId;
    private Integer logNo;
    private LocalDate maintenanceDate;
    private BigDecimal cost;
    private String description;
    private Integer loggedBy;

    /** Mã thiết bị. */
    public Integer getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(Integer equipmentId) {
        this.equipmentId = equipmentId;
    }

    /** Số thứ tự. */
    public Integer getLogNo() {
        return logNo;
    }

    public void setLogNo(Integer logNo) {
        this.logNo = logNo;
    }

    /** Ngày bảo trì. */
    public LocalDate getMaintenanceDate() {
        return maintenanceDate;
    }

    public void setMaintenanceDate(LocalDate maintenanceDate) {
        this.maintenanceDate = maintenanceDate;
    }

    /** Chi phí. */
    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = Check.nonNegative(cost, "Chi phí");
    }

    /** Mô tả. */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = Check.text(description, "Mô tả");
    }

    /** Mã nhân viên ghi. */
    public Integer getLoggedBy() {
        return loggedBy;
    }

    public void setLoggedBy(Integer loggedBy) {
        this.loggedBy = loggedBy;
    }
}
