package com.gym.billing.service;

import com.gym.billing.dao.BillingQueryDao;
import com.gym.billing.model.InvoiceView;
import com.gym.core.DateTimeUtils;
import com.gym.core.exception.BusinessException;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/** Xuất PDF hóa đơn. Ưu tiên Arial trên Windows để nhúng đủ ký tự tiếng Việt. */
public class InvoicePdfService {
    private final BillingQueryDao queryDao;

    public InvoicePdfService() { this(new BillingQueryDao()); }

    public InvoicePdfService(BillingQueryDao queryDao) { this.queryDao = queryDao; }

    public void exportInvoice(int billId, Path output) {
        if (billId <= 0) throw new BusinessException("Mã hóa đơn không hợp lệ.");
        if (output == null) throw new BusinessException("Hãy chọn nơi lưu tệp PDF.");
        InvoiceView invoice = queryDao.findInvoiceById(billId)
                .orElseThrow(() -> new BusinessException("Hóa đơn không còn tồn tại. Hãy tải lại danh sách."));
        Path absolute = output.toAbsolutePath();
        try {
            Path parent = absolute.getParent();
            if (parent != null) Files.createDirectories(parent);
            Document document = new Document(PageSize.A4, 42, 42, 46, 42);
            try (OutputStream stream = Files.newOutputStream(absolute)) {
                PdfWriter.getInstance(document, stream);
                document.open();
                BaseFont baseFont = loadVietnameseFont();
                Font title = new Font(baseFont, 20, Font.BOLD);
                Font heading = new Font(baseFont, 12, Font.BOLD);
                Font body = new Font(baseFont, 11, Font.NORMAL);
                Font small = new Font(baseFont, 9, Font.NORMAL);

                Paragraph brand = new Paragraph("GYM PLANNER", title);
                brand.setAlignment(Element.ALIGN_CENTER);
                document.add(brand);
                Paragraph caption = new Paragraph("HÓA ĐƠN THANH TOÁN", heading);
                caption.setAlignment(Element.ALIGN_CENTER);
                caption.setSpacingAfter(18f);
                document.add(caption);

                PdfPTable table = new PdfPTable(new float[]{1.25f, 3.2f});
                table.setWidthPercentage(100);
                table.setSpacingBefore(6f);
                addRow(table, "Mã hóa đơn", "HD-" + invoice.billId(), heading, body);
                addRow(table, "Khách hàng", invoice.customerName(), heading, body);
                addRow(table, "Số điện thoại", invoice.phone(), heading, body);
                addRow(table, "Gói / dịch vụ", invoice.planName(), heading, body);
                addRow(table, "Ngày lập", DateTimeUtils.format(invoice.invoiceDate()), heading, body);
                addRow(table, "Hạn thanh toán", DateTimeUtils.format(invoice.dueDate()), heading, body);
                addRow(table, "Ngày thanh toán", DateTimeUtils.format(invoice.paymentDate()), heading, body);
                addRow(table, "Trạng thái", invoice.status().getLabel(), heading, body);
                addRow(table, "Tổng tiền", formatMoney(invoice.totalAmount()) + " VNĐ", heading, body);
                addRow(table, "Đã thanh toán", formatMoney(invoice.amountPaid()) + " VNĐ", heading, body);
                document.add(table);

                Paragraph note = new Paragraph("Lưu ý: hóa đơn gắn với gói tập được tính theo bảng giá của gói tại thời điểm xuất. "
                        + "Hệ thống không hỗ trợ trả góp.", small);
                note.setSpacingBefore(16f);
                document.add(note);
                Paragraph footer = new Paragraph("Gym Planner · Hóa đơn được tạo từ hệ thống quản lý.", small);
                footer.setAlignment(Element.ALIGN_CENTER);
                footer.setSpacingBefore(34f);
                document.add(footer);
                document.close();
            } catch (Exception e) {
                if (document.isOpen()) document.close();
                throw e;
            }
        } catch (Exception e) {
            throw new BusinessException("Không xuất được hóa đơn PDF. Hãy kiểm tra quyền ghi của thư mục và thử lại.", e);
        }
    }

    private static void addRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell left = new PdfPCell(new Phrase(label, labelFont));
        left.setPadding(8f);
        left.setVerticalAlignment(Element.ALIGN_MIDDLE);
        PdfPCell right = new PdfPCell(new Phrase(value == null || value.isBlank() ? "—" : value, valueFont));
        right.setPadding(8f);
        right.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(left);
        table.addCell(right);
    }

    private static String formatMoney(java.math.BigDecimal amount) {
        return java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("vi-VN"))
                .format(amount == null ? java.math.BigDecimal.ZERO : amount);
    }

    private static BaseFont loadVietnameseFont() throws Exception {
        String[] candidates = {
                "C:/Windows/Fonts/arial.ttf",
                "C:/Windows/Fonts/Arial.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                "/usr/share/fonts/truetype/liberation2/LiberationSans-Regular.ttf"
        };
        for (String candidate : candidates) {
            if (Files.isRegularFile(Path.of(candidate))) {
                return BaseFont.createFont(candidate, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            }
        }
        // Fallback vẫn tạo được PDF, nhưng font cơ bản có thể không hiển thị đầy đủ dấu tiếng Việt.
        return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
    }
}
