package com.gym.billing.ui;

import com.gym.billing.dao.BillingQueryDao;
import com.gym.billing.dao.BillingTransactionDao;
import com.gym.billing.model.ExpiringMembershipView;
import com.gym.billing.service.BillingService;
import com.gym.core.dao.MemberDao;
import com.gym.core.dao.MembershipPlanDao;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/** TC23: filter 7/30 ngày và màu/nhãn cảnh báo trên ExpiryPanel thật. */
class ExpiryPanelTest {

    private static final LocalDate TODAY = LocalDate.now();
    private static final Color RED = new Color(0xFDE2E2);
    private static final Color YELLOW = new Color(0xFFF2CC);

    @BeforeAll
    static void requireDisplay() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Cần màn hình để dựng ExpiryPanel.");
    }

    @Test
    void tc23_expiryPanel_filtersAndRendersWarningColors() throws Exception {
        List<ExpiringMembershipView> catalog = List.of(
                new ExpiringMembershipView(1, 11, "Hết hạn hôm nay", "0901", "Gói A", TODAY, 0),
                new ExpiringMembershipView(2, 12, "Sắp hết hạn", "0902", "Gói B", TODAY.plusDays(5), 5),
                new ExpiringMembershipView(3, 13, "Còn xa", "0903", "Gói C", TODAY.plusDays(20), 20)
        );
        FakeBillingQueryDao queryDao = new FakeBillingQueryDao(catalog);
        BillingService service = new BillingService(
                new BillingTransactionDao(), queryDao, new MembershipPlanDao(), new MemberDao());

        AtomicReference<ExpiryPanel> panelRef = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> panelRef.set(new ExpiryPanel(service)));
        ExpiryPanel panel = panelRef.get();

        awaitRows(panel, 2);
        assertEquals(7, queryDao.lastDays);
        assertEquals(2, panel.expiryTable().getRowCount());
        assertWarning(panel, 0, "HẾT HẠN HÔM NAY", RED);
        assertWarning(panel, 1, "SẮP HẾT HẠN", YELLOW);
        assertTrue(panel.summaryLabel().getText().contains("Tổng 2"));

        SwingUtilities.invokeAndWait(() -> {
            panel.daysSelector().setSelectedItem(30);
            panel.reloadNow();
        });
        awaitRows(panel, 3);
        assertEquals(30, queryDao.lastDays);
        assertEquals(3, panel.expiryTable().getRowCount());
        assertEquals("CẦN LIÊN HỆ", panel.expiryTable().getValueAt(2, 7));
        Component far = renderCell(panel.expiryTable(), 2, 0);
        assertEquals(Color.WHITE, far.getBackground());
    }

    private static void awaitRows(ExpiryPanel panel, int expected) throws Exception {
        long deadline = System.currentTimeMillis() + 5_000;
        while (System.currentTimeMillis() < deadline) {
            AtomicReference<Integer> count = new AtomicReference<>(-1);
            SwingUtilities.invokeAndWait(() -> count.set(panel.expiryTable().getRowCount()));
            if (count.get() == expected) {
                return;
            }
            Thread.sleep(40);
        }
        throw new AssertionError("Timeout chờ ExpiryPanel có " + expected + " dòng, hiện có "
                + panel.expiryTable().getRowCount());
    }

    private static void assertWarning(ExpiryPanel panel, int row, String label, Color background) throws Exception {
        AtomicReference<String> text = new AtomicReference<>();
        AtomicReference<Color> color = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            text.set(String.valueOf(panel.expiryTable().getValueAt(row, 7)));
            color.set(renderCell(panel.expiryTable(), row, 7).getBackground());
        });
        assertEquals(label, text.get());
        assertEquals(background, color.get());
    }

    private static Component renderCell(JTable table, int row, int column) {
        TableCellRenderer renderer = table.getCellRenderer(row, column);
        return table.prepareRenderer(renderer, row, column);
    }

    private static final class FakeBillingQueryDao extends BillingQueryDao {
        private final List<ExpiringMembershipView> catalog;
        private int lastDays = -1;

        FakeBillingQueryDao(List<ExpiringMembershipView> catalog) {
            this.catalog = catalog;
        }

        @Override
        public List<ExpiringMembershipView> findExpiringMemberships(LocalDate today, int days) {
            lastDays = days;
            return catalog.stream()
                    .filter(r -> r.daysLeft() >= 0 && r.daysLeft() <= days)
                    .toList();
        }
    }
}
