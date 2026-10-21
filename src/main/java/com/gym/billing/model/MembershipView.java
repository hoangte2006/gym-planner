package com.gym.billing.model;

import com.gym.core.model.MembershipStatus;
import java.time.LocalDate;

/** Bản ghi gói đã đăng ký, ghép thông tin hội viên và gói. */
public record MembershipView(int membershipId, int visitorId, String memberName, String phone,
                             String planName, LocalDate startDate, LocalDate endDate,
                             MembershipStatus status) { }
