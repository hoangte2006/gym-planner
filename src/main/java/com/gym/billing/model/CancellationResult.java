package com.gym.billing.model;

import java.util.List;

/** Kết quả hủy gói và danh sách đăng ký lớp tương lai không tự động bị hủy. */
public record CancellationResult(int membershipId, int visitorId, String memberName,
                                String phone, List<ClassImpact> futureClasses) {
    public CancellationResult {
        futureClasses = futureClasses == null ? List.of() : List.copyOf(futureClasses);
    }
}
