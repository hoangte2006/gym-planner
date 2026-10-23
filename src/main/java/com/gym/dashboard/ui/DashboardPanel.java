package com.gym.dashboard.ui;

import com.gym.app.Session;
import com.gym.core.DateTimeUtils;
import com.gym.core.model.StaffRole;
import com.gym.core.ui.Async;
import com.gym.core.ui.Dialogs;
import com.gym.core.ui.Theme;
import com.gym.dashboard.model.BusiestClass;
import com.gym.dashboard.model.DateRange;
import com.gym.dashboard.model.TrainerSessionCount;
import com.gym.dashboard.service.DashboardService;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    /** Khoảng lọc đang chọn, dùng cho 2 biểu đồ theo tháng và cho tệp xuất. Chỉ đọc/ghi trên luồng giao diện. */
    private DateRange range = DateRange.lastMonths(LocalDate.now(), DashboardService.DEFAULT_RANGE_MONTHS);

    private final StatCard activeMembersCard = new StatCard("Hội viên đang hoạt động", "gói còn hiệu lực hôm nay", Theme.SUCCESS);
    private final StatCard monthlyRevenueCard = new StatCard("Doanh thu tháng này", "tính theo ngày", Theme.ACCENT);
    private final StatCard expiringMembershipsCard = new StatCard("Gói sắp hết hạn", expiringNote(DashboardService.EXPIRING_DAYS), Theme.WARNING);
    private final JComboBox<Integer> expiringDaysBox = new JComboBox<>(DashboardService.EXPIRING_DAY_OPTIONS.toArray(new Integer[0]));
    private final StatCard classTodayCard = new StatCard("Lớp học hôm nay", "số lớp học hôm nay", Theme.INFO);

    private final JPanel chartArea = new JPanel(new GridLayout(0, 2, 12, 12));   // 2 cột, số hàng tự tăng theo số biểu đồ đang hiện
    private final EquipmentStatusChartPanel equipmentChart = new EquipmentStatusChartPanel();
    private final MonthlyRevenueChartPanel monthlyRevenueChart = new MonthlyRevenueChartPanel();
    private final MaintenanceCostChartPanel maintenanceCostChart = new MaintenanceCostChartPanel();
    private final ClassOccupancyChartPanel classOccupancyChart = new ClassOccupancyChartPanel();

    private final RankingPanel topTrainersPanel = new RankingPanel("Top huấn luyện viên theo số buổi dạy",
            "Hạng", "Huấn luyện viên", "Số buổi");
    private final RankingPanel busiestClassesPanel = new RankingPanel("Lớp học đông nhất",
            "Hạng", "Lớp", "Đăng ký / sức chứa");

    public DashboardPanel() {
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        add(buildHeader(), BorderLayout.NORTH);

        // Phần thân nằm trong vùng cuộn: cửa sổ thấp thì cuộn dọc, cửa sổ cao thì giãn đầy khung
        ScrollableContent center = new ScrollableContent(new BorderLayout(12, 12));
        center.add(buildCards(), BorderLayout.NORTH);
        center.add(buildChartArea(), BorderLayout.CENTER);
        if (canSeeRevenue) {                    // bảng xếp hạng là số liệu phân tích, chỉ Quản lý
            center.add(buildRankings(), BorderLayout.SOUTH);
        }
        JScrollPane scroll = new JScrollPane(center);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        refresh();
    }

    private JPanel buildHeader() {
        JPanel top = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Dashboard");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        JButton reload = new JButton("Làm mới");
        reload.addActionListener(e -> refresh());

        expiringDaysBox.setRenderer(new DefaultListCellRenderer() {      // hiện "7 ngày" thay vì chỉ số 7
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index,
                                                                   boolean selected, boolean focus) {
                return super.getListCellRendererComponent(list, value + " ngày", index, selected, focus);
            }
        });
        expiringDaysBox.addActionListener(e -> refreshExpiring());        // đổi mốc chỉ nạp lại thẻ này

        JButton export = new JButton("Xuất ▾");
        export.addActionListener(e -> buildExportMenu().show(export, 0, export.getHeight()));

        JPanel tools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        tools.add(new JLabel("Gói sắp hết hạn trong:"));
        tools.add(expiringDaysBox);
        tools.add(export);
        tools.add(reload);
        top.add(title, BorderLayout.WEST);
        top.add(tools, BorderLayout.EAST);

        JPanel header = new JPanel(new BorderLayout(0, 8));
        header.add(top, BorderLayout.NORTH);
        if (canSeeRevenue) {                    // khoảng lọc chỉ tác động lên các biểu đồ tài chính nên chỉ Quản lý cần thấy
            header.add(new DateRangeBar(this::onRangeChanged), BorderLayout.CENTER);
        }
        return header;
    }

    private JPopupMenu buildExportMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem csv = new JMenuItem("Xuất CSV (mở bằng Excel)");
        csv.addActionListener(e -> export(false));
        JMenuItem pdf = new JMenuItem("Xuất PDF");
        pdf.addActionListener(e -> export(true));
        menu.add(csv);
        menu.add(pdf);
        return menu;
    }

    /** Hỏi nơi lưu rồi xuất báo cáo Dashboard ra CSV hoặc PDF (việc ghi tệp chạy ở luồng nền). */
    private void export(boolean pdf) {
        String extension = pdf ? ".pdf" : ".csv";
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(pdf ? "Lưu báo cáo PDF" : "Lưu báo cáo CSV");
        chooser.setSelectedFile(new File("dashboard-" + DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now()) + extension));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path chosen = chooser.getSelectedFile().toPath();
        if (!chosen.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(extension)) {
            chosen = chosen.resolveSibling(chosen.getFileName() + extension);       // tự thêm đuôi nếu người dùng quên gõ
        }
        Path output = chosen;
        if (Files.exists(output) && !Dialogs.confirm(this, "Tệp đã tồn tại:\n" + output + "\nBạn có muốn ghi đè không?")) {
            return;
        }
        DateRange exportRange = range;
        int days = (Integer) expiringDaysBox.getSelectedItem();
        Async.run(this, () -> {
            if (pdf) {
                service.exportPdf(exportRange, days, output);
            } else {
                service.exportCsv(exportRange, days, output);
            }
            return output;
        }, saved -> Dialogs.info(this, "Đã xuất báo cáo tại:\n" + saved));
    }

    /** Người dùng đổi khoảng thời gian: chỉ nạp lại các chỉ số phụ thuộc khoảng này. */
    private void onRangeChanged(DateRange newRange) {
        range = newRange;
        refreshRangeCharts();
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

    private JPanel buildRankings() {
        JPanel rankings = new JPanel(new GridLayout(1, 2, 12, 12));
        rankings.add(topTrainersPanel);
        rankings.add(busiestClassesPanel);
        return rankings;
    }

    private JPanel buildChartArea() {
        if (canSeeRevenue) {
            chartArea.add(monthlyRevenueChart);     // biểu đồ 1: doanh thu theo tháng, chỉ Quản lý
            chartArea.add(maintenanceCostChart);    // biểu đồ 4: chi phí bảo trì theo tháng, chỉ Quản lý
        }
        chartArea.add(classOccupancyChart);         // biểu đồ 3: tỷ lệ lấp đầy lớp trong tuần, ai cũng thấy
        chartArea.add(equipmentChart);              // biểu đồ 2: thiết bị theo trạng thái, ai cũng thấy
        return chartArea;
    }

    /** Nạp lại số liệu: mỗi chỉ số một việc nền riêng, xong chỉ số nào hiện chỉ số đó. */
    public void refresh() {
        activeMembersCard.clear();
        Async.run(this, service::activeMembers, count -> activeMembersCard.setValue(String.valueOf(count)));
        refreshExpiring();
        classTodayCard.clear();
        Async.run(this, service::classToday, count -> classTodayCard.setValue(String.valueOf(count)));
        Async.run(this, service::equipmentByStatus, equipmentChart::setData);          // biểu đồ 2
        Async.run(this, service::classOccupancyThisWeek, classOccupancyChart::setData);   // biểu đồ 3
        if (canSeeRevenue) {                    // chỉ Quản lý: nếu không, Service từ chối và Lễ tân thấy hộp thoại lỗi
            monthlyRevenueCard.clear();
            Async.run(this, service::monthlyRevenue, money -> monthlyRevenueCard.setValue(MONEY.format(money)));
            refreshRangeCharts();
        }
    }

    /** Nạp lại các chỉ số theo khoảng lọc hiện tại (2 biểu đồ theo tháng và 2 bảng xếp hạng). Chỉ Quản lý. */
    private void refreshRangeCharts() {
        DateRange current = range;
        Async.run(this, () -> service.monthlyRevenueChart(current), monthlyRevenueChart::setData);               // biểu đồ 1
        Async.run(this, () -> service.monthlyMaintenanceCostChart(current), maintenanceCostChart::setData);     // biểu đồ 4
        Async.run(this, () -> service.topTrainers(current), trainers -> {                                       // chỉ số 5
            List<String[]> rows = new ArrayList<>();
            int rank = 1;
            for (TrainerSessionCount t : trainers) {
                rows.add(new String[]{String.valueOf(rank++), t.trainerName(), String.valueOf(t.sessions())});
            }
            topTrainersPanel.setRows(rows);
        });
        Async.run(this, () -> service.busiestClasses(current), classes -> {                                     // chỉ số 6
            List<String[]> rows = new ArrayList<>();
            int rank = 1;
            for (BusiestClass c : classes) {
                rows.add(new String[]{String.valueOf(rank++), c.className() + " · " + DateTimeUtils.format(c.start()),
                        c.registrations() + " / " + c.capacity()});
            }
            busiestClassesPanel.setRows(rows);
        });
    }

    /** Nạp lại riêng thẻ "Gói sắp hết hạn" theo mốc ngày đang chọn (7 hoặc 30). */
    private void refreshExpiring() {
        int days = (Integer) expiringDaysBox.getSelectedItem();
        expiringMembershipsCard.setNote(expiringNote(days));
        expiringMembershipsCard.clear();
        Async.run(this, () -> service.expiringMemberships(days),
                count -> expiringMembershipsCard.setValue(String.valueOf(count)));
    }

    private static String expiringNote(int days) {
        return "gói hết hạn trong " + days + " ngày";
    }
}
