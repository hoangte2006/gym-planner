package com.gym.billing.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import com.gym.billing.model.ExpiringMembershipView;
import com.gym.billing.service.BillingService;
import com.gym.core.DateTimeUtils;
import com.gym.core.ui.Async;
import com.gym.core.ui.Toast;

/** Cảnh báo gói hết hạn trong 7/30 ngày, có số điện thoại liên hệ và màu + chữ cảnh báo. */
public class ExpiryPanel extends JPanel {
    private final BillingService service = new BillingService();
    private final JComboBox<Integer> days = new JComboBox<>(new Integer[]{7, 30});
    private final ExpiryTableModel model = new ExpiryTableModel();
    private final JTable table = new JTable(model);
    private final JLabel summary = new JLabel("Chọn phạm vi 7 hoặc 30 ngày.");

    public ExpiryPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Hiển thị gói sắp hết hạn trong")); top.add(days); top.add(new JLabel("ngày kể từ hôm nay"));
        JButton reload = new JButton("Tải danh sách");
        reload.addActionListener(e -> load());
        top.add(reload);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(28);
        table.setFillsViewportHeight(true);
        table.setDefaultRenderer(Object.class, new ExpiryRenderer());
        summary.setBorder(BorderFactory.createEmptyBorder(3, 4, 3, 4));
        JPanel north = new JPanel(new BorderLayout(4, 4));
        north.add(new JLabel("SẮP HẾT HẠN  ·  Đỏ: hết hạn hôm nay (0 ngày)  ·  Vàng: còn từ 1 đến 7 ngày"), BorderLayout.NORTH);
        north.add(top, BorderLayout.CENTER);
        add(north, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(summary, BorderLayout.SOUTH);
        load();
    }

    private void load() {
        Integer count = (Integer) days.getSelectedItem();
        int range = count == null ? 7 : count;
        Async.run(this, () -> service.findExpiring(range), rows -> {
            model.setRows(rows);
            long today = rows.stream().filter(r -> r.daysLeft() == 0).count();
            long week = rows.stream().filter(r -> r.daysLeft() >= 1 && r.daysLeft() <= 7).count();
            summary.setText("Tổng " + rows.size() + " gói · Hết hạn hôm nay: " + today + " · Còn 1–7 ngày: " + week);
            if (rows.isEmpty()) Toast.show(this, "Không có gói sắp hết hạn trong " + range + " ngày.");
        });
    }

    private final class ExpiryRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                                  boolean focus, int row, int column) {
            super.getTableCellRendererComponent(t, value, selected, focus, row, column);
            if (!selected && row >= 0 && row < model.rows.size()) {
                long left = model.rows.get(row).daysLeft();
                setBackground(left <= 0 ? new Color(0xFDE2E2) : left <= 7 ? new Color(0xFFF2CC) : Color.WHITE);
                setForeground(new Color(0x202020));
            }
            return this;
        }
    }

    private static final class ExpiryTableModel extends AbstractTableModel {
        private final String[] columns = {"Mã gói", "Mã HV", "Hội viên", "SĐT liên hệ", "Gói tập", "Ngày kết thúc", "Còn lại", "Cảnh báo"};
        private List<ExpiringMembershipView> rows = new ArrayList<>();
        void setRows(List<ExpiringMembershipView> newRows) { rows = new ArrayList<>(newRows); fireTableDataChanged(); }
        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return columns[column]; }
        @Override public Object getValueAt(int rowIndex, int columnIndex) {
            ExpiringMembershipView r = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> r.membershipId(); case 1 -> r.visitorId(); case 2 -> r.memberName(); case 3 -> r.phone();
                case 4 -> r.planName(); case 5 -> DateTimeUtils.format(r.endDate());
                case 6 -> r.daysLeft() + " ngày";
                case 7 -> r.daysLeft() <= 0 ? "HẾT HẠN HÔM NAY" : r.daysLeft() <= 7 ? "SẮP HẾT HẠN" : "CẦN LIÊN HỆ";
                default -> "";
            };
        }
    }
}
