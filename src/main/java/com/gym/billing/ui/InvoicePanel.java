package com.gym.billing.ui;

import com.github.lgooddatepicker.components.DatePicker;
import com.gym.billing.model.InvoiceView;
import com.gym.billing.service.BillingService;
import com.gym.billing.service.InvoicePdfService;
import com.gym.core.DateTimeUtils;
import com.gym.core.model.BillingStatus;
import com.gym.core.ui.Async;
import com.gym.core.ui.DataTable;
import com.gym.core.ui.DatePickers;
import com.gym.core.ui.Dialogs;
import com.gym.core.ui.Toast;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

/** Danh sách hóa đơn: tìm kiếm, lọc ngày/trạng thái, phân trang, thu tiền/thử lại và xuất PDF. */
public class InvoicePanel extends JPanel {
    private enum StatusFilter {
        ALL("Tất cả trạng thái", null), PENDING("Chờ thanh toán", BillingStatus.PENDING),
        PAID("Đã thanh toán", BillingStatus.PAID), FAILED("Thất bại", BillingStatus.FAILED);
        private final String label;
        private final BillingStatus status;
        StatusFilter(String label, BillingStatus status) { this.label = label; this.status = status; }
        @Override public String toString() { return label; }
    }

    private final BillingService service = new BillingService();
    private final InvoicePdfService pdfService = new InvoicePdfService();
    private final DataTable<InvoiceView> table = new DataTable<>(20);
    private final JTextField keyword = new JTextField(15);
    private final JComboBox<StatusFilter> status = new JComboBox<>(StatusFilter.values());
    private final DatePicker from = DatePickers.date();
    private final DatePicker to = DatePickers.date();
    private final JButton paidButton = new JButton("Đánh dấu đã thu tiền");
    private final JButton failedButton = new JButton("Đánh dấu thất bại");
    private final JButton retryButton = new JButton("Thử thanh toán lại");
    private final JButton pdfButton = new JButton("Xuất PDF");

