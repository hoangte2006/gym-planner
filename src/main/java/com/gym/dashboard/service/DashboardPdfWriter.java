package com.gym.dashboard.service;

import com.gym.core.DateTimeUtils;
import com.gym.dashboard.model.BusiestClass;
import com.gym.dashboard.model.ClassOccupancy;
import com.gym.dashboard.model.DashboardReport;
import com.gym.dashboard.model.EquipmentStatusCount;
import com.gym.dashboard.model.MonthlyMaintenanceCost;
import com.gym.dashboard.model.MonthlyRevenue;
import com.gym.dashboard.model.TrainerSessionCount;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Ghi báo cáo Dashboard ra PDF (OpenPDF). Ưu tiên font Arial của Windows để nhúng đủ dấu tiếng Việt
 * (cùng cách với xuất hóa đơn của module billing). Không đụng DB hay giao diện.
 */
public final class DashboardPdfWriter {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MM/yyyy");
    private static final Color HEADER_BACKGROUND = new Color(0xE3, 0xE8, 0xEF);

    private DashboardPdfWriter() {
    }

    public static void write(DashboardReport r, Path output) throws Exception {
        Document document = new Document(PageSize.A4, 42, 42, 46, 42);
        try (OutputStream stream = Files.newOutputStream(output)) {
            PdfWriter.getInstance(document, stream);
            document.open();
            BaseFont base = loadVietnameseFont();
            Font title = new Font(base, 18, Font.BOLD);
            Font heading = new Font(base, 12, Font.BOLD);
            Font body = new Font(base, 10, Font.NORMAL);
            Font bold = new Font(base, 10, Font.BOLD);

            Paragraph head = new Paragraph("BÁO CÁO DASHBOARD GYM PLANNER", title);
            head.setAlignment(Element.ALIGN_CENTER);
            document.add(head);
            Paragraph when = new Paragraph("Thời điểm xuất: " + DateTimeUtils.format(r.generatedAt()), body);
            when.setAlignment(Element.ALIGN_CENTER);
            when.setSpacingAfter(14f);
            document.add(when);

            PdfPTable summary = table("Chỉ số chính", heading, new float[]{3f, 1.4f});
            header(summary, bold, "Chỉ số", "Giá trị");
            row(summary, body, "Hội viên đang hoạt động", String.valueOf(r.activeMembers()));
            row(summary, body, "Gói hết hạn trong " + r.expiringDays() + " ngày", String.valueOf(r.expiringMemberships()));
            row(summary, body, "Lớp học hôm nay", String.valueOf(r.classToday()));
            if (r.includesManagerData()) {
                row(summary, body, "Doanh thu tháng này", money(r.monthlyRevenue()));
            }
            document.add(summary);

            if (r.includesManagerData()) {
                String range = DateTimeUtils.format(r.range().from()) + " - " + DateTimeUtils.format(r.range().to());
                PdfPTable revenue = table("Doanh thu theo tháng (" + range + ")", heading, new float[]{1.5f, 2f});
                header(revenue, bold, "Tháng", "Doanh thu");
                for (MonthlyRevenue m : r.revenueByMonth()) {
                    row(revenue, body, MONTH.format(m.month()), money(m.revenue()));
                }
                document.add(revenue);

                PdfPTable maintenance = table("Chi phí bảo trì theo tháng (" + range + ")", heading, new float[]{1.5f, 2f});
                header(maintenance, bold, "Tháng", "Chi phí");
                for (MonthlyMaintenanceCost m : r.maintenanceByMonth()) {
                    row(maintenance, body, MONTH.format(m.month()), money(m.cost()));
                }
                document.add(maintenance);

                PdfPTable trainers = table("Top huấn luyện viên theo số buổi dạy", heading, new float[]{0.8f, 3f, 1.2f});
                header(trainers, bold, "Hạng", "Huấn luyện viên", "Số buổi");
                int rank = 1;
                for (TrainerSessionCount t : r.topTrainers()) {
                    row(trainers, body, String.valueOf(rank++), t.trainerName(), String.valueOf(t.sessions()));
                }
                document.add(trainers);

                PdfPTable classes = table("Lớp học đông nhất", heading, new float[]{0.8f, 2.2f, 1.8f, 1.2f});
                header(classes, bold, "Hạng", "Lớp", "Thời gian", "Đăng ký / sức chứa");
                rank = 1;
                for (BusiestClass c : r.busiestClasses()) {
                    row(classes, body, String.valueOf(rank++), c.className(), DateTimeUtils.format(c.start()),
                            c.registrations() + " / " + c.capacity());
                }
                document.add(classes);
            }

            PdfPTable occupancy = table("Tỷ lệ lấp đầy lớp học trong tuần", heading, new float[]{1.4f, 1.4f, 1.2f, 1.2f});
            header(occupancy, bold, "Ngày", "Chỗ đã đăng ký", "Sức chứa", "Tỷ lệ (%)");
            for (ClassOccupancy o : r.occupancyThisWeek()) {
                row(occupancy, body, DateTimeUtils.format(o.date()), String.valueOf(o.seatsTaken()),
                        String.valueOf(o.capacity()), String.format(Locale.ROOT, "%.1f", o.percent()));
            }
            document.add(occupancy);

            PdfPTable equipment = table("Thiết bị theo trạng thái", heading, new float[]{2f, 1.2f});
            header(equipment, bold, "Trạng thái", "Số lượng");
            List<EquipmentStatusCount> rows = r.equipmentByStatus();
            for (EquipmentStatusCount e : rows) {
                row(equipment, body, e.status().getLabel(), String.valueOf(e.count()));
            }
            document.add(equipment);
            document.close();
        } catch (Exception e) {
            if (document.isOpen()) {
                document.close();
            }
            throw e;
        }
    }

