package com.gym.billing.model;

import java.time.LocalDate;

/** Hội viên có gói Active sắp hết hạn. daysLeft=0 nghĩa là hết hạn hôm nay. */
public record ExpiringMembershipView(int membershipId, int visitorId, String memberName,
                                     String phone, String planName, LocalDate endDate, long daysLeft) { }
