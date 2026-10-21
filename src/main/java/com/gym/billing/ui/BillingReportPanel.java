package com.gym.billing.ui;

import com.github.lgooddatepicker.components.DatePicker;
import com.gym.billing.model.InvoiceView;
import com.gym.billing.model.RevenueByPlan;
import com.gym.billing.service.BillingService;
import com.gym.core.DateTimeUtils;
import com.gym.core.ui.Async;
import com.gym.core.ui.DataTable;
import com.gym.core.ui.DatePickers;
import com.gym.core.ui.Dialogs;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;

/** Báo cáo doanh thu đã thu theo gói và công nợ chưa thanh toán. */
public class BillingReportPanel extends JPanel {
    private final BillingService service = new BillingService();
    private final DatePicker from = DatePickers.date();
    private final DatePicker to = DatePickers.date();
    private final JTextField debtKeyword = new JTextField(18);
    private final DataTable<RevenueByPlan> revenueTable = new DataTable<>(20);
    private final DataTable<InvoiceView> debtTable = new DataTable<>(20);

    public BillingReportPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        LocalDate today = LocalDate.now();
        from.setDate(today.withDayOfMonth(1));
        to.setDate(today);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filters.add(new JLabel("Kỳ báo cáo từ:")); filters.add(from);
        filters.add(new JLabel("đến:")); filters.add(to);
        JButton run = new JButton("Chạy báo cáo");
        run.addActionListener(e -> loadReports());
        filters.add(run);
        JTabbedPane reportTabs = new JTabbedPane();
        revenueTable.addColumn("Gói / dịch vụ", RevenueByPlan::planName, 240)
                .addColumn("Số hóa đơn Paid", RevenueByPlan::paidInvoiceCount, 150)
                .addColumn("Doanh thu đã thu", r -> BillingFormat.money(r.revenue()), 180);
        debtTable.addColumn("Mã HĐ", InvoiceView::billId, 70)
                .addColumn("Khách hàng", InvoiceView::customerName, 160)
                .addColumn("SĐT", InvoiceView::phone, 110)
                .addColumn("Gói / dịch vụ", InvoiceView::planName, 130)
                .addColumn("Ngày lập", i -> DateTimeUtils.format(i.invoiceDate()), 135)
                .addColumn("Hạn thanh toán", i -> DateTimeUtils.format(i.dueDate()), 115)
                .addColumn("Số tiền phải thu", r -> BillingFormat.money(r.totalAmount()), 140)
                .addColumn("Trạng thái", i -> i.status().getLabel(), 120)
                .setLoader((page, size) -> service.findDebts(page, size, debtKeyword.getText(), from.getDate(), to.getDate()));
        JPanel debtPanel = new JPanel(new BorderLayout(6, 6));
        JPanel debtFilters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        debtFilters.add(new JLabel("Lọc công nợ theo tên/SĐT:")); debtFilters.add(debtKeyword);
        JButton searchDebt = new JButton("Tìm công nợ");
        searchDebt.addActionListener(e -> debtTable.goToPage(1));
        debtKeyword.addActionListener(e -> debtTable.goToPage(1));
        debtFilters.add(searchDebt);
        debtPanel.add(debtFilters, BorderLayout.NORTH); debtPanel.add(debtTable, BorderLayout.CENTER);
        reportTabs.addTab("Doanh thu theo gói", revenueTable);
        reportTabs.addTab("Công nợ Pending / Failed", debtPanel);
        JPanel north = new JPanel(new BorderLayout(4, 4));
        north.add(new JLabel("BÁO CÁO TV3  ·  Doanh thu tính theo ngày thanh toán của hóa đơn Paid; công nợ gồm Pending và Failed."), BorderLayout.NORTH);
        north.add(filters, BorderLayout.CENTER);
        add(north, BorderLayout.NORTH);
        add(reportTabs, BorderLayout.CENTER);
        loadReports();
    }

    private void loadReports() {
        LocalDate start = from.getDate();
        LocalDate end = to.getDate();
        if (start == null || end == null) { Dialogs.warn(this, "Hãy chọn đủ ngày bắt đầu và ngày kết thúc."); return; }
        if (end.isBefore(start)) { Dialogs.warn(this, "Ngày kết thúc không được trước ngày bắt đầu."); return; }
        Async.run(this, () -> service.revenueByPlan(start, end), revenueTable::setItems);
        debtTable.goToPage(1);
    }
}
