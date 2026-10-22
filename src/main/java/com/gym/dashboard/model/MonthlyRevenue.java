package com.gym.dashboard.model;

import java.math.BigDecimal;
import java.time.YearMonth;

/** Doanh thu của một tháng (tổng tiền hóa đơn Paid): một cột của biểu đồ "doanh thu theo tháng". */
public record MonthlyRevenue(YearMonth month, BigDecimal revenue) {
}