    /**
     * Bảng có tiêu đề nằm NGAY TRONG bảng (hàng đầu, không viền, gộp hết các cột) và giữ nguyên khối:
     * nếu không đủ chỗ ở cuối trang thì cả tiêu đề lẫn bảng cùng sang trang sau, không để tiêu đề bị bỏ lại một mình.
     */
    private static PdfPTable table(String title, Font heading, float[] widths) {
        PdfPTable t = new PdfPTable(widths);
        t.setWidthPercentage(100);
        t.setKeepTogether(true);
        t.setSpacingBefore(10f);
        PdfPCell titleCell = new PdfPCell(new Phrase(title, heading));
        titleCell.setColspan(widths.length);
        titleCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        titleCell.setPaddingLeft(0f);
        titleCell.setPaddingBottom(4f);
        t.addCell(titleCell);
        return t;
    }

    private static void header(PdfPTable t, Font font, String... labels) {
        for (String label : labels) {
            PdfPCell cell = new PdfPCell(new Phrase(label, font));
            cell.setBackgroundColor(HEADER_BACKGROUND);
            cell.setPadding(4f);
            t.addCell(cell);
        }
    }

    private static void row(PdfPTable t, Font font, String... values) {
        for (String value : values) {
            PdfPCell cell = new PdfPCell(new Phrase(value, font));
            cell.setPadding(3f);
            t.addCell(cell);
        }
    }

    private static String money(BigDecimal amount) {
        return new DecimalFormat("#,##0.00").format(amount == null ? BigDecimal.ZERO : amount);
    }

    private static BaseFont loadVietnameseFont() throws Exception {
        String[] candidates = {
                "C:/Windows/Fonts/arial.ttf",
                "C:/Windows/Fonts/Arial.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                "/usr/share/fonts/truetype/liberation2/LiberationSans-Regular.ttf"
        };
        for (String candidate : candidates) {
            if (Files.isRegularFile(Path.of(candidate))) {
                return BaseFont.createFont(candidate, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            }
        }
        // Dự phòng: vẫn tạo được PDF nhưng font cơ bản có thể thiếu một số dấu tiếng Việt.
        return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
    }
}
