package com.gym.dashboard.model;

import com.gym.core.model.EquipmentStatus;

/** Một dòng kết quả của biểu đồ "thiết bị theo trạng thái": trạng thái và số thiết bị đang ở trạng thái đó. */
public record EquipmentStatusCount(EquipmentStatus status, int count) {
}
