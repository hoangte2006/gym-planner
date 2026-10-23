package com.gym.dashboard.model;

import java.time.LocalDate;

/**
 * Mức lấp đầy các lớp học trong MỘT ngày: tổng số chỗ đã đăng ký (không tính đăng ký đã hủy)
 * trên tổng sức chứa của các lớp diễn ra ngày đó.
 */
public record ClassOccupancy(LocalDate date, int seatsTaken, int capacity) {

    /** Tỷ lệ lấp đầy theo phần trăm (0 đến 100). Ngày không có lớp nào (sức chứa 0) tính là 0. */
    public double percent() {
        return capacity == 0 ? 0.0 : 100.0 * seatsTaken / capacity;
    }
}
