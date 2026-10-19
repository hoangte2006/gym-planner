package com.gym.dashboard.ui;

import com.gym.app.Session;
import com.gym.core.DBConnection;
import com.gym.core.model.Manager;
import com.gym.core.model.Receptionist;
import com.gym.core.model.Staff;
import com.gym.core.model.StaffRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Kiểm tra CẤU TRÚC màn hình Dashboard theo vai trò: Quản lý thấy đủ, Lễ tân không thấy doanh thu.
 * Đếm số thẻ (StatCard) và số biểu đồ có trong cây thành phần của màn hình. Cần DB (màn hình nạp số liệu khi mở).
 */
class DashboardPanelTest {

    @BeforeAll
    static void requireDatabaseAndDisplay() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Cần môi trường có màn hình để dựng thành phần Swing.");
        boolean up;
        try {
            DBConnection.test();
            up = true;
        } catch (RuntimeException e) {
            up = false;
        }
        assumeTrue(up, "Không kết nối được DB, bỏ qua kiểm thử màn hình Dashboard.");
    }

    @AfterEach
    void logout() {
        Session.logout();     // Session là biến toàn cục: trả về trạng thái chưa đăng nhập cho test khác
    }

    private static DashboardPanel panelFor(StaffRole role) {
        Staff staff = role == StaffRole.MANAGER ? new Manager() : new Receptionist();
        staff.setFullName("Test " + role.getLabel());
        staff.setPhone("0900000000");
        staff.setSalary(new BigDecimal("100"));
        staff.setRole(role);
        Session.login(staff);
        return new DashboardPanel();
    }

    /** Đếm các thành phần thuộc một lớp trong cả cây con của container. */
    private static int count(Container root, Class<?> type) {
        int n = type.isInstance(root) ? 1 : 0;
        for (Component child : root.getComponents()) {
            if (child instanceof Container c) {
                n += count(c, type);
            } else if (type.isInstance(child)) {
                n++;
            }
        }
        return n;
    }

    @Test
    void managerSeesFourCardsAndBothCharts() {
        DashboardPanel panel = panelFor(StaffRole.MANAGER);
        assertEquals(4, count(panel, StatCard.class), "Quản lý thấy 4 thẻ");
        assertEquals(1, count(panel, MonthlyRevenueChartPanel.class), "Quản lý thấy biểu đồ doanh thu");
        assertEquals(1, count(panel, EquipmentStatusChartPanel.class), "Quản lý thấy biểu đồ thiết bị");
    }

    @Test
    void receptionistSeesThreeCardsAndOnlyTheEquipmentChart() {
        DashboardPanel panel = panelFor(StaffRole.RECEPTIONIST);
        assertEquals(3, count(panel, StatCard.class), "Lễ tân thấy 3 thẻ (không có doanh thu)");
        assertEquals(0, count(panel, MonthlyRevenueChartPanel.class), "Lễ tân không thấy biểu đồ doanh thu");
        assertEquals(1, count(panel, EquipmentStatusChartPanel.class), "Lễ tân vẫn thấy biểu đồ thiết bị");
    }

    @Test
    void receptionistDashboardShowsNoRevenueText() {
        DashboardPanel panel = panelFor(StaffRole.RECEPTIONIST);
        assertEquals(0, countLabelsContaining(panel, "Doanh thu"), "Không được có chữ 'Doanh thu' nào trên màn hình Lễ tân");
    }

    private static int countLabelsContaining(Container root, String text) {
        int n = 0;
        for (Component child : root.getComponents()) {
            if (child instanceof javax.swing.JLabel label && label.getText() != null && label.getText().contains(text)) {
                n++;
            }
            if (child instanceof Container c) {
                n += countLabelsContaining(c, text);
            }
        }
        return n;
    }
}
