package com.gym.dashboard.service;

import com.gym.app.Session;
import com.gym.core.DBConnection;
import com.gym.core.exception.BusinessException;
import com.gym.core.model.Manager;
import com.gym.core.model.Receptionist;
import com.gym.core.model.Staff;
import com.gym.core.model.StaffRole;
import com.gym.core.model.Trainer;
import com.gym.dashboard.model.DateRange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Xuất báo cáo Dashboard với DB thật: kiểm tra phân quyền (Quản lý có doanh thu, Lễ tân không) và lỗi đầu vào. */
class DashboardExportTest {

    private final DashboardService service = new DashboardService();
    private final DateRange range = DateRange.lastMonths(LocalDate.now(), 3);

    @BeforeAll
    static void requireDatabase() {
        boolean up;
        try {
            DBConnection.test();
            up = true;
        } catch (RuntimeException e) {
            up = false;
        }
        assumeTrue(up, "Không kết nối được DB, bỏ qua kiểm thử xuất báo cáo.");
    }

    @AfterEach
    void logout() {
        Session.logout();       // Session là biến toàn cục: trả về chưa đăng nhập cho test khác
    }

    private static void loginAs(StaffRole role) {
        Staff staff = switch (role) {
            case MANAGER -> new Manager();
            case RECEPTIONIST -> new Receptionist();
            case TRAINER -> new Trainer();
        };
        staff.setFullName("Test " + role.getLabel());
        staff.setPhone("0900000000");
        staff.setSalary(new BigDecimal("100"));
        staff.setRole(role);
        Session.login(staff);
    }

    private static String read(Path file) throws Exception {
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    @Test
    void managerCsvContainsRevenueAndMaintenanceCost(@TempDir Path dir) throws Exception {
        loginAs(StaffRole.MANAGER);
        Path file = dir.resolve("m.csv");
        service.exportCsv(range, 7, file);
        String text = read(file);
        assertTrue(text.startsWith("﻿"), "Tệp CSV phải có BOM");
        assertTrue(text.contains("Doanh thu tháng này"));
        assertTrue(text.contains("Chi phí bảo trì theo tháng"));
        assertTrue(text.contains("Top huấn luyện viên theo số buổi dạy"));
        assertTrue(text.contains("Lớp học đông nhất"));
        assertTrue(text.contains("Hội viên đang hoạt động"));
    }

    @Test
    void receptionistCsvHasNoRevenueNorMaintenanceCost(@TempDir Path dir) throws Exception {
        loginAs(StaffRole.RECEPTIONIST);
        Path file = dir.resolve("r.csv");
        service.exportCsv(range, 7, file);
        String text = read(file);
        assertFalse(text.contains("Doanh thu"), "Lễ tân không được thấy doanh thu trong tệp xuất");
        assertFalse(text.contains("Chi phí"), "Lễ tân không được thấy chi phí trong tệp xuất");
        assertFalse(text.contains("Top huấn luyện viên"), "Lễ tân không được thấy bảng xếp hạng trong tệp xuất");
        assertTrue(text.contains("Hội viên đang hoạt động"));
    }

    @Test
    void pdfExportWorksForBothRoles(@TempDir Path dir) throws Exception {
        for (StaffRole role : new StaffRole[]{StaffRole.MANAGER, StaffRole.RECEPTIONIST}) {
            loginAs(role);
            Path file = dir.resolve(role.name() + ".pdf");
            service.exportPdf(range, 30, file);
            assertEquals("%PDF", new String(Files.readAllBytes(file), 0, 4, StandardCharsets.US_ASCII), role.name());
        }
    }

    @Test
    void trainerAndLoggedOutUsersAreRefused(@TempDir Path dir) {
        loginAs(StaffRole.TRAINER);
        assertThrows(BusinessException.class, () -> service.exportCsv(range, 7, dir.resolve("t.csv")));
        Session.logout();
        assertThrows(BusinessException.class, () -> service.exportCsv(range, 7, dir.resolve("x.csv")));
    }

    @Test
    void badInputsGiveFriendlyErrors(@TempDir Path dir) {
        loginAs(StaffRole.MANAGER);
        assertThrows(BusinessException.class, () -> service.exportCsv(range, 8, dir.resolve("a.csv")), "mốc ngày lạ");
        assertThrows(BusinessException.class, () -> service.exportCsv(null, 7, dir.resolve("b.csv")), "thiếu khoảng");
        assertThrows(BusinessException.class, () -> service.exportCsv(range, 7, null), "thiếu đường dẫn");
        assertThrows(BusinessException.class,
                () -> service.exportCsv(DateRange.lastMonths(LocalDate.now(), 40), 7, dir.resolve("c.csv")), "khoảng quá dài");
    }

    @Test
    void exportCreatesMissingFolders(@TempDir Path dir) throws Exception {
        loginAs(StaffRole.MANAGER);
        Path file = dir.resolve("sub").resolve("deeper").resolve("r.csv");
        service.exportCsv(range, 7, file);
        assertTrue(Files.isRegularFile(file));
    }
}
