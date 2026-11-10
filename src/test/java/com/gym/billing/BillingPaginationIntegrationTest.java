package com.gym.billing;

import com.gym.billing.dao.BillingQueryDao;
import com.gym.billing.model.InvoiceView;
import com.gym.billing.model.MembershipView;
import com.gym.billing.service.BillingService;
import com.gym.core.DBConnection;
import com.gym.core.dao.MemberDao;
import com.gym.core.dao.MembershipDao;
import com.gym.core.dao.MembershipPlanDao;
import com.gym.core.dao.PageResult;
import com.gym.core.dao.VisitorDao;
import com.gym.core.model.BillingStatus;
import com.gym.core.model.Member;
import com.gym.core.model.Membership;
import com.gym.core.model.MembershipPlan;
import com.gym.core.model.MembershipStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Deque;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * TC27: nạp hàng nghìn hóa đơn/Membership; tìm kiếm và phân trang DB-side.
 * Bỏ qua nếu không nối được SQL Server.
 */
class BillingPaginationIntegrationTest {

    private static final int INVOICE_COUNT = 1200;
    private static final int MEMBERSHIP_COUNT = 250;

    private final MembershipPlanDao planDao = new MembershipPlanDao();
    private final MemberDao memberDao = new MemberDao();
    private final VisitorDao visitorDao = new VisitorDao();
    private final MembershipDao membershipDao = new MembershipDao();
    private final BillingService billingService = new BillingService();
    private final BillingQueryDao queryDao = new BillingQueryDao();

    @BeforeAll
    static void requireDatabase() {
        boolean up;
        try {
            DBConnection.test();
            up = true;
        } catch (RuntimeException e) {
            up = false;
        }
        assumeTrue(up, "Không kết nối được DB, bỏ qua TC27 integration.");
    }

    @Test
    void tc27_largeInvoiceAndMembershipPages_areDbSideCorrect() throws Exception {
        long base = System.nanoTime() % 10_000_000L;
        String tag = "PG" + base;
        String phone = "07" + String.format("%08d", base % 100_000_000L);
        Deque<Runnable> cleanup = new ArrayDeque<>();

        MembershipPlan plan = new MembershipPlan();
        plan.setPlanName("ZZ-Page-" + tag);
        plan.setDurationMonths(1);
        plan.setPrice(new BigDecimal("150000.00"));
        planDao.insert(plan);
        cleanup.push(() -> planDao.deleteById(plan.getPlanId()));

        Member member = new Member();
        member.setFirstName("Page");
        member.setLastName(tag);
        member.setPhone(phone);
        member.setEmail("page" + tag + "@test.com");
        member.setBirthDate(LocalDate.of(1990, 5, 5));
        member.setPhoto(new byte[]{9, 9, 9});
        memberDao.insert(member);
        cleanup.push(() -> visitorDao.deleteById(member.getVisitorId()));

        Membership active = new Membership();
        active.setVisitorId(member.getVisitorId());
        active.setPlanId(plan.getPlanId());
        active.setStartDate(LocalDate.now().minusDays(10));
        active.setEndDate(LocalDate.now().plusDays(20));
        active.setStatus(MembershipStatus.ACTIVE);
        membershipDao.insert(active);
        cleanup.push(() -> membershipDao.deleteById(active.getMembershipId()));

        try {
            seedExpiredMemberships(member.getVisitorId(), plan.getPlanId(), MEMBERSHIP_COUNT - 1);
            seedInvoices(member.getVisitorId(), active.getMembershipId(), INVOICE_COUNT);

            long invoiceStarted = System.nanoTime();
            PageResult<InvoiceView> page1 = billingService.findInvoices(
                    1, 50, tag, BillingStatus.PENDING, null, null);
            PageResult<InvoiceView> page2 = billingService.findInvoices(
                    2, 50, tag, BillingStatus.PENDING, null, null);
            long invoiceMs = (System.nanoTime() - invoiceStarted) / 1_000_000L;

            assertEquals(INVOICE_COUNT, page1.totalItems());
            assertEquals(50, page1.items().size());
            assertEquals(50, page2.items().size());
            assertTrue(page1.items().stream().allMatch(i -> i.customerName().contains(tag)));
            assertTrue(page2.items().stream().noneMatch(i ->
                    page1.items().stream().anyMatch(p -> p.billId() == i.billId())));
            assertTrue(invoiceMs < 3000, "Phân trang hóa đơn quá chậm: " + invoiceMs + "ms");

            long membershipStarted = System.nanoTime();
            PageResult<MembershipView> mPage1 = billingService.findMemberships(
                    1, 40, tag, null);
            PageResult<MembershipView> mPage3 = billingService.findMemberships(
                    3, 40, tag, null);
            long membershipMs = (System.nanoTime() - membershipStarted) / 1_000_000L;

            assertEquals(MEMBERSHIP_COUNT, mPage1.totalItems());
            assertEquals(40, mPage1.items().size());
            assertEquals(40, mPage3.items().size());
            assertTrue(mPage1.items().stream().allMatch(m -> m.memberName().contains(tag)));
            assertTrue(membershipMs < 3000, "Phân trang membership quá chậm: " + membershipMs + "ms");

            // Đảm bảo OFFSET/FETCH không kéo full list về JVM: tổng item của một trang đúng pageSize.
            assertEquals(50, queryDao.findInvoicePage(1, 50, tag, BillingStatus.PENDING, null, null)
                    .items().size());
        } finally {
            deleteSeedData(member.getVisitorId(), plan.getPlanId());
            while (!cleanup.isEmpty()) {
                try {
                    cleanup.pop().run();
                } catch (RuntimeException ignored) {
                    // best-effort
                }
            }
        }
    }

