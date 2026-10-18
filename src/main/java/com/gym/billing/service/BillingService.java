package com.gym.billing.service;

import com.gym.billing.dao.BillingQueryDao;
import com.gym.billing.dao.BillingTransactionDao;
import com.gym.billing.model.CancellationResult;
import com.gym.billing.model.ExpiringMembershipView;
import com.gym.billing.model.InvoiceView;
import com.gym.billing.model.MemberChoice;
import com.gym.billing.model.MembershipView;
import com.gym.billing.model.RenewalBase;
import com.gym.billing.model.RevenueByPlan;
import com.gym.billing.model.SalePreview;
import com.gym.core.dao.MemberDao;
import com.gym.core.dao.MembershipPlanDao;
import com.gym.core.dao.PageResult;
import com.gym.core.exception.BusinessException;
import com.gym.core.model.BillingStatus;
import com.gym.core.model.MembershipPlan;
import com.gym.core.model.MembershipStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Quy tắc nghiệp vụ TV3; UI chỉ được gọi service, không truy cập DAO trực tiếp. */
public class BillingService {

    private final BillingTransactionDao transactionDao;
    private final BillingQueryDao queryDao;
    private final MembershipPlanDao planDao;
    private final MemberDao memberDao;

    public BillingService() {
        this(new BillingTransactionDao(), new BillingQueryDao(), new MembershipPlanDao(), new MemberDao());
    }

    public BillingService(BillingTransactionDao transactionDao, BillingQueryDao queryDao,
                          MembershipPlanDao planDao, MemberDao memberDao) {
        this.transactionDao = transactionDao;
        this.queryDao = queryDao;
        this.planDao = planDao;
        this.memberDao = memberDao;
    }

    public List<MembershipPlan> findPlans() { return planDao.findAll(); }

    public List<MemberChoice> searchMembers(String keyword) { return queryDao.searchMembers(keyword); }

    public Optional<byte[]> findMemberPhoto(int visitorId) { return memberDao.findPhoto(visitorId); }

    public SalePreview previewSale(int visitorId, int planId, boolean renewing, LocalDate today) {
        if (visitorId <= 0 || planId <= 0 || today == null) {
            throw new BusinessException("Hãy chọn hội viên, gói tập và ngày giao dịch hợp lệ.");
        }
        MembershipPlan plan = planDao.findById(planId)
                .orElseThrow(() -> new BusinessException("Gói tập không còn tồn tại. Hãy tải lại bảng giá."));
        LocalDate start = today;
        if (renewing) {
            RenewalBase base = queryDao.findRenewalBase(visitorId).orElseThrow(() ->
                    new BusinessException("Hội viên chưa có gói đang hiệu lực hoặc gói đã hết hạn để gia hạn. Hãy chọn Bán gói mới."));
            start = BillingDateUtils.calculateRenewalStartDate(today, base.endDate());
        }
        LocalDate end = BillingDateUtils.calculateEndDate(start, plan.getDurationMonths());
        return new SalePreview(start, end, BillingRules.normalizeMoney(plan.getPrice()),
                plan.getPlanName(), plan.getDurationMonths());
    }

    public BillingTransactionDao.SaleResult sellPlan(int visitorId, int planId) {
        return sellPlan(visitorId, planId, LocalDate.now());
    }

    public BillingTransactionDao.SaleResult sellPlan(int visitorId, int planId, LocalDate today) {
        return transactionDao.sellMembership(visitorId, planId, today);
    }

    public BillingTransactionDao.SaleResult renewPlan(int visitorId, int planId) {
        return renewPlan(visitorId, planId, LocalDate.now());
    }

    public BillingTransactionDao.SaleResult renewPlan(int visitorId, int planId, LocalDate today) {
        return transactionDao.renewMembership(visitorId, planId, today);
    }

    public PageResult<InvoiceView> findInvoices(int page, int pageSize, String keyword,
                                                 BillingStatus status, LocalDate from, LocalDate to) {
        return queryDao.findInvoicePage(page, pageSize, keyword, status, from, to);
    }

    public PageResult<InvoiceView> findDebts(int page, int pageSize, String keyword, LocalDate from, LocalDate to) {
        return queryDao.findDebtPage(page, pageSize, keyword, from, to);
    }

    public Optional<InvoiceView> findInvoice(int billId) { return queryDao.findInvoiceById(billId); }

    public void markInvoicePaid(int billId) { transactionDao.markInvoicePaid(billId); }

    public void markInvoiceFailed(int billId) { transactionDao.markInvoiceFailed(billId); }

    public void retryFailedInvoice(int billId) { transactionDao.retryFailedInvoice(billId); }

    public PageResult<MembershipView> findMemberships(int page, int pageSize,
                                                       String keyword, MembershipStatus status) {
        return queryDao.findMembershipPage(page, pageSize, keyword, status);
    }

    public CancellationResult previewCancellation(int membershipId) {
        return transactionDao.previewCancellation(membershipId, LocalDateTime.now());
    }

    public CancellationResult cancelMembership(int membershipId) {
        return transactionDao.cancelMembership(membershipId, LocalDateTime.now());
    }

    public List<ExpiringMembershipView> findExpiring(int days) {
        if (days != 7 && days != 30) throw new BusinessException("Chỉ hỗ trợ cảnh báo trong 7 hoặc 30 ngày.");
        return queryDao.findExpiringMemberships(LocalDate.now(), days);
    }

    public List<RevenueByPlan> revenueByPlan(LocalDate from, LocalDate to) {
        return queryDao.revenueByPlan(from, to);
    }

    /** Trả số dòng đã chuyển sang Expired; gọi từ scheduler và nút hỗ trợ kiểm thử. */
    public int expirePastMemberships(LocalDate today) {
        return transactionDao.expirePastMemberships(today);
    }
}
