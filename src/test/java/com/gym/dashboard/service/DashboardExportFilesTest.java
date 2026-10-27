package com.gym.dashboard.service;

import com.gym.dashboard.model.BusiestClass;
import com.gym.dashboard.model.ClassOccupancy;
import com.gym.dashboard.model.DashboardReport;
import com.gym.dashboard.model.DateRange;
import com.gym.dashboard.model.EquipmentStatusCount;
import com.gym.dashboard.model.MonthlyMaintenanceCost;
import com.gym.dashboard.model.MonthlyRevenue;
import com.gym.dashboard.model.TrainerSessionCount;
import com.gym.core.model.EquipmentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Test thuần (không cần DB) cho việc ghi báo cáo Dashboard ra tệp CSV và PDF. */
class DashboardExportFilesTest {

    private static DashboardReport report(boolean finance) {
        LocalDate monday = LocalDate.of(2026, 10, 19);
        return new DashboardReport(
                LocalDateTime.of(2026, 10, 22, 16, 30),
                finance,
                new DateRange(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 10, 22)),
                7, 1218, 5, 9,
                finance ? new BigDecimal("2245.50") : BigDecimal.ZERO,
                finance ? List.of(new MonthlyRevenue(YearMonth.of(2026, 9), new BigDecimal("49700.00")),
                        new MonthlyRevenue(YearMonth.of(2026, 10), new BigDecimal("2245.50"))) : List.of(),
                finance ? List.of(new MonthlyMaintenanceCost(YearMonth.of(2026, 9), new BigDecimal("810.00"))) : List.of(),
                finance ? List.of(new TrainerSessionCount("Nguyễn Văn An", 14), new TrainerSessionCount("Lê, Thị \"Bé\"", 9)) : List.of(),
                finance ? List.of(new BusiestClass("Zumba", LocalDateTime.of(2026, 10, 19, 18, 0), 20, 20)) : List.of(),
                List.of(new ClassOccupancy(monday, 6, 10), new ClassOccupancy(monday.plusDays(1), 0, 0)),
                List.of(new EquipmentStatusCount(EquipmentStatus.ACTIVE, 88), new EquipmentStatusCount(EquipmentStatus.BROKEN, 6)));
    }

    @Test
    void csvFileStartsWithUtf8BomSoExcelReadsVietnameseCorrectly(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("report.csv");
        DashboardCsvWriter.write(report(true), file);

        byte[] bytes = Files.readAllBytes(file);
        assertEquals((byte) 0xEF, bytes[0]);                        // BOM UTF-8 = EF BB BF
        assertEquals((byte) 0xBB, bytes[1]);
        assertEquals((byte) 0xBF, bytes[2]);

        String text = new String(bytes, StandardCharsets.UTF_8);
        assertTrue(text.contains("Hội viên đang hoạt động,1218"), "Tiếng Việt phải còn nguyên dấu");
        assertTrue(text.contains("Hoạt động,88"), "Nhãn trạng thái thiết bị bằng tiếng Việt");
        assertTrue(text.contains("09/2026,49700.00"), "Số tiền ghi dạng thuần, dấu chấm thập phân, không có dấu phân cách nghìn");
        assertTrue(text.contains("19/10/2026,6,10,60.0"), "Tỷ lệ lấp đầy 6/10 = 60.0");
        assertTrue(text.contains("1,Nguyễn Văn An,14"), "Top huấn luyện viên: hạng 1");
        assertTrue(text.contains("2,\"Lê, Thị \"\"Bé\"\"\",9"), "Tên có dấu phẩy và nháy kép phải được bọc đúng");
        assertTrue(text.contains("1,Zumba,19/10/2026 18:00,20,20"), "Lớp đông nhất: hạng, tên, thời gian, đăng ký, sức chứa");
        assertTrue(text.contains("20/10/2026,0,0,0.0"), "Ngày không có lớp: 0/0 và 0.0, không chia cho 0");
        assertTrue(text.contains("\r\n"), "Dòng kết thúc bằng CRLF");
    }

    @Test
    void csvForReceptionistHasNoFinanceSections() {
        String text = DashboardCsvWriter.toCsv(report(false));
        assertFalse(text.contains("Doanh thu"), "Lễ tân không được có doanh thu trong tệp");
        assertFalse(text.contains("Chi phí"), "Lễ tân không được có chi phí bảo trì trong tệp");
        assertFalse(text.contains("Top huấn luyện viên"), "Lễ tân không được có bảng xếp hạng trong tệp");
        assertFalse(text.contains("Lớp học đông nhất"), "Lễ tân không được có bảng xếp hạng trong tệp");
        assertTrue(text.contains("Hội viên đang hoạt động,1218"), "Số liệu cơ bản vẫn có");
        assertTrue(text.contains("Thiết bị theo trạng thái"));
    }

    @Test
    void csvEscapesCommasQuotesAndLineBreaks() {
        assertEquals("abc", DashboardCsvWriter.escape("abc"));
        assertEquals("\"a,b\"", DashboardCsvWriter.escape("a,b"));
        assertEquals("\"say \"\"hi\"\"\"", DashboardCsvWriter.escape("say \"hi\""));
        assertEquals("\"line1\nline2\"", DashboardCsvWriter.escape("line1\nline2"));
        assertEquals("", DashboardCsvWriter.escape(null));
    }

    @Test
    void pdfFileIsAValidPdf(@TempDir Path dir) throws Exception {
        Path manager = dir.resolve("manager.pdf");
        Path receptionist = dir.resolve("receptionist.pdf");
        DashboardPdfWriter.write(report(true), manager);
        DashboardPdfWriter.write(report(false), receptionist);

        for (Path f : List.of(manager, receptionist)) {
            byte[] bytes = Files.readAllBytes(f);
            assertTrue(bytes.length > 1000, "Tệp PDF không được rỗng: " + f);
            assertEquals("%PDF", new String(bytes, 0, 4, StandardCharsets.US_ASCII), "Đầu tệp phải là %PDF");
        }
        assertTrue(Files.size(manager) > Files.size(receptionist), "Bản của Quản lý có thêm phần tài chính nên lớn hơn");
    }
}
