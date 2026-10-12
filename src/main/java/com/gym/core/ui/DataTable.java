package com.gym.core.ui;

import com.gym.core.DateTimeUtils;
import com.gym.core.dao.PageResult;
import com.gym.core.model.DbEnum;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Bảng dữ liệu có phân trang, dùng cho mọi danh sách trong ứng dụng.
 *
 * <pre>
 * DataTable&lt;Room&gt; table = new DataTable&lt;&gt;(20);
 * table.addColumn("Tên phòng", Room::getRoomName);
 * table.addColumn("Sức chứa", Room::getCapacity, 80);
 * table.setLoader((page, size) -&gt; roomService.findPage(page, size));
 * table.reload();                                   // nạp trang 1 ở luồng nền
 * table.onDoubleClick(room -&gt; openEditor(room));
 * </pre>
 *
 * Giá trị hiển thị tự định dạng: ngày dd/MM/yyyy, tiền 1,234.50, enum thành nhãn tiếng Việt có màu.
 */
public class DataTable<T> extends JPanel {

    private static final long serialVersionUID = 1L;

    /** Hàm nạp một trang dữ liệu (chạy ở luồng nền). page bắt đầu từ 1. */
    @FunctionalInterface
    public interface PageLoader<T> {
        PageResult<T> load(int page, int pageSize) throws Exception;
    }

    private record Column<T>(String title, Function<T, Object> getter, int width) {
    }

    private final List<Column<T>> columns = new ArrayList<>();
    private final DefaultTableModel model = new DefaultTableModel() {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JButton prevButton = new JButton("‹ Trước");
    private final JButton nextButton = new JButton("Sau ›");
    private final JLabel infoLabel = new JLabel(" ", SwingConstants.CENTER);

    private final int pageSize;
    private PageLoader<T> loader;
    private List<T> items = new ArrayList<>();
    private int page = 1;
    private int totalPages = 1;
    private Consumer<T> doubleClickListener;
    private Consumer<T> selectionListener;

    public DataTable(int pageSize) {
        super(new BorderLayout());
        this.pageSize = pageSize;

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowGrid(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(0xE0E0E0));
        table.setFillsViewportHeight(true);
        table.setDefaultRenderer(Object.class, new CellRenderer());
        table.getTableHeader().setReorderingAllowed(false);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.add(prevButton);
        buttons.add(nextButton);
        bar.add(infoLabel, BorderLayout.CENTER);
        bar.add(buttons, BorderLayout.EAST);
        add(bar, BorderLayout.SOUTH);

        prevButton.addActionListener(e -> goToPage(page - 1));
        nextButton.addActionListener(e -> goToPage(page + 1));
        updatePagingControls(0);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && doubleClickListener != null) {
                    T selected = getSelected();
                    if (selected != null) {
                        doubleClickListener.accept(selected);
                    }
                }
            }
        });
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && selectionListener != null) {
                selectionListener.accept(getSelected());
            }
        });
    }

    // ---------- Khai báo cột và nguồn dữ liệu ----------

    public DataTable<T> addColumn(String title, Function<T, Object> getter) {
        return addColumn(title, getter, 0);
    }

    /** width = 0 thì để bảng tự chia. */
    public DataTable<T> addColumn(String title, Function<T, Object> getter, int width) {
        columns.add(new Column<>(title, getter, width));
        applyColumns();
        return this;
    }

    /** Đặt hàm nạp từng trang (dùng cho bảng lớn). */
    public DataTable<T> setLoader(PageLoader<T> loader) {
        this.loader = loader;
        return this;
    }

    public DataTable<T> onDoubleClick(Consumer<T> listener) {
        this.doubleClickListener = listener;
        return this;
    }

    /** Gọi khi người dùng chọn một dòng (đối số có thể null khi bỏ chọn). */
    public DataTable<T> onSelectionChanged(Consumer<T> listener) {
        this.selectionListener = listener;
        return this;
    }

    // ---------- Nạp dữ liệu ----------

    /** Nạp lại trang đang xem (hoặc trang 1 nếu chưa có). */
    public void reload() {
        goToPage(page);
    }

    public void goToPage(int target) {
        if (loader == null) {
            throw new IllegalStateException("Chưa gọi setLoader().");
        }
        int wanted = Math.max(1, target);
        Async.run(this, () -> loader.load(wanted, pageSize), result -> {
            this.page = wanted;
            this.totalPages = Math.max(1, result.totalPages());
            showItems(result.items());
            updatePagingControls(result.totalItems());
        });
    }

    /** Hiện sẵn một danh sách (không phân trang, không cần loader). */
    public void setItems(List<T> newItems) {
        this.page = 1;
        this.totalPages = 1;
        showItems(newItems);
        updatePagingControls(newItems.size());
    }

    /** Dòng đang chọn; null nếu chưa chọn. */
    public T getSelected() {
        int row = table.getSelectedRow();
        return row < 0 || row >= items.size() ? null : items.get(row);
    }

    public List<T> getItems() {
        return List.copyOf(items);
    }

    // ---------- Nội bộ ----------

    private void showItems(List<T> newItems) {
        this.items = new ArrayList<>(newItems);
        applyColumns();
        model.setRowCount(0);
        for (T item : items) {
            Object[] row = new Object[columns.size()];
            for (int i = 0; i < row.length; i++) {
                row[i] = columns.get(i).getter().apply(item);
            }
            model.addRow(row);
        }
    }

    private void applyColumns() {
        Object[] titles = columns.stream().map(Column::title).toArray();
        if (model.getColumnCount() != titles.length) {
            model.setColumnIdentifiers(titles);
        }
        for (int i = 0; i < columns.size() && i < table.getColumnModel().getColumnCount(); i++) {
            int width = columns.get(i).width();
            if (width > 0) {
                table.getColumnModel().getColumn(i).setPreferredWidth(width);
            }
        }
    }

    private void updatePagingControls(long totalItems) {
        prevButton.setEnabled(page > 1);
        nextButton.setEnabled(page < totalPages);
        infoLabel.setText(totalItems == 0 ? "Không có dữ liệu"
                : "Trang " + page + " / " + totalPages + "  ·  " + totalItems + " dòng");
    }

    /** Định dạng giá trị theo kiểu: ngày, tiền, enum (nhãn + màu). */
    private static class CellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        private final DecimalFormat money = new DecimalFormat("#,##0.00");

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus,
                                                       int row, int column) {
            String text;
            Color color = null;
            if (value == null) {
                text = "";
            } else if (value instanceof DbEnum e) {
                text = e.getLabel();
                color = Color.decode(e.getColorHex());
            } else if (value instanceof LocalDate d) {
                text = DateTimeUtils.format(d);
            } else if (value instanceof LocalDateTime dt) {
                text = DateTimeUtils.format(dt);
            } else if (value instanceof BigDecimal b) {
                text = money.format(b);
            } else if (value instanceof Boolean b) {
                text = b ? "Có" : "Không";
            } else {
                text = value.toString();
            }
            super.getTableCellRendererComponent(t, text, selected, focus, row, column);
            if (color != null && !selected) {
                setForeground(color);
            }
            setHorizontalAlignment(value instanceof Number || value instanceof BigDecimal
                    ? SwingConstants.RIGHT : SwingConstants.LEFT);
            return this;
        }
    }
}
