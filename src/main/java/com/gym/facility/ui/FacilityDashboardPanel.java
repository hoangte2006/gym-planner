
package com.gym.facility.ui;

import com.gym.core.dao.RoomDao;
import com.gym.core.dao.StaffDao;
import com.gym.core.model.Room;
import com.gym.core.model.Staff;
import com.gym.core.model.StaffRole;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

public class FacilityDashboardPanel extends JPanel {

    private final StaffDao staffDao = new StaffDao();
    private final RoomDao roomDao = new RoomDao();

    private static final Color PRIMARY = new Color(37, 99, 235);
    private static final Color BACKGROUND = new Color(245, 247, 251);
    private static final Color TEXT = new Color(31, 41, 55);

    public FacilityDashboardPanel() {
        setLayout(new BorderLayout());
        setBackground(BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel title = new JLabel("Nhân sự & Cơ sở vật chất");
        title.setFont(new Font("Segoe UI", Font.BOLD, 25));
        title.setForeground(TEXT);

        JLabel subtitle = new JLabel(
                "Quản lý thông tin nhân viên và phòng tập"
        );
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(new Color(107, 114, 128));

        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.setOpaque(false);
        heading.add(title);
        heading.add(Box.createVerticalStrut(8));
        heading.add(subtitle);
        heading.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 14));

        tabs.addTab(
                "  Nhân sự  ",
                createStaffPanel()
        );

        tabs.addTab(
                "  Phòng tập  ",
                createRoomPanel()
        );

