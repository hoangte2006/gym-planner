package com.gym.facility.service;

import com.gym.core.model.Equipment;
import com.gym.core.model.Session;

import java.util.List;

/**
 * Kết quả đổi trạng thái thiết bị.
 *
 * Bao gồm:
 * - Thiết bị sau khi đổi trạng thái.
 * - Danh sách buổi tập bị ảnh hưởng.
 */
public record EquipmentStatusChangeResult(
        Equipment equipment,
        List<Session> affectedSessions
) {

    public EquipmentStatusChangeResult {
        affectedSessions = affectedSessions == null
                ? List.of()
                : List.copyOf(affectedSessions);
    }
}