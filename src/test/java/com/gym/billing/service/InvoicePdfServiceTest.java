package com.gym.billing.service;

import com.gym.billing.dao.BillingQueryDao;
import com.gym.billing.model.InvoiceView;
import com.gym.core.model.BillingStatus;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** TC24: xuất PDF hóa đơn Paid với tên tiếng Việt. */
class InvoicePdfServiceTest {

    private Path output;

    @AfterEach
    void cleanup() {
        if (output != null) {
            try {
                Files.deleteIfExists(output);
            } catch (Exception ignored) {
                output.toFile().deleteOnExit();
            }
        }
    }

    @Test
    void tc24_exportPaidInvoicePdf_keepsVietnameseFields() throws Exception {
        InvoiceView invoice = new InvoiceView(
                42,
                7,
                "Nguyễn Văn An",
                "0901234567",
                99,
                "Gói VIP 3 tháng",
                LocalDateTime.of(2026, 10, 17, 9, 30),
                LocalDate.of(2026, 10, 24),
                LocalDateTime.of(2026, 10, 17, 10, 0),
                new BigDecimal("1200000.00"),
                new BigDecimal("1200000.00"),
                BillingStatus.PAID
        );
        FakeBillingQueryDao queryDao = new FakeBillingQueryDao(invoice);
        InvoicePdfService pdfService = new InvoicePdfService(queryDao);
        output = Files.createTempFile("invoice-tv3-", ".pdf");

        pdfService.exportInvoice(42, output);

        assertTrue(Files.size(output) > 0);
        byte[] pdfBytes = Files.readAllBytes(output);
        String text;
        PdfReader reader = new PdfReader(pdfBytes);
        try {
            text = new PdfTextExtractor(reader).getTextFromPage(1);
        } finally {
            reader.close();
        }
        assertTrue(text.contains("HD-42") || text.contains("42"), text);
        assertTrue(text.contains("Nguyễn Văn An"), text);
        assertTrue(text.contains("Gói VIP 3 tháng"), text);
        assertTrue(text.contains(BillingStatus.PAID.getLabel()) || text.contains("Paid")
                || text.contains("Đã thanh toán"), text);
        assertTrue(text.contains("1.200.000") || text.contains("1200000") || text.contains("1,200,000"), text);
    }

    private static final class FakeBillingQueryDao extends BillingQueryDao {
        private final InvoiceView invoice;

        FakeBillingQueryDao(InvoiceView invoice) {
            this.invoice = invoice;
        }

        @Override
        public Optional<InvoiceView> findInvoiceById(int billId) {
            return billId == invoice.billId() ? Optional.of(invoice) : Optional.empty();
        }
    }
}
