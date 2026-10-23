package com.gym.dashboard.model;

import com.gym.core.DateTimeUtils;
import com.gym.core.exception.BusinessException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

/**
 * Khoảng thời gian lọc của Dashboard, tính từ ngày "from" đến ngày "to", CẢ HAI ĐỀU ĐƯỢC TÍNH (gồm cả ngày cuối).
 * Dùng cho các biểu đồ theo tháng (doanh thu, chi phí bảo trì) và cho tệp xuất.
 */
public record DateRange(LocalDate from, LocalDate to) {

    public DateRange {
        if (from == null || to == null) {
            throw new BusinessException("Hãy chọn đủ ngày bắt đầu và ngày kết thúc.");
        }
        if (from.isAfter(to)) {
            throw new BusinessException("Ngày bắt đầu không được sau ngày kết thúc.");
        }
    }

    /** "months" tháng gần nhất: từ ngày đầu của tháng cũ nhất đến hôm nay (gồm cả tháng này). */
    public static DateRange lastMonths(LocalDate today, int months) {
        return new DateRange(DateTimeUtils.startOfMonth(today).minusMonths(months - 1L), today);
    }

    /** Từ ngày 1/1 của năm hiện tại đến hôm nay. */
    public static DateRange thisYear(LocalDate today) {
        return new DateRange(today.withDayOfYear(1), today);
    }

    /** Số tháng lịch mà khoảng này đi qua (gồm tháng đầu và tháng cuối), ví dụ 15/01 đến 03/03 là 3 tháng. */
    public int monthCount() {
        return (int) ChronoUnit.MONTHS.between(YearMonth.from(from), YearMonth.from(to)) + 1;
    }
}
