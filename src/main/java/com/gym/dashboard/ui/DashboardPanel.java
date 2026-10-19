package com.gym.dashboard.ui;

import com.gym.app.Session;
import com.gym.core.model.StaffRole;
import com.gym.core.ui.Async;
import com.gym.core.ui.Theme;
import com.gym.dashboard.service.DashboardService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.text.DecimalFormat;

/**
 * Màn hình Dashboard: hàng thẻ số liệu ở trên, khu biểu đồ ở dưới.
 * Màn hình chỉ gọi Service (không gọi DAO) và chạy việc nặng bằng Async.
 */
public class DashboardPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");   // 2245 -> 2,245.00

    private final DashboardService service = new DashboardService();
    /** Chỉ Quản lý được xem doanh thu (ma trận phân quyền); Lễ tân chỉ xem số liệu cơ bản. */
    private final boolean canSeeRevenue = Session.hasRole(StaffRole.MANAGER);

    private final StatCard activeMembersCard = new StatCard("Hội viên đang hoạt động", "gói còn hiệu lực hôm nay", Theme.SUCCESS);
    private final StatCard monthlyRevenueCard = new StatCard("Doanh thu tháng này", "tính theo ngày", Theme.ACCENT);
    private final StatCard expiringMembershipsCard = new StatCard("Gói sắp hết hạn", "gói hết hạn trong " + DashboardService.EXPIRING_DAYS + " ngày", Theme.WARNING);
    private final StatCard classTodayCard = new StatCard("Lớp học hôm nay", "số lớp học hôm nay", Theme.INFO);

    private final JPanel chartArea = new JPanel(new GridLayout(1, 0, 12, 12));   // 1 hàng, số cột = số biểu đồ đang hiện
    private final EquipmentStatusChartPanel equipmentChart = new EquipmentStatusChartPanel();
    private final MonthlyRevenueChartPanel monthlyRevenueChart = new MonthlyRevenueChartPanel();

    public DashboardPanel() {
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        add(buildHeader(), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(12, 12));
        center.add(buildCards(), BorderLayout.NORTH);
        center.add(buildChartArea(), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        refresh();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Dashboard");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        JButton reload = new JButton("Làm mới");
        reload.addActionListener(e -> refresh());
        header.add(title, BorderLayout.WEST);
        header.add(reload, BorderLayout.EAST);
        return header;
    }

    private JPanel buildCards() {
        // Quản lý thấy 4 thẻ; Lễ tân thấy 3 thẻ (không có doanh thu) nên lưới đổi số cột theo số thẻ
        JPanel cards = new JPanel(new GridLayout(1, canSeeRevenue ? 4 : 3, 12, 12));
        cards.add(activeMembersCard);
        if (canSeeRevenue) {
            cards.add(monthlyRevenueCard);
        }
        cards.add(expiringMembershipsCard);
        cards.add(classTodayCard);
        return cards;
    }

    private JPanel buildChartArea() {
        if (canSeeRevenue) {
            chartArea.add(monthlyRevenueChart);     // biểu đồ 1 (bên trái): doanh thu theo tháng, chỉ Quản lý
        }
        chartArea.add(equipmentChart);              // biểu đồ 2: thiết bị theo trạng thái, ai cũng thấy
        return chartArea;
    }

    /** Nạp lại số liệu: mỗi chỉ số một việc nền riêng, xong chỉ số nào hiện chỉ số đó. */
    public void refresh() {
        activeMembersCard.clear();
        Async.run(this, service::activeMembers, count -> activeMembersCard.setValue(String.valueOf(count)));
        expiringMembershipsCard.clear();
        Async.run(this, service::expiringMemberships, count -> expiringMembershipsCard.setValue(String.valueOf(count)));
        classTodayCard.clear();
        Async.run(this, service::classToday, count -> classTodayCard.setValue(String.valueOf(count)));
        Async.run(this, service::equipmentByStatus, equipmentChart::setData);          // biểu đồ 2
        if (canSeeRevenue) {                    // chỉ Quản lý: nếu không, Service từ chối và Lễ tân thấy hộp thoại lỗi
            monthlyRevenueCard.clear();
            Async.run(this, service::monthlyRevenue, money -> monthlyRevenueCard.setValue(MONEY.format(money)));
            Async.run(this, service::monthlyRevenueChart, monthlyRevenueChart::setData);   // biểu đồ 1
        }
    }
}