    private static void seedExpiredMemberships(int visitorId, int planId, int count) throws Exception {
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(
                     "INSERT INTO MEMBERSHIP (Visitor_ID, Plan_ID, Start_Date, End_Date, Status) "
                             + "VALUES (?, ?, ?, ?, 'Expired')")) {
            cn.setAutoCommit(false);
            LocalDate start = LocalDate.now().minusYears(3);
            for (int i = 0; i < count; i++) {
                LocalDate s = start.plusDays(i);
                ps.setInt(1, visitorId);
                ps.setInt(2, planId);
                ps.setDate(3, java.sql.Date.valueOf(s));
                ps.setDate(4, java.sql.Date.valueOf(s.plusMonths(1)));
                ps.addBatch();
                if (i % 100 == 99) {
                    ps.executeBatch();
                }
            }
            ps.executeBatch();
            cn.commit();
            cn.setAutoCommit(true);
        }
    }

    private static void seedInvoices(int visitorId, int membershipId, int count) throws Exception {
        try (Connection cn = DBConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(
                     "INSERT INTO BILLING_LOG (Visitor_ID, Membership_ID, Invoice_Date, Due_Date, "
                             + "Payment_Date, Amount_Paid, Billing_Status) "
                             + "VALUES (?, ?, DATEADD(DAY, -?, CURRENT_TIMESTAMP), CAST(GETDATE() AS DATE), "
                             + "NULL, 0.00, 'Pending')")) {
            cn.setAutoCommit(false);
            for (int i = 0; i < count; i++) {
                ps.setInt(1, visitorId);
                ps.setInt(2, membershipId);
                ps.setInt(3, i % 400);
                ps.addBatch();
                if (i % 200 == 199) {
                    ps.executeBatch();
                }
            }
            ps.executeBatch();
            cn.commit();
            cn.setAutoCommit(true);
        }
    }

    private static void deleteSeedData(int visitorId, int planId) throws Exception {
        try (Connection cn = DBConnection.getConnection(); Statement st = cn.createStatement()) {
            st.executeUpdate("DELETE FROM BILLING_LOG WHERE Visitor_ID = " + visitorId);
            st.executeUpdate("DELETE FROM MEMBERSHIP WHERE Visitor_ID = " + visitorId
                    + " OR Plan_ID = " + planId);
        }
    }

}