        add(heading, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel createStaffPanel() {
        return new DataTablePanel<>(
                "Danh sách nhân sự",
                new String[]{
                        "Mã NV", "Họ và tên", "Số điện thoại",
                        "Vai trò", "Ngày vào làm", "Lương"
                },
                this::loadStaff,
                staff -> new Object[]{
                        staff.getStaffId(),
                        staff.getFullName(),
                        staff.getPhone(),
                        staff.getRoleName(),
                        staff.getHireDate() == null
                                ? "—"
                                : staff.getHireDate().format(
                                DateTimeFormatter.ofPattern("dd/MM/yyyy")
                        ),
                        staff.getSalary() == null
                                ? "—"
                                : NumberFormat.getNumberInstance(
                                Locale.forLanguageTag("vi-VN")
                        ).format(staff.getSalary()) + " đ"
                }
        );
    }

    private JPanel createRoomPanel() {
        return new DataTablePanel<Room>(
                "Danh sách phòng tập",
                new String[]{
                        "Mã phòng", "Tên phòng", "Sức chứa", "Tầng / Vị trí"
                },
                ArrayList::new,
                room -> new Object[]{
                        room.getRoomId(),
                        room.getRoomName(),
                        room.getCapacity() + " người",
                        room.getFloorLocation() == null
                                || room.getFloorLocation().isBlank()
                                ? "Chưa cập nhật"
                                : room.getFloorLocation()
                }
        );
    }

    private List<Staff> loadStaff() {
        List<Staff> result = new ArrayList<>();

        // Lấy nhân sự theo từng vai trò từ DAO hiện có.
        for (StaffRole role : StaffRole.values()) {
            List<Staff> staffByRole = staffDao.findByRole(role);
            if (staffByRole != null) {
                result.addAll(staffByRole);
            }
        }

        result.sort((a, b) ->
                Integer.compare(a.getStaffId(), b.getStaffId()));

        return result;
    }

    /**
     * Bảng dữ liệu dùng chung cho màn hình nhân sự và phòng.
     * Tìm kiếm và phân trang thực hiện trên dữ liệu đã tải.
     */
    private static class DataTablePanel<T> extends JPanel {

        private static final int PAGE_SIZE = 10;

        private final String heading;
        private final String[] columns;
        private final Supplier<List<T>> loader;
        private final Function<T, Object[]> rowMapper;

        private final JTextField searchField = new JTextField(22);
        private final JLabel statusLabel = new JLabel(" ");
        private final JLabel pageLabel = new JLabel();
        private final JButton previousButton = new JButton("← Trước");
        private final JButton nextButton = new JButton("Sau →");

        private final DefaultTableModel tableModel;
        private final JTable table;

        private List<T> allRows = new ArrayList<>();
        private List<T> filteredRows = new ArrayList<>();
        private int currentPage = 0;

        DataTablePanel(
                String heading,
                String[] columns,
                Supplier<List<T>> loader,
                Function<T, Object[]> rowMapper
        ) {
            this.heading = heading;
            this.columns = columns;
            this.loader = loader;
            this.rowMapper = rowMapper;

            setLayout(new BorderLayout(0, 16));
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

            tableModel = new DefaultTableModel(columns, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            table = new JTable(tableModel);
            table.setRowHeight(38);
            table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setAutoCreateRowSorter(true);
            table.getTableHeader().setFont(
                    new Font("Segoe UI", Font.BOLD, 13)
            );
            table.getTableHeader().setReorderingAllowed(false);

            buildToolbar();
            buildTable();
            buildFooter();

            reloadData();
        }

        private void buildToolbar() {
            JPanel toolbar = new JPanel(new BorderLayout(12, 8));
            toolbar.setOpaque(false);

            JLabel title = new JLabel(heading);
            title.setFont(new Font("Segoe UI", Font.BOLD, 18));
            title.setForeground(TEXT);

            JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            searchPanel.setOpaque(false);

            searchField.setToolTipText("Nhập từ khóa tìm kiếm");
            searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            searchField.setPreferredSize(new Dimension(240, 36));

            JButton searchButton = new JButton("Tìm kiếm");
            stylePrimaryButton(searchButton);
            searchButton.addActionListener(e -> applySearch());

            JButton refreshButton = new JButton("Làm mới");
            refreshButton.addActionListener(e -> reloadData());

            searchPanel.add(searchField);
            searchPanel.add(searchButton);
            searchPanel.add(refreshButton);

            searchField.addActionListener(e -> applySearch());

            toolbar.add(title, BorderLayout.NORTH);
            toolbar.add(searchPanel, BorderLayout.SOUTH);

            add(toolbar, BorderLayout.NORTH);
        }

        private void buildTable() {
            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.setBorder(
                    BorderFactory.createLineBorder(new Color(229, 231, 235))
            );
            scrollPane.getViewport().setBackground(Color.WHITE);

            add(scrollPane, BorderLayout.CENTER);
        }

        private void buildFooter() {
            JPanel footer = new JPanel(new BorderLayout());
            footer.setOpaque(false);

            statusLabel.setForeground(new Color(107, 114, 128));
            pageLabel.setForeground(TEXT);

            JPanel paging = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            paging.setOpaque(false);

            previousButton.addActionListener(e -> {
                if (currentPage > 0) {
                    currentPage--;
                    renderPage();
                }
            });

            nextButton.addActionListener(e -> {
                int totalPages = getTotalPages();
                if (currentPage + 1 < totalPages) {
                    currentPage++;
                    renderPage();
                }
            });

            paging.add(previousButton);
            paging.add(pageLabel);
            paging.add(nextButton);

            footer.add(statusLabel, BorderLayout.WEST);
            footer.add(paging, BorderLayout.EAST);

            add(footer, BorderLayout.SOUTH);
        }

        private void reloadData() {
            try {
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                List<T> loaded = loader.get();
                allRows = loaded == null
                        ? new ArrayList<>()
                        : new ArrayList<>(loaded);

                applySearch();

            } catch (Exception ex) {
                allRows = new ArrayList<>();
                filteredRows = new ArrayList<>();
                currentPage = 0;
                renderPage();

                statusLabel.setText("Không thể tải dữ liệu.");
                JOptionPane.showMessageDialog(
                        this,
                        "Không thể tải dữ liệu từ cơ sở dữ liệu.\n"
                                + "Chi tiết: " + ex.getMessage(),
                        "Lỗi tải dữ liệu",
                        JOptionPane.ERROR_MESSAGE
                );
            } finally {
                setCursor(Cursor.getDefaultCursor());
            }
        }

        private void applySearch() {
            String keyword = searchField.getText()
                    .trim()
                    .toLowerCase(Locale.ROOT);

            filteredRows = new ArrayList<>();

            for (T item : allRows) {
                Object[] values = rowMapper.apply(item);

                boolean matches = keyword.isEmpty();

                if (!matches) {
                    for (Object value : values) {
                        if (value != null
                                && value.toString()
                                .toLowerCase(Locale.ROOT)
                                .contains(keyword)) {
                            matches = true;
                            break;
                        }
                    }
                }

                if (matches) {
                    filteredRows.add(item);
                }
            }

            currentPage = 0;
            renderPage();
        }

        private int getTotalPages() {
            return Math.max(
                    1,
                    (int) Math.ceil(filteredRows.size() / (double) PAGE_SIZE)
            );
        }

        private void renderPage() {
            tableModel.setRowCount(0);

            int start = currentPage * PAGE_SIZE;
            int end = Math.min(start + PAGE_SIZE, filteredRows.size());

            for (int i = start; i < end; i++) {
                tableModel.addRow(rowMapper.apply(filteredRows.get(i)));
            }

            statusLabel.setText(
                    "Tổng cộng " + filteredRows.size() + " bản ghi"
            );

            pageLabel.setText(
                    "Trang " + (currentPage + 1) + "/" + getTotalPages()
            );

            previousButton.setEnabled(currentPage > 0);
            nextButton.setEnabled(currentPage + 1 < getTotalPages());
        }

        private static void stylePrimaryButton(JButton button) {
            button.setBackground(PRIMARY);
            button.setForeground(Color.WHITE);
            button.setFocusPainted(false);
            button.setOpaque(true);
            button.setBorderPainted(false);
        }
    }
}
