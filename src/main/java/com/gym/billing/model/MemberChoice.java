package com.gym.billing.model;

import java.time.LocalDate;

/** Dữ liệu gọn để chọn hội viên; không tải VARBINARY ảnh trong danh sách. */
public record MemberChoice(int visitorId, String displayName, String phone, boolean hasPhoto,
                           String activePlanName, LocalDate activeStartDate, LocalDate activeEndDate) {
    @Override
    public String toString() {
        String photo = hasPhoto ? "Có ảnh" : "THIẾU ẢNH";
        String active = activePlanName == null ? "Chưa có gói Active"
                : "Đang dùng " + activePlanName + " đến " + activeEndDate;
        return "#" + visitorId + " · " + displayName + " · " + phone + " · " + photo + " · " + active;
    }
}
