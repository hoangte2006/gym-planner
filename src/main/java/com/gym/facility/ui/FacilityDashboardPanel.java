package com.gym.facility.ui;

import com.gym.core.dao.EquipmentDao;
import com.gym.core.dao.RoomDao;
import com.gym.core.dao.StaffDao;
import com.gym.core.model.Equipment;
import com.gym.core.model.EquipmentStatus;
import com.gym.core.model.Room;
import com.gym.core.model.Staff;
import com.gym.core.model.StaffRole;
import com.gym.facility.service.EquipmentService;
import com.gym.facility.service.RoomService;
import com.gym.facility.service.StaffService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

public class FacilityDashboardPanel extends JPanel {

    private final StaffService staffService = new StaffService(new StaffDao());
    private final RoomService roomService = new RoomService(new RoomDao());
    private final EquipmentService equipmentService =
            new EquipmentService(new EquipmentDao(), new RoomDao());

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
                "Quản lý thông tin nhân viên, phòng tập và thiết bị"
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

        tabs.addTab(
                "  Thiết bị  ",
                createEquipmentPanel()
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
        return new DataTablePanel<>(
                "Danh sách phòng tập",
                new String[]{
                        "Mã phòng", "Tên phòng", "Sức chứa", "Tầng / Vị trí"
                },
                this::loadRoom,
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

    private JPanel createEquipmentPanel() {
        return new EquipmentManagementPanel(equipmentService);
    }

    private List<Staff> loadStaff() {
        List<Staff> result = new ArrayList<>();

        // Lấy nhân sự theo từng vai trò từ DAO hiện có.
        for (StaffRole role : StaffRole.values()) {
            List<Staff> staffByRole = staffService.findStaffByRole(role);
            if (staffByRole != null) {
                result.addAll(staffByRole);
            }
        }

        result.sort((a, b) ->
                Integer.compare(a.getStaffId(), b.getStaffId()));

        return result;
    }

    private List<Room> loadRoom() {
        List<Room> result = new ArrayList<>();

        List<Room> roomByPage = roomService.findAllPaged(1, 10);
        result.addAll(roomByPage);

        return result;

    }
    /**
     * Màn hình quản lý thiết bị. Giao diện chỉ gọi EquipmentService;
     * mọi kiểm tra nghiệp vụ và thao tác dữ liệu do Service/DAO đảm nhiệm.
     */
    private static class EquipmentManagementPanel extends JPanel {

        private static final int PAGE_SIZE = 10;
        private static final DateTimeFormatter DATE_FORMAT =
                DateTimeFormatter.ofPattern("dd/MM/yyyy");

        private final EquipmentService equipmentService;
        private final JTextField searchField = new JTextField(20);
        private final JLabel statusLabel = new JLabel(" ");
        private final JLabel pageLabel = new JLabel();
        private final JButton previousButton = new JButton("← Trước");
        private final JButton nextButton = new JButton("Sau →");

        private final DefaultTableModel model = new DefaultTableModel(
                new String[]{"Mã thiết bị", "Tên thiết bị", "Mã phòng", "Tình trạng", "Ngày mua"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        private final JTable table = new JTable(model);

        private List<Equipment> allEquipment = new ArrayList<>();
        private List<Equipment> filteredEquipment = new ArrayList<>();
        private int currentPage;

        EquipmentManagementPanel(EquipmentService equipmentService) {
            this.equipmentService = equipmentService;

            setLayout(new BorderLayout(0, 16));
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

            buildToolbar();
            buildTable();
            buildFooter();
            reloadData();
        }

        private void buildToolbar() {
            JPanel toolbar = new JPanel(new BorderLayout(12, 10));
            toolbar.setOpaque(false);

            JLabel title = new JLabel("Danh sách thiết bị");
            title.setFont(new Font("Segoe UI", Font.BOLD, 18));
            title.setForeground(TEXT);

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            actions.setOpaque(false);

            searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            searchField.setPreferredSize(new Dimension(220, 36));
            searchField.setToolTipText("Tìm theo mã, tên, phòng, tình trạng hoặc ngày mua");

            JButton searchButton = new JButton("Tìm kiếm");
            stylePrimaryButton(searchButton);
            searchButton.addActionListener(e -> applySearch());
            searchField.addActionListener(e -> applySearch());

            JButton refreshButton = new JButton("Làm mới");
            refreshButton.addActionListener(e -> reloadData());

            JButton addButton = new JButton("+ Thêm thiết bị");
            stylePrimaryButton(addButton);
            addButton.addActionListener(e -> showEquipmentDialog(null));

            JButton editButton = new JButton("Sửa");
            editButton.addActionListener(e -> editSelectedEquipment());

            JButton deleteButton = new JButton("Xóa");
            deleteButton.addActionListener(e -> deleteSelectedEquipment());

            actions.add(searchField);
            actions.add(searchButton);
            actions.add(refreshButton);
            actions.add(addButton);
            actions.add(editButton);
            actions.add(deleteButton);

            toolbar.add(title, BorderLayout.NORTH);
            toolbar.add(actions, BorderLayout.CENTER);
            add(toolbar, BorderLayout.NORTH);
        }

        private void buildTable() {
            table.setRowHeight(38);
            table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setAutoCreateRowSorter(true);
            table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
            table.getTableHeader().setReorderingAllowed(false);

            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.setBorder(BorderFactory.createLineBorder(new Color(229, 231, 235)));
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
                if (currentPage + 1 < getTotalPages()) {
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
                List<Equipment> loaded = equipmentService.findAll();
                allEquipment = loaded == null ? new ArrayList<>() : new ArrayList<>(loaded);
                applySearch();
            } catch (Exception ex) {
                showError("Không thể tải danh sách thiết bị.\nChi tiết: " + ex.getMessage());
            } finally {
                setCursor(Cursor.getDefaultCursor());
            }
        }

        private void applySearch() {
            String keyword = searchField.getText().trim().toLowerCase(Locale.ROOT);
            filteredEquipment = new ArrayList<>();

            for (Equipment equipment : allEquipment) {
                String status = equipment.getOperationalStatus() == null
                        ? "" : equipment.getOperationalStatus().getDbValue();
                String roomId = equipment.getRoomId() == null
                        ? "" : String.valueOf(equipment.getRoomId());
                String purchaseDate = equipment.getPurchaseDate() == null
                        ? "" : equipment.getPurchaseDate().format(DATE_FORMAT);
                String searchable = String.join(" ",
                        String.valueOf(equipment.getEquipmentId()),
                        equipment.getEquipmentName() == null ? "" : equipment.getEquipmentName(),
                        roomId, status, purchaseDate
                ).toLowerCase(Locale.ROOT);

                if (keyword.isEmpty() || searchable.contains(keyword)) {
                    filteredEquipment.add(equipment);
                }
            }

            filteredEquipment.sort((a, b) -> Integer.compare(
                    a.getEquipmentId() == null ? 0 : a.getEquipmentId(),
                    b.getEquipmentId() == null ? 0 : b.getEquipmentId()
            ));
            currentPage = 0;
            renderPage();
        }

        private int getTotalPages() {
            return Math.max(1, (int) Math.ceil(filteredEquipment.size() / (double) PAGE_SIZE));
        }

        private void renderPage() {
            model.setRowCount(0);
            int start = currentPage * PAGE_SIZE;
            int end = Math.min(start + PAGE_SIZE, filteredEquipment.size());

            for (int i = start; i < end; i++) {
                Equipment equipment = filteredEquipment.get(i);
                model.addRow(new Object[]{
                        equipment.getEquipmentId(),
                        equipment.getEquipmentName(),
                        equipment.getRoomId() == null ? "Chưa xếp phòng" : equipment.getRoomId(),
                        equipment.getOperationalStatus() == null
                                ? "Chưa cập nhật" : equipment.getOperationalStatus().getDbValue(),
                        equipment.getPurchaseDate() == null
                                ? "—" : equipment.getPurchaseDate().format(DATE_FORMAT)
                });
            }

            statusLabel.setText("Tổng cộng " + filteredEquipment.size() + " thiết bị");
            pageLabel.setText("Trang " + (currentPage + 1) + "/" + getTotalPages());
            previousButton.setEnabled(currentPage > 0);
            nextButton.setEnabled(currentPage + 1 < getTotalPages());
        }

        private Equipment getSelectedEquipment() {
            int selectedViewRow = table.getSelectedRow();
            if (selectedViewRow < 0) {
                JOptionPane.showMessageDialog(this,
                        "Vui lòng chọn một thiết bị trong bảng.",
                        "Chưa chọn thiết bị", JOptionPane.INFORMATION_MESSAGE);
                return null;
            }

            int selectedModelRow = table.convertRowIndexToModel(selectedViewRow);
            Object idValue = model.getValueAt(selectedModelRow, 0);
            if (!(idValue instanceof Number)) {
                showError("Không đọc được mã thiết bị đã chọn.");
                return null;
            }
            int id = ((Number) idValue).intValue();

            for (Equipment equipment : allEquipment) {
                if (equipment.getEquipmentId() != null && equipment.getEquipmentId() == id) {
                    return equipment;
                }
            }
            showError("Không tìm thấy thiết bị trong dữ liệu đã tải. Hãy làm mới bảng.");
            return null;
        }

        private void editSelectedEquipment() {
            Equipment selected = getSelectedEquipment();
            if (selected != null) {
                showEquipmentDialog(selected);
            }
        }

        private void deleteSelectedEquipment() {
            Equipment selected = getSelectedEquipment();
            if (selected == null) {
                return;
            }

            int answer = JOptionPane.showConfirmDialog(
                    this,
                    "Bạn có chắc muốn xóa thiết bị \"" + selected.getEquipmentName() + "\" không?",
                    "Xác nhận xóa thiết bị",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (answer != JOptionPane.YES_OPTION) {
                return;
            }

            try {
                equipmentService.delete(selected.getEquipmentId());
                reloadData();
                JOptionPane.showMessageDialog(this, "Đã xóa thiết bị.",
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                showError("Không thể xóa thiết bị.\nChi tiết: " + ex.getMessage());
            }
        }

        private void showEquipmentDialog(Equipment existing) {
            JTextField nameField = new JTextField(
                    existing == null || existing.getEquipmentName() == null
                            ? "" : existing.getEquipmentName(), 22);
            JTextField roomIdField = new JTextField(
                    existing == null || existing.getRoomId() == null
                            ? "" : existing.getRoomId().toString(), 22);
            JTextField purchaseDateField = new JTextField(
                    existing == null || existing.getPurchaseDate() == null
                            ? "" : existing.getPurchaseDate().toString(), 22);
            JComboBox<EquipmentStatus> statusCombo =
                    new JComboBox<>(EquipmentStatus.values());
            if (existing != null && existing.getOperationalStatus() != null) {
                statusCombo.setSelectedItem(existing.getOperationalStatus());
            }

            JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
            form.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));
            form.add(new JLabel("Tên thiết bị *"));
            form.add(nameField);
            form.add(new JLabel("Mã phòng (để trống nếu chưa xếp)"));
            form.add(roomIdField);
            form.add(new JLabel("Tình trạng *"));
            form.add(statusCombo);
            form.add(new JLabel("Ngày mua (yyyy-MM-dd)"));
            form.add(purchaseDateField);

            String title = existing == null ? "Thêm thiết bị" : "Cập nhật thiết bị";
            int result = JOptionPane.showConfirmDialog(
                    this, form, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) {
                return;
            }

            try {
                Equipment equipment = new Equipment();
                if (existing != null) {
                    equipment.setEquipmentId(existing.getEquipmentId());
                }
                equipment.setEquipmentName(nameField.getText().trim());

                String roomText = roomIdField.getText().trim();
                if (roomText.isEmpty()) {
                    equipment.setRoomId(null);
                } else {
                    equipment.setRoomId(Integer.parseInt(roomText));
                }

                EquipmentStatus selectedStatus = (EquipmentStatus) statusCombo.getSelectedItem();
                equipment.setOperationalStatus(selectedStatus);

                String dateText = purchaseDateField.getText().trim();
                if (dateText.isEmpty()) {
                    equipment.setPurchaseDate(null);
                } else {
                    equipment.setPurchaseDate(LocalDate.parse(dateText));
                }

                if (existing == null) {
                    equipmentService.create(equipment);
                } else {
                    equipmentService.update(equipment);
                }

                reloadData();
                JOptionPane.showMessageDialog(this,
                        existing == null ? "Đã thêm thiết bị." : "Đã cập nhật thiết bị.",
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } catch (NumberFormatException ex) {
                showError("Mã phòng phải là số nguyên hợp lệ.");
            } catch (DateTimeParseException ex) {
                showError("Ngày mua không hợp lệ. Hãy nhập theo định dạng yyyy-MM-dd, ví dụ 2026-10-21.");
            } catch (Exception ex) {
                showError("Không thể lưu thiết bị.\nChi tiết: " + ex.getMessage());
            }
        }

        private static void stylePrimaryButton(JButton button) {
            button.setBackground(PRIMARY);
            button.setForeground(Color.WHITE);
            button.setFocusPainted(false);
            button.setOpaque(true);
            button.setBorderPainted(false);
        }

        private void showError(String message) {
            JOptionPane.showMessageDialog(this, message, "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Bảng dữ liệu dùng chung cho màn hình nhân sự, phòng và thiết bị.
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
