package com.gym.billing.model;

import com.gym.core.model.MembershipStatus;
import java.time.LocalDate;

/** Gói Active hiện tại hoặc gói Expired gần nhất làm mốc gia hạn. */
public record RenewalBase(Integer membershipId, MembershipStatus status, LocalDate endDate) { }
