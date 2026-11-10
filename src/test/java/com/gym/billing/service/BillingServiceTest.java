package com.gym.billing.service;

import com.gym.billing.dao.BillingQueryDao;
import com.gym.billing.dao.BillingTransactionDao;
import com.gym.billing.model.CancellationResult;
import com.gym.billing.model.ClassImpact;
import com.gym.billing.model.ExpiringMembershipView;
import com.gym.billing.model.InvoiceView;
import com.gym.billing.model.MembershipView;
import com.gym.billing.model.RenewalBase;
import com.gym.billing.model.RevenueByPlan;
import com.gym.core.ErrorTranslator;
import com.gym.core.dao.MemberDao;
import com.gym.core.dao.MembershipPlanDao;
import com.gym.core.dao.PageResult;
import com.gym.core.exception.BusinessException;
import com.gym.core.model.BillingStatus;
import com.gym.core.model.Member;
import com.gym.core.model.MembershipPlan;
import com.gym.core.model.MembershipStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TC06–TC09, TC13, TC18–TC19 (side-effects), TC21–TC23, TC25–TC27. */
class BillingServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 17);

    private final Map<Integer, Boolean> photoFlags = new HashMap<>();
    private FakeBillingTransactionDao transactionDao;
    private FakeBillingQueryDao queryDao;
    private FakeMembershipPlanDao planDao;
    private FakeMemberDao memberDao;
    private BillingService service;

    @BeforeEach
    void setUp() {
        photoFlags.clear();
        transactionDao = new FakeBillingTransactionDao(photoFlags);
        queryDao = new FakeBillingQueryDao();
        planDao = new FakeMembershipPlanDao();
        memberDao = new FakeMemberDao(photoFlags);
        service = new BillingService(transactionDao, queryDao, planDao, memberDao);

        MembershipPlan plan = new MembershipPlan();
        plan.setPlanId(1);
        plan.setPlanName("Gói 1 tháng");
        plan.setDurationMonths(1);
        plan.setPrice(new BigDecimal("500000.00"));
        planDao.plans.put(1, plan);
        transactionDao.plans.put(1, plan);
    }

    @Test
    void tc06_sellWithoutPhoto_isBlocked() {
        photoFlags.put(10, false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.sellPlan(10, 1, TODAY));

        assertTrue(ex.getMessage().contains("ảnh"));
        assertTrue(transactionDao.memberships.isEmpty());
        assertTrue(transactionDao.invoices.isEmpty());
    }

    @Test
    void tc07_sellWithPhoto_createsActiveMembershipAndPendingBill() {
        photoFlags.put(11, true);

        BillingTransactionDao.SaleResult result = service.sellPlan(11, 1, TODAY);

        assertEquals(TODAY, result.startDate());
        assertEquals(TODAY.plusMonths(1), result.endDate());
        assertEquals(1, transactionDao.memberships.size());
        assertEquals(MembershipStatus.ACTIVE, transactionDao.memberships.get(0).status);
        assertEquals(1, transactionDao.invoices.size());
        FakeInvoice invoice = transactionDao.invoices.get(0);
        assertEquals(BillingStatus.PENDING, invoice.status);
        assertEquals(0, BigDecimal.ZERO.setScale(2).compareTo(invoice.amountPaid));
        assertEquals(TODAY.plusDays(7), invoice.dueDate);
        assertEquals(result.membershipId(), invoice.membershipId);
        assertEquals(11, invoice.visitorId);
    }

    @Test
    void tc08_sellWhenActiveExists_isBlocked() {
        photoFlags.put(12, true);
        transactionDao.memberships.add(new FakeMembership(100, 12, 1, TODAY.minusDays(5),
                TODAY.plusDays(10), MembershipStatus.ACTIVE));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.sellPlan(12, 1, TODAY));

        assertTrue(ex.getMessage().contains("Gia hạn"));
        long activeCount = transactionDao.memberships.stream()
                .filter(m -> m.visitorId == 12 && m.status == MembershipStatus.ACTIVE)
                .count();
        assertEquals(1, activeCount);
        assertTrue(transactionDao.invoices.isEmpty());
    }

    @Test
    void tc09_brMem07_mapsError50006_andSellKeepsSameOwner() {
        SQLException sql = new SQLException(
                "BR-MEM-07: BILLING_LOG.Visitor_ID must be the owner of the membership in Membership_ID.",
                "S0001",
                50006);
        assertEquals("Hóa đơn phải thuộc về chủ của gói tập được chọn.", ErrorTranslator.toMessage(sql));

        photoFlags.put(13, true);
        BillingTransactionDao.SaleResult result = service.sellPlan(13, 1, TODAY);
        FakeInvoice invoice = transactionDao.invoices.get(0);
        FakeMembership membership = transactionDao.memberships.get(0);
        assertEquals(result.visitorId(), invoice.visitorId);
        assertEquals(membership.visitorId, invoice.visitorId);
        assertEquals(membership.membershipId, invoice.membershipId);
    }

    @Test
    void tc13_renewWithoutMembership_isRejected() {
        photoFlags.put(14, true);
        queryDao.renewalBases.clear();

        BusinessException preview = assertThrows(BusinessException.class,
                () -> service.previewSale(14, 1, true, TODAY));
        BusinessException renew = assertThrows(BusinessException.class,
                () -> service.renewPlan(14, 1, TODAY));

        assertTrue(preview.getMessage().contains("Bán gói mới"));
        assertTrue(renew.getMessage().contains("Bán gói mới"));
    }

    @Test
    void tc18_markFailed_clearsPaymentFields() {
        FakeInvoice invoice = pendingInvoice(200, 20, 1);
        transactionDao.invoices.add(invoice);

        service.markInvoiceFailed(200);

        assertEquals(BillingStatus.FAILED, invoice.status);
        assertNull(invoice.paymentDate);
        assertEquals(0, BigDecimal.ZERO.setScale(2).compareTo(invoice.amountPaid));
    }

    @Test
    void tc19_retryFailed_resetsToPendingWithZeroPaid() {
        FakeInvoice invoice = pendingInvoice(201, 21, 1);
        invoice.status = BillingStatus.FAILED;
        invoice.paymentDate = LocalDateTime.of(2026, 10, 1, 10, 0);
        invoice.amountPaid = new BigDecimal("100.00");
        transactionDao.invoices.add(invoice);

        service.retryFailedInvoice(201);

        assertEquals(BillingStatus.PENDING, invoice.status);
        assertNull(invoice.paymentDate);
        assertEquals(0, BigDecimal.ZERO.setScale(2).compareTo(invoice.amountPaid));
    }

    @Test
    void tc21_expirePastMemberships_keepsEndDateTodayActive() {
        FakeMembership past = new FakeMembership(1, 30, 1, TODAY.minusMonths(1),
                TODAY.minusDays(1), MembershipStatus.ACTIVE);
        FakeMembership todayEnd = new FakeMembership(2, 31, 1, TODAY.minusMonths(1),
                TODAY, MembershipStatus.ACTIVE);
        transactionDao.memberships.add(past);
        transactionDao.memberships.add(todayEnd);

        int changed = service.expirePastMemberships(TODAY);

        assertEquals(1, changed);
        assertEquals(MembershipStatus.EXPIRED, past.status);
        assertEquals(MembershipStatus.ACTIVE, todayEnd.status);
    }

    @Test
    void tc22_cancelActive_showsImpactsWithoutAutoRefundOrClassCancel() {
        FakeMembership active = new FakeMembership(50, 40, 1, TODAY.minusDays(1),
                TODAY.plusDays(20), MembershipStatus.ACTIVE);
        transactionDao.memberships.add(active);
        ClassImpact impact = new ClassImpact(9, "Yoga sáng",
                LocalDateTime.of(2026, 10, 20, 8, 0),
                LocalDateTime.of(2026, 10, 20, 9, 0),
                "Registered");
        transactionDao.futureImpacts.put(40, List.of(impact));
        FakeInvoice invoice = pendingInvoice(300, 40, 50);
        invoice.status = BillingStatus.PAID;
        invoice.amountPaid = new BigDecimal("500000.00");
        invoice.paymentDate = LocalDateTime.of(2026, 10, 16, 9, 0);
        transactionDao.invoices.add(invoice);
        transactionDao.classCancelledFlags.put(9, false);

        CancellationResult preview = service.previewCancellation(50);
        assertEquals(1, preview.futureClasses().size());
        assertEquals("Yoga sáng", preview.futureClasses().get(0).className());

        CancellationResult cancelled = service.cancelMembership(50);
        assertEquals(MembershipStatus.CANCELLED, active.status);
        assertEquals(1, cancelled.futureClasses().size());
        assertEquals(BillingStatus.PAID, invoice.status);
        assertEquals(0, new BigDecimal("500000.00").compareTo(invoice.amountPaid));
        assertFalse(transactionDao.classCancelledFlags.get(9));
    }

    @Test
    void tc23_expiringFiltersAndWarningColors() {
        queryDao.expiringRows = List.of(
                new ExpiringMembershipView(1, 1, "A", "0901", "Gói", TODAY, 0),
                new ExpiringMembershipView(2, 2, "B", "0902", "Gói", TODAY.plusDays(5), 5),
                new ExpiringMembershipView(3, 3, "C", "0903", "Gói", TODAY.plusDays(20), 20)
        );

        List<ExpiringMembershipView> seven = service.findExpiring(7);
        assertEquals(7, queryDao.lastExpiringDays);
        assertEquals(2, seven.size());
        assertTrue(seven.stream().allMatch(r -> r.daysLeft() <= 7));

        List<ExpiringMembershipView> thirty = service.findExpiring(30);
        assertEquals(30, queryDao.lastExpiringDays);
        assertEquals(3, thirty.size());

        assertThrows(BusinessException.class, () -> service.findExpiring(14));

        assertEquals(new Color(0xFDE2E2), warningColor(0));
        assertEquals("HẾT HẠN HÔM NAY", warningLabel(0));
        assertEquals(new Color(0xFFF2CC), warningColor(5));
        assertEquals("SẮP HẾT HẠN", warningLabel(5));
        assertEquals(Color.WHITE, warningColor(20));
    }

    @Test
    void tc25_revenueIncludesOnlyPaidByPaymentDate() {
        queryDao.revenueRows = List.of(
                new RevenueByPlan("Gói 1 tháng", 2, new BigDecimal("1000000.00"))
        );

        List<RevenueByPlan> rows = service.revenueByPlan(TODAY.minusDays(30), TODAY);
        assertEquals(1, rows.size());
        assertEquals(0, new BigDecimal("1000000.00").compareTo(rows.get(0).revenue()));
        assertTrue(queryDao.pendingExcludedFromRevenue);
        assertEquals(BillingStatus.PAID, queryDao.lastRevenueStatusFilter);
    }

    @Test
    void tc26_debtsShowOnlyPendingOrFailed() {
        InvoiceView pending = invoiceView(1, 1, "Nguyễn A", BillingStatus.PENDING, TODAY.plusDays(3));
        InvoiceView failed = invoiceView(2, 2, "Trần B", BillingStatus.FAILED, TODAY.plusDays(1));
        queryDao.debtRows = List.of(pending, failed);

        PageResult<InvoiceView> debts = service.findDebts(1, 20, null, TODAY.minusDays(7), TODAY);
        assertEquals(2, debts.items().size());
        assertTrue(debts.items().stream().allMatch(i ->
                i.status() == BillingStatus.PENDING || i.status() == BillingStatus.FAILED));
        assertEquals("Nguyễn A", debts.items().get(0).customerName());
        assertEquals(TODAY.plusDays(3), debts.items().get(0).dueDate());
        assertTrue(queryDao.lastDebtOnly);
    }

    @Test
    void tc27_paginationIsDbSide() {
        List<MembershipView> all = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            all.add(new MembershipView(i, i, "HV " + i, "09" + i, "Gói",
                    TODAY.minusDays(i), TODAY.plusDays(i), MembershipStatus.ACTIVE));
        }
        queryDao.allMemberships = all;

        PageResult<MembershipView> page2 = service.findMemberships(2, 10, "HV", MembershipStatus.ACTIVE);
        assertEquals(2, page2.page());
        assertEquals(10, page2.pageSize());
        assertEquals(25, page2.totalItems());
        assertEquals(10, page2.items().size());
        assertEquals(11, page2.items().get(0).membershipId());
        assertEquals("HV", queryDao.lastMembershipKeyword);
        assertEquals(MembershipStatus.ACTIVE, queryDao.lastMembershipStatus);

        List<InvoiceView> invoices = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            invoices.add(invoiceView(i, i, "KH " + i, BillingStatus.PENDING, TODAY.plusDays(i)));
        }
        queryDao.allInvoices = invoices;
        PageResult<InvoiceView> invoicePage = service.findInvoices(1, 5, "KH", BillingStatus.PENDING,
                TODAY.minusDays(1), TODAY.plusDays(30));
        assertEquals(5, invoicePage.items().size());
        assertEquals(15, invoicePage.totalItems());
        assertEquals(1, invoicePage.page());
    }

    private static Color warningColor(long daysLeft) {
        return daysLeft <= 0 ? new Color(0xFDE2E2) : daysLeft <= 7 ? new Color(0xFFF2CC) : Color.WHITE;
    }

    private static String warningLabel(long daysLeft) {
        return daysLeft <= 0 ? "HẾT HẠN HÔM NAY" : daysLeft <= 7 ? "SẮP HẾT HẠN" : "CẦN LIÊN HỆ";
    }

    private static FakeInvoice pendingInvoice(int billId, int visitorId, int membershipId) {
        FakeInvoice invoice = new FakeInvoice();
        invoice.billId = billId;
        invoice.visitorId = visitorId;
        invoice.membershipId = membershipId;
        invoice.status = BillingStatus.PENDING;
        invoice.amountPaid = BigDecimal.ZERO.setScale(2);
        invoice.dueDate = TODAY.plusDays(7);
        invoice.invoiceDate = TODAY.atStartOfDay();
        invoice.totalAmount = new BigDecimal("500000.00");
        return invoice;
    }

    private static InvoiceView invoiceView(int billId, int visitorId, String name,
                                           BillingStatus status, LocalDate due) {
        return new InvoiceView(billId, visitorId, name, "0900000000", billId, "Gói 1 tháng",
                TODAY.atStartOfDay(), due, null, new BigDecimal("500000.00"),
                BigDecimal.ZERO.setScale(2), status);
    }

    private static final class FakeMembership {
        final int membershipId;
        final int visitorId;
        final int planId;
        final LocalDate startDate;
        final LocalDate endDate;
        MembershipStatus status;

        FakeMembership(int membershipId, int visitorId, int planId,
                       LocalDate startDate, LocalDate endDate, MembershipStatus status) {
            this.membershipId = membershipId;
            this.visitorId = visitorId;
            this.planId = planId;
            this.startDate = startDate;
            this.endDate = endDate;
            this.status = status;
        }
    }

    private static final class FakeInvoice {
        int billId;
        int visitorId;
        int membershipId;
        BillingStatus status;
        BigDecimal amountPaid;
        BigDecimal totalAmount;
        LocalDate dueDate;
        LocalDateTime invoiceDate;
        LocalDateTime paymentDate;
    }

    private static final class FakeBillingTransactionDao extends BillingTransactionDao {
        private final Map<Integer, Boolean> photoFlags;
        private final Map<Integer, MembershipPlan> plans = new HashMap<>();
        private final List<FakeMembership> memberships = new ArrayList<>();
        private final List<FakeInvoice> invoices = new ArrayList<>();
        private final Map<Integer, List<ClassImpact>> futureImpacts = new HashMap<>();
        private final Map<Integer, Boolean> classCancelledFlags = new HashMap<>();
        private final AtomicInteger membershipSeq = new AtomicInteger(1000);
        private final AtomicInteger billSeq = new AtomicInteger(2000);

        FakeBillingTransactionDao(Map<Integer, Boolean> photoFlags) {
            this.photoFlags = photoFlags;
        }

        @Override
        public SaleResult sellMembership(int visitorId, int planId, LocalDate today) {
            requirePhoto(visitorId);
            MembershipPlan plan = requirePlan(planId);
            for (FakeMembership old : memberships) {
                if (old.visitorId == visitorId && old.status == MembershipStatus.ACTIVE) {
                    if (old.endDate != null && old.endDate.isBefore(today)) {
                        old.status = MembershipStatus.EXPIRED;
                    } else {
                        throw new BusinessException("Hội viên đang có gói tập còn hiệu lực đến "
                                + old.endDate + "; hãy dùng chức năng Gia hạn thay vì bán gói mới.");
                    }
                }
            }
            return createSale(visitorId, planId, plan, today, today, false);
        }

        @Override
        public SaleResult renewMembership(int visitorId, int planId, LocalDate today) {
            requirePhoto(visitorId);
            MembershipPlan plan = requirePlan(planId);
            LocalDate renewalBase = today;
            Optional<FakeMembership> active = memberships.stream()
                    .filter(m -> m.visitorId == visitorId && m.status == MembershipStatus.ACTIVE)
                    .findFirst();
            if (active.isPresent()) {
                FakeMembership old = active.get();
                if (old.endDate != null && old.endDate.isAfter(today)) {
                    renewalBase = old.endDate;
                }
                old.status = MembershipStatus.EXPIRED;
            } else {
                Optional<FakeMembership> expired = memberships.stream()
                        .filter(m -> m.visitorId == visitorId && m.status == MembershipStatus.EXPIRED)
                        .findFirst();
                if (expired.isEmpty()) {
                    throw new BusinessException(
                            "Hội viên chưa có gói đang hiệu lực hoặc gói đã hết hạn để gia hạn. Hãy chọn Bán gói mới.");
                }
                if (expired.get().endDate != null && expired.get().endDate.isAfter(today)) {
                    renewalBase = expired.get().endDate;
                }
            }
            LocalDate start = BillingDateUtils.calculateRenewalStartDate(today, renewalBase);
            return createSale(visitorId, planId, plan, today, start, true);
        }

        @Override
        public CancellationResult previewCancellation(int membershipId, LocalDateTime now) {
            FakeMembership membership = requireActive(membershipId);
            return new CancellationResult(membershipId, membership.visitorId, "HV " + membership.visitorId,
                    "090", futureImpacts.getOrDefault(membership.visitorId, List.of()));
        }

        @Override
        public CancellationResult cancelMembership(int membershipId, LocalDateTime now) {
            FakeMembership membership = requireActive(membershipId);
            List<ClassImpact> impacts = futureImpacts.getOrDefault(membership.visitorId, List.of());
            membership.status = MembershipStatus.CANCELLED;
            return new CancellationResult(membershipId, membership.visitorId, "HV " + membership.visitorId,
                    "090", impacts);
        }

        @Override
        public void markInvoicePaid(int billId) {
            FakeInvoice invoice = requireInvoice(billId);
            BillingRules.requireCanMarkPaid(invoice.status);
            invoice.status = BillingStatus.PAID;
            invoice.paymentDate = LocalDateTime.now();
            invoice.amountPaid = BillingRules.normalizeMoney(invoice.totalAmount);
        }

        @Override
        public void markInvoiceFailed(int billId) {
            FakeInvoice invoice = requireInvoice(billId);
            BillingRules.requireCanMarkFailed(invoice.status);
            invoice.status = BillingStatus.FAILED;
            invoice.paymentDate = null;
            invoice.amountPaid = BigDecimal.ZERO.setScale(2);
        }

        @Override
        public void retryFailedInvoice(int billId) {
            FakeInvoice invoice = requireInvoice(billId);
            BillingRules.requireCanRetry(invoice.status);
            invoice.status = BillingStatus.PENDING;
            invoice.paymentDate = null;
            invoice.amountPaid = BigDecimal.ZERO.setScale(2);
        }

        @Override
        public int expirePastMemberships(LocalDate today) {
            int changed = 0;
            for (FakeMembership membership : memberships) {
                if (membership.status == MembershipStatus.ACTIVE
                        && membership.endDate != null
                        && membership.endDate.isBefore(today)) {
                    membership.status = MembershipStatus.EXPIRED;
                    changed++;
                }
            }
            return changed;
        }

        private SaleResult createSale(int visitorId, int planId, MembershipPlan plan,
                                      LocalDate invoiceDay, LocalDate startDate, boolean renewed) {
            LocalDate endDate = BillingDateUtils.calculateEndDate(startDate, plan.getDurationMonths());
            int membershipId = membershipSeq.getAndIncrement();
            memberships.add(new FakeMembership(membershipId, visitorId, planId, startDate, endDate,
                    MembershipStatus.ACTIVE));
            int billId = billSeq.getAndIncrement();
            FakeInvoice invoice = new FakeInvoice();
            invoice.billId = billId;
            invoice.visitorId = visitorId;
            invoice.membershipId = membershipId;
            invoice.status = BillingStatus.PENDING;
            invoice.amountPaid = BigDecimal.ZERO.setScale(2);
            invoice.totalAmount = BillingRules.normalizeMoney(plan.getPrice());
            invoice.dueDate = invoiceDay.plusDays(7);
            invoice.invoiceDate = invoiceDay.atStartOfDay();
            invoices.add(invoice);
            return new SaleResult(membershipId, billId, visitorId, planId, startDate, endDate,
                    BillingRules.normalizeMoney(plan.getPrice()), renewed);
        }

        private void requirePhoto(int visitorId) {
            if (!Boolean.TRUE.equals(photoFlags.get(visitorId))) {
                throw new BusinessException(
                        "Hội viên chưa có ảnh. Hãy mở Hồ sơ hội viên → Thêm ảnh ngay trước khi bán/gia hạn gói.");
            }
        }

        private MembershipPlan requirePlan(int planId) {
            MembershipPlan plan = plans.get(planId);
            if (plan == null) {
                throw new BusinessException("Gói tập không còn tồn tại. Hãy tải lại danh sách.");
            }
            return plan;
        }

        private FakeMembership requireActive(int membershipId) {
            FakeMembership membership = memberships.stream()
                    .filter(m -> m.membershipId == membershipId)
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("Không tìm thấy gói tập."));
            if (membership.status != MembershipStatus.ACTIVE) {
                throw new BusinessException("Chỉ có thể hủy gói đang ở trạng thái Đang hiệu lực.");
            }
            return membership;
        }

        private FakeInvoice requireInvoice(int billId) {
            return invoices.stream()
                    .filter(i -> i.billId == billId)
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("Không tìm thấy hóa đơn."));
        }
    }

    private static final class FakeBillingQueryDao extends BillingQueryDao {
        private final Map<Integer, RenewalBase> renewalBases = new HashMap<>();
        private List<ExpiringMembershipView> expiringRows = List.of();
        private int lastExpiringDays = -1;
        private List<RevenueByPlan> revenueRows = List.of();
        private boolean pendingExcludedFromRevenue;
        private BillingStatus lastRevenueStatusFilter = BillingStatus.PAID;
        private List<InvoiceView> debtRows = List.of();
        private boolean lastDebtOnly;
        private List<MembershipView> allMemberships = List.of();
        private String lastMembershipKeyword;
        private MembershipStatus lastMembershipStatus;
        private List<InvoiceView> allInvoices = List.of();

        @Override
        public Optional<RenewalBase> findRenewalBase(int visitorId) {
            return Optional.ofNullable(renewalBases.get(visitorId));
        }

        @Override
        public List<ExpiringMembershipView> findExpiringMemberships(LocalDate today, int days) {
            lastExpiringDays = days;
            return expiringRows.stream()
                    .filter(r -> r.daysLeft() >= 0 && r.daysLeft() <= days)
                    .toList();
        }

        @Override
        public List<RevenueByPlan> revenueByPlan(LocalDate from, LocalDate to) {
            pendingExcludedFromRevenue = true;
            lastRevenueStatusFilter = BillingStatus.PAID;
            return revenueRows;
        }

        @Override
        public PageResult<InvoiceView> findDebtPage(int page, int pageSize, String keyword,
                                                    LocalDate from, LocalDate to) {
            lastDebtOnly = true;
            return new PageResult<>(debtRows, page, pageSize, debtRows.size());
        }

        @Override
        public PageResult<MembershipView> findMembershipPage(int page, int pageSize, String keyword,
                                                             MembershipStatus status) {
            lastMembershipKeyword = keyword;
            lastMembershipStatus = status;
            List<MembershipView> filtered = allMemberships.stream()
                    .filter(m -> keyword == null || keyword.isBlank() || m.memberName().contains(keyword))
                    .filter(m -> status == null || m.status() == status)
                    .toList();
            int fromIdx = Math.max(0, (page - 1) * pageSize);
            int toIdx = Math.min(filtered.size(), fromIdx + pageSize);
            List<MembershipView> items = fromIdx >= filtered.size()
                    ? List.of()
                    : filtered.subList(fromIdx, toIdx);
            return new PageResult<>(items, page, pageSize, filtered.size());
        }

        @Override
        public PageResult<InvoiceView> findInvoicePage(int page, int pageSize, String keyword,
                                                       BillingStatus status, LocalDate from, LocalDate to) {
            List<InvoiceView> filtered = allInvoices.stream()
                    .filter(i -> keyword == null || keyword.isBlank() || i.customerName().contains(keyword))
                    .filter(i -> status == null || i.status() == status)
                    .toList();
            int fromIdx = Math.max(0, (page - 1) * pageSize);
            int toIdx = Math.min(filtered.size(), fromIdx + pageSize);
            List<InvoiceView> items = fromIdx >= filtered.size()
                    ? List.of()
                    : filtered.subList(fromIdx, toIdx);
            return new PageResult<>(items, page, pageSize, filtered.size());
        }
    }

    private static final class FakeMembershipPlanDao extends MembershipPlanDao {
        private final Map<Integer, MembershipPlan> plans = new HashMap<>();

        @Override
        protected String tableName() {
            return "MEMBERSHIP_PLAN";
        }

        @Override
        protected String idColumn() {
            return "Plan_ID";
        }

        @Override
        protected MembershipPlan mapRow(ResultSet rs) {
            return null;
        }

        @Override
        public Optional<MembershipPlan> findById(Integer id) {
            return Optional.ofNullable(plans.get(id));
        }

        @Override
        public List<MembershipPlan> findAll() {
            return new ArrayList<>(plans.values());
        }
    }

    private static final class FakeMemberDao extends MemberDao {
        private final Map<Integer, Boolean> photoFlags;

        FakeMemberDao(Map<Integer, Boolean> photoFlags) {
            this.photoFlags = photoFlags;
        }

        @Override
        protected String tableName() {
            return "MEMBER";
        }

        @Override
        protected String idColumn() {
            return "Visitor_ID";
        }

        @Override
        protected Member mapRow(ResultSet rs) {
            return null;
        }

        @Override
        public Optional<byte[]> findPhoto(int visitorId) {
            if (Boolean.TRUE.equals(photoFlags.get(visitorId))) {
                return Optional.of(new byte[]{1, 2, 3});
            }
            return Optional.empty();
        }
    }
}
