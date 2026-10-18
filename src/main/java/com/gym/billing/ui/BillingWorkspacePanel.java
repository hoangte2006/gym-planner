package com.gym.billing.ui;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;

/** Màn hình gốc TV3. Các tab được tạo khi mở lần đầu để không bắn nhiều truy vấn song song khi vào module. */
public class BillingWorkspacePanel extends JPanel {
    private final JTabbedPane tabs = new JTabbedPane();
    private final String[] names = {"Bảng giá gói", "Bán / Gia hạn", "Gói đã bán", "Hóa đơn & Thanh toán", "Sắp hết hạn", "Báo cáo"};
    private final boolean[] loaded = new boolean[names.length];

    public BillingWorkspacePanel() {
        super(new BorderLayout());
        tabs.addTab(names[0], placeholder());
        for (int i = 1; i < names.length; i++) tabs.addTab(names[i], placeholder());
        tabs.addChangeListener(e -> loadTab(tabs.getSelectedIndex()));
        add(tabs, BorderLayout.CENTER);
        loadTab(0);
    }

    private JPanel placeholder() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("Màn hình sẽ được khởi tạo khi bạn mở tab này.", JLabel.CENTER), BorderLayout.CENTER);
        return panel;
    }

    private void loadTab(int index) {
        if (index < 0 || index >= names.length || loaded[index]) return;
        JPanel panel = switch (index) {
            case 0 -> new PlanPanel();
            case 1 -> new SellWizardPanel();
            case 2 -> new MembershipPanel();
            case 3 -> new InvoicePanel();
            case 4 -> new ExpiryPanel();
            case 5 -> new BillingReportPanel();
            default -> placeholder();
        };
        tabs.setComponentAt(index, panel);
        loaded[index] = true;
    }
}
