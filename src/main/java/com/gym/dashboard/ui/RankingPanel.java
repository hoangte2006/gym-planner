package com.gym.dashboard.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.List;

/**
 * Bảng xếp hạng nhỏ: tiêu đề và một bảng chỉ đọc (cột đầu là hạng). Dùng chung cho "Top huấn luyện viên" và
 * "Lớp học đông nhất". Cách dùng: dựng với tiêu đề và tên các cột, rồi gọi setRows(...) mỗi khi có dữ liệu mới
 * (trên luồng giao diện, ví dụ trong onSuccess của Async).
 */
public class RankingPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    static final String EMPTY_MESSAGE = "Chưa có dữ liệu trong khoảng này";

    private final DefaultTableModel model;
    private final JTable table;

    public RankingPanel(String title, String... columns) {
        super(new BorderLayout(0, 6));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        model = new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;                       // bảng chỉ để xem
            }
        };
        table = new JTable(model);
        table.setRowHeight(24);
        table.setRowSelectionAllowed(false);
        table.setFocusable(false);
        table.setShowGrid(false);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(center);                       // hạng căn giữa
        table.getColumnModel().getColumn(0).setMaxWidth(60);
        if (columns.length > 2) {
            table.getColumnModel().getColumn(columns.length - 1).setCellRenderer(center);   // cột số liệu cuối căn giữa
        }

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));

        JPanel body = new JPanel(new BorderLayout());
        body.add(table.getTableHeader(), BorderLayout.NORTH);
        body.add(table, BorderLayout.CENTER);

        add(titleLabel, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
        setRows(List.of());
    }

    /** Thay toàn bộ các dòng. Mỗi phần tử là một dòng, số ô bằng số cột. Danh sách rỗng thì hiện câu thông báo. */
    public void setRows(List<String[]> rows) {
        model.setRowCount(0);
        if (rows.isEmpty()) {
            String[] empty = new String[model.getColumnCount()];
            java.util.Arrays.fill(empty, "");
            empty[1] = EMPTY_MESSAGE;
            model.addRow(empty);
            return;
        }
        for (String[] row : rows) {
            model.addRow(row);
        }
    }

    /** Số dòng dữ liệu đang hiện (không tính dòng thông báo "chưa có dữ liệu"). */
    int dataRowCount() {
        boolean placeholder = model.getRowCount() == 1 && EMPTY_MESSAGE.equals(model.getValueAt(0, 1));
        return placeholder ? 0 : model.getRowCount();
    }
}
