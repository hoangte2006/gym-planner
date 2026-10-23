package com.gym.dashboard.model;

import java.math.BigDecimal;
import java.time.YearMonth;

/** Tổng chi phí bảo trì thiết bị của một tháng: một cột của biểu đồ "chi phí bảo trì theo tháng". */
public record MonthlyMaintenanceCost(YearMonth month, BigDecimal cost) {
}

// record là một loại class đặc biệt trong Java 16 trở lên, được sử dụng để định nghĩa các lớp dữ liệu bất biến (immutable data classes). Nó tự động tạo ra các phương thức như constructor, getter, equals(), hashCode(), và toString() dựa trên các trường dữ liệu được khai báo trong record.