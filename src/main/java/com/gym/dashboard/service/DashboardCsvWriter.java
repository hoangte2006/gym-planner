package com.gym.dashboard.service;

import com.gym.core.DateTimeUtils;
import com.gym.dashboard.model.BusiestClass;
import com.gym.dashboard.model.ClassOccupancy;
import com.gym.dashboard.model.DashboardReport;
import com.gym.dashboard.model.EquipmentStatusCount;
import com.gym.dashboard.model.MonthlyMaintenanceCost;
import com.gym.dashboard.model.MonthlyRevenue;
import com.gym.dashboard.model.TrainerSessionCount;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Ghi báo cáo Dashboard ra CSV. Tệp bắt đầu bằng BOM UTF-8 (3 byte EF BB BF) để Excel nhận ra UTF-8
 * và hiện đúng tiếng Việt; dòng kết thúc bằng CRLF; ô có dấu phẩy, nháy kép hoặc xuống dòng được bọc nháy kép.
 * Số tiền ghi bằng dấu chấm thập phân (không có dấu phân cách hàng nghìn) để máy đọc lại được.
 * Lớp thuần: không đụng DB hay giao diện.
 */
public final class DashboardCsvWriter {

    static final String BOM = "﻿";
    private static final String EOL = "\r\n";
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MM/yyyy");

    private DashboardCsvWriter() {
    }

    public static void write(DashboardReport report, Path output) throws IOException {
        Files.writeString(output, toCsv(report), StandardCharsets.UTF_8);    // writeString không tự thêm BOM nên BOM nằm trong chuỗi
    }

    static String toCsv(DashboardReport r) {
        StringBuilder sb = new StringBuilder(BOM);
        row(sb, "Báo cáo Dashboard Gym Planner");
        row(sb, "Thời điểm xuất", DateTimeUtils.format(r.generatedAt()));
        blank(sb);

        row(sb, "Chỉ số", "Giá trị");
        row(sb, "Hội viên đang hoạt động", String.valueOf(r.activeMembers()));
        row(sb, "Gói hết hạn trong " + r.expiringDays() + " ngày", String.valueOf(r.expiringMemberships()));
        row(sb, "Lớp học hôm nay", String.valueOf(r.classToday()));
        if (r.includesManagerData()) {
            row(sb, "Doanh thu tháng này", money(r.monthlyRevenue()));
        }

        if (r.includesManagerData()) {
            blank(sb);
            row(sb, "Khoảng thời gian lọc", DateTimeUtils.format(r.range().from()) + " - " + DateTimeUtils.format(r.range().to()));
            blank(sb);
            row(sb, "Doanh thu theo tháng");
            row(sb, "Tháng", "Doanh thu");
            for (MonthlyRevenue m : r.revenueByMonth()) {
                row(sb, MONTH.format(m.month()), money(m.revenue()));
            }
            blank(sb);
            row(sb, "Chi phí bảo trì theo tháng");
            row(sb, "Tháng", "Chi phí");
            for (MonthlyMaintenanceCost m : r.maintenanceByMonth()) {
                row(sb, MONTH.format(m.month()), money(m.cost()));
            }
            blank(sb);
            row(sb, "Top huấn luyện viên theo số buổi dạy");
            row(sb, "Hạng", "Huấn luyện viên", "Số buổi");
            int rank = 1;
            for (TrainerSessionCount t : r.topTrainers()) {
                row(sb, String.valueOf(rank++), t.trainerName(), String.valueOf(t.sessions()));
            }
            blank(sb);
            row(sb, "Lớp học đông nhất");
            row(sb, "Hạng", "Lớp", "Thời gian", "Đã đăng ký", "Sức chứa");
            rank = 1;
            for (BusiestClass c : r.busiestClasses()) {
                row(sb, String.valueOf(rank++), c.className(), DateTimeUtils.format(c.start()),
                        String.valueOf(c.registrations()), String.valueOf(c.capacity()));
            }
        }

        blank(sb);
        row(sb, "Tỷ lệ lấp đầy lớp học trong tuần");
        row(sb, "Ngày", "Chỗ đã đăng ký", "Sức chứa", "Tỷ lệ (%)");
        for (ClassOccupancy o : r.occupancyThisWeek()) {
            row(sb, DateTimeUtils.format(o.date()), String.valueOf(o.seatsTaken()), String.valueOf(o.capacity()),
                    String.format(Locale.ROOT, "%.1f", o.percent()));
        }

        blank(sb);
        row(sb, "Thiết bị theo trạng thái");
        row(sb, "Trạng thái", "Số lượng");
        List<EquipmentStatusCount> equipment = r.equipmentByStatus();
        for (EquipmentStatusCount e : equipment) {
            row(sb, e.status().getLabel(), String.valueOf(e.count()));
        }
        return sb.toString();
    }

    private static String money(BigDecimal amount) {
        return (amount == null ? BigDecimal.ZERO : amount).toPlainString();
    }

    private static void blank(StringBuilder sb) {
        sb.append(EOL);
    }

    private static void row(StringBuilder sb, String... cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(cells[i]));
        }
        sb.append(EOL);
    }

    /** Bọc nháy kép khi ô có dấu phẩy, nháy kép hoặc xuống dòng; nháy kép trong ô được nhân đôi. */
    static String escape(String cell) {
        if (cell == null) {
            return "";
        }
        if (cell.contains(",") || cell.contains("\"") || cell.contains("\n") || cell.contains("\r")) {
            return '"' + cell.replace("\"", "\"\"") + '"';
        }
        return cell;
    }
}