    public InvoicePanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 4));
        filters.add(new JLabel("Tìm mã/tên/SĐT/gói:")); filters.add(keyword);
        filters.add(new JLabel("Trạng thái:")); filters.add(status);
        filters.add(new JLabel("Từ ngày:")); filters.add(from);
        filters.add(new JLabel("Đến ngày:")); filters.add(to);
        JButton search = new JButton("Lọc / Tải lại");
        JButton clear = new JButton("Bỏ lọc");
        filters.add(search); filters.add(clear);
        search.addActionListener(e -> table.goToPage(1));
        keyword.addActionListener(e -> table.goToPage(1));
        clear.addActionListener(e -> {
            keyword.setText(""); status.setSelectedItem(StatusFilter.ALL); from.clear(); to.clear(); table.goToPage(1);
        });

        table.addColumn("Mã HĐ", InvoiceView::billId, 65)
                .addColumn("Khách hàng", InvoiceView::customerName, 155)
                .addColumn("SĐT", InvoiceView::phone, 105)
                .addColumn("Gói / dịch vụ", InvoiceView::planName, 125)
                .addColumn("Ngày lập", i -> DateTimeUtils.format(i.invoiceDate()), 135)
                .addColumn("Hạn thanh toán", i -> DateTimeUtils.format(i.dueDate()), 115)
                .addColumn("Tổng tiền", i -> BillingFormat.money(i.totalAmount()), 115)
                .addColumn("Đã thu", i -> BillingFormat.money(i.amountPaid()), 105)
                .addColumn("Trạng thái", i -> i.status().getLabel(), 115)
                .setLoader((page, size) -> service.findInvoices(page, size, keyword.getText(), selectedStatus(), from.getDate(), to.getDate()))
                .onSelectionChanged(this::updateActions)
                .onDoubleClick(this::showDetails);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        paidButton.addActionListener(e -> markPaid());
        failedButton.addActionListener(e -> markFailed());
        retryButton.addActionListener(e -> retry());
        pdfButton.addActionListener(e -> exportPdf());
        actions.add(retryButton); actions.add(failedButton); actions.add(paidButton); actions.add(pdfButton);
        JPanel north = new JPanel(new BorderLayout(4, 4));
        north.add(new JLabel("HÓA ĐƠN & THANH TOÁN  ·  Hóa đơn Paid không thể sửa tiền/ngày; muốn sửa cần quy trình hoàn tiền riêng."), BorderLayout.NORTH);
        north.add(filters, BorderLayout.CENTER);
        add(north, BorderLayout.NORTH);
        add(table, BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);
        updateActions(null);
        table.reload();
    }

    private BillingStatus selectedStatus() {
        StatusFilter f = (StatusFilter) status.getSelectedItem();
        return f == null ? null : f.status;
    }

    private void updateActions(InvoiceView invoice) {
        boolean pending = invoice != null && invoice.status() == BillingStatus.PENDING;
        boolean failed = invoice != null && invoice.status() == BillingStatus.FAILED;
        paidButton.setEnabled(pending);
        failedButton.setEnabled(pending);
        retryButton.setEnabled(failed);
        pdfButton.setEnabled(invoice != null);
    }

    private void markPaid() {
        InvoiceView row = table.getSelected();
        if (row == null) return;
        if (!Dialogs.confirm(this, "Xác nhận đã nhận đủ " + BillingFormat.money(row.totalAmount())
                + " từ " + row.customerName() + "?\nHệ thống sẽ lưu trạng thái Paid và thời điểm thanh toán hiện tại.")) return;
        Async.run(this, () -> { service.markInvoicePaid(row.billId()); return null; }, ignored -> {
            Toast.show(this, "Đã ghi nhận thanh toán hóa đơn #" + row.billId() + "."); table.reload();
        });
    }

    private void markFailed() {
        InvoiceView row = table.getSelected();
        if (row == null) return;
        if (!Dialogs.confirm(this, "Đánh dấu hóa đơn #" + row.billId() + " là Thất bại? Số tiền đã thu sẽ là 0.")) return;
        Async.run(this, () -> { service.markInvoiceFailed(row.billId()); return null; }, ignored -> {
            Toast.show(this, "Đã cập nhật trạng thái hóa đơn."); table.reload();
        });
    }

    private void retry() {
        InvoiceView row = table.getSelected();
        if (row == null) return;
        Async.run(this, () -> { service.retryFailedInvoice(row.billId()); return null; }, ignored -> {
            Toast.show(this, "Hóa đơn đã chuyển về Chờ thanh toán."); table.reload();
        });
    }

    private void exportPdf() {
        InvoiceView row = table.getSelected();
        if (row == null) return;
        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        chooser.setDialogTitle("Lưu hóa đơn PDF");
        chooser.setSelectedFile(new java.io.File("hoa-don-" + row.billId() + ".pdf"));
        if (chooser.showSaveDialog(this) != javax.swing.JFileChooser.APPROVE_OPTION) return;
        Path output = chooser.getSelectedFile().toPath();
        if (!output.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf")) output = output.resolveSibling(output.getFileName() + ".pdf");
        Path finalOutput = output;
        if (Files.exists(finalOutput) && !Dialogs.confirm(this,
                "Tệp đã tồn tại:\n" + finalOutput + "\nBạn có muốn ghi đè không?")) return;
        Async.run(this, () -> { pdfService.exportInvoice(row.billId(), finalOutput); return finalOutput; }, path ->
                Dialogs.info(this, "Đã xuất hóa đơn #" + row.billId() + " tại:\n" + path));
    }

    private void showDetails(InvoiceView i) {
        String detail = "Mã hóa đơn: HD-" + i.billId()
                + "\nKhách hàng: " + i.customerName() + " (" + i.phone() + ")"
                + "\nGói / dịch vụ: " + i.planName()
                + "\nNgày lập: " + DateTimeUtils.format(i.invoiceDate())
                + "\nHạn thanh toán: " + DateTimeUtils.format(i.dueDate())
                + "\nNgày thanh toán: " + DateTimeUtils.format(i.paymentDate())
                + "\nTổng tiền: " + BillingFormat.money(i.totalAmount())
                + "\nĐã thu: " + BillingFormat.money(i.amountPaid())
                + "\nTrạng thái: " + i.status().getLabel();
        Dialogs.info(this, detail);
    }
}
