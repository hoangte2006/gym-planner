package com.gym.app.home;

import com.gym.app.Session;
import com.gym.core.model.Manager;
import com.gym.core.model.Receptionist;
import com.gym.core.model.Staff;
import com.gym.core.model.StaffRole;
import com.gym.core.model.Trainer;
import com.gym.core.ui.Theme;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/** Cấu trúc trang chủ theo vai trò, dòng việc, và cách ghi thứ ngày. */
class HomePanelTest {

    @BeforeAll
    static void requireDisplay() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Cần môi trường có màn hình để dựng thành phần Swing.");
    }

    @AfterEach
    void logout() {
        Session.logout();
    }

    private static HomePanel panelFor(StaffRole role) {
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
        return new HomePanel();
    }

    private static int countRows(Container root) {
        int n = 0;
        for (Component c : root.getComponents()) {
            if (c instanceof HomePanel.TaskRow) {
                n++;
            }
            if (c instanceof Container inner) {
                n += countRows(inner);
            }
        }
        return n;
    }

    private static boolean hasText(Container root, String fragment) {
        for (Component c : root.getComponents()) {
            if (c instanceof JLabel label && label.getText() != null && label.getText().contains(fragment)) {
                return true;
            }
            if (c instanceof Container inner && hasText(inner, fragment)) {
                return true;
            }
        }
        return false;
    }

    @Test
    void managerAndReceptionistSeeThreeTaskRows() {
        assertEquals(3, countRows(panelFor(StaffRole.MANAGER)));
        assertEquals(3, countRows(panelFor(StaffRole.RECEPTIONIST)));
    }

    @Test
    void trainerSeesGreetingAndANoteButNoTaskRows() {
        HomePanel panel = panelFor(StaffRole.TRAINER);
        assertEquals(0, countRows(panel));
        assertTrue(hasText(panel, "Xin chào, Test Huấn luyện viên"), "Có lời chào theo tên");
        assertTrue(hasText(panel, "Lịch tập"), "Có dòng nhắc xem lịch dạy");
    }

    @Test
    void greetingShowsTheLoggedInName() {
        assertTrue(hasText(panelFor(StaffRole.MANAGER), "Xin chào, Test Quản lý"));
    }

    @Test
    void dateLabelNamesTheWeekdayInVietnamese() {
        assertEquals("Thứ Hai, 26/10/2026", HomePanel.dateLabel(LocalDate.of(2026, 10, 26)));
        assertEquals("Thứ Ba, 27/10/2026", HomePanel.dateLabel(LocalDate.of(2026, 10, 27)));
        assertEquals("Chủ nhật, 25/10/2026", HomePanel.dateLabel(LocalDate.of(2026, 10, 25)));
        assertEquals("Thứ Bảy, 24/10/2026", HomePanel.dateLabel(LocalDate.of(2026, 10, 24)));
    }

    @Test
    void taskRowShowsTheNumberAndSwitchesTextBetweenSomeAndNone() {
        HomePanel.TaskRow row = new HomePanel.TaskRow("gói hết hạn", "Không có gói nào", Theme.WARNING);
        assertEquals("—", row.number(), "Chưa có số liệu thì hiện gạch");

        row.setCount(3);
        assertEquals("3", row.number());
        assertEquals("gói hết hạn", row.text());

        row.setCount(0);
        assertEquals("0", row.number());
        assertEquals("Không có gói nào", row.text(), "0 việc thì đổi sang câu 'không có'");

        row.clear();
        assertEquals("—", row.number(), "Làm mới thì về gạch trong lúc nạp");
    }
}
