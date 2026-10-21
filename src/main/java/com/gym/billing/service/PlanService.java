package com.gym.billing.service;

import com.gym.core.Validator;
import com.gym.core.dao.MembershipPlanDao;
import com.gym.billing.dao.BillingQueryDao;
import com.gym.core.dao.PageResult;
import com.gym.core.exception.BusinessException;
import com.gym.core.model.MembershipPlan;

import java.math.BigDecimal;
import java.util.Optional;

/** Nghiệp vụ quản lý bảng giá gói tập. Không chứa Swing hoặc SQL. */
public class PlanService {

    private static final int PLAN_NAME_MAX = 50;
    private static final int DESCRIPTION_MAX = 255;
    private static final BigDecimal MAX_PRICE = new BigDecimal("99999999.99");

    private final MembershipPlanDao planDao;
    private final BillingQueryDao queryDao;

    public PlanService() {
        this(new MembershipPlanDao(), new BillingQueryDao());
    }

    // Constructor này giúp kiểm thử service dễ hơn nếu sau này cần mock DAO.
    public PlanService(MembershipPlanDao planDao) {
        this(planDao, new BillingQueryDao());
    }

    public PlanService(MembershipPlanDao planDao, BillingQueryDao queryDao) {
        this.planDao = planDao;
        this.queryDao = queryDao;
    }

    public PageResult<MembershipPlan> findPage(int page, int pageSize) {
        return planDao.findPage(page, pageSize);
    }

    public MembershipPlan create(String name, String durationText, String priceText, String description) {
        MembershipPlan plan = buildValidatedPlan(null, name, durationText, priceText, description);
        ensureUniqueName(plan.getPlanName(), null);
        return planDao.insert(plan);
    }

    public MembershipPlan update(int planId, String name, String durationText,
                                 String priceText, String description) {
        if (planId <= 0) {
            throw new BusinessException("Mã gói tập không hợp lệ.");
        }
        if (planDao.findById(planId).isEmpty()) {
            throw new BusinessException("Gói tập không còn tồn tại. Hãy tải lại danh sách.");
        }
        MembershipPlan previous = planDao.findById(planId).orElseThrow();
        MembershipPlan plan = buildValidatedPlan(planId, name, durationText, priceText, description);
        ensureUniqueName(plan.getPlanName(), planId);
        // DB không lưu snapshot giá/thời hạn trên hóa đơn. Nếu đã bán, giữ nguyên tên, thời hạn, giá
        // để lịch sử hóa đơn và báo cáo không đổi theo bảng giá hiện tại.
        if (queryDao.isPlanInUse(planId)) {
            boolean changedHistoricalFields = !previous.getPlanName().equals(plan.getPlanName())
                    || previous.getDurationMonths() != plan.getDurationMonths()
                    || previous.getPrice().compareTo(plan.getPrice()) != 0;
            if (changedHistoricalFields) {
                throw new BusinessException("Gói này đã được bán. Do CSDL không lưu snapshot giá hóa đơn, chỉ được sửa mô tả để bảo toàn lịch sử; không đổi tên, thời hạn hoặc giá.");
            }
        }
        planDao.update(plan);
        return plan;
    }

    private MembershipPlan buildValidatedPlan(Integer id, String name, String durationText,
                                               String priceText, String description) {
        Validator validator = new Validator()
                .required("Tên gói", name)
                .maxLength("Tên gói", name, PLAN_NAME_MAX)
                .required("Thời hạn (tháng)", durationText)
                .required("Giá", priceText)
                .maxLength("Mô tả", description, DESCRIPTION_MAX);
        validator.throwIfInvalid();

        int duration;
        try {
            duration = Integer.parseInt(durationText.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException("Thời hạn phải là số nguyên dương, tính theo tháng.");
        }
        if (duration <= 0) {
            throw new BusinessException("Thời hạn phải lớn hơn 0 tháng.");
        }

        BigDecimal price;
        try {
            price = new BigDecimal(priceText.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            throw new BusinessException("Giá phải là số hợp lệ, ví dụ 500000 hoặc 500000.00.");
        }
        if (price.signum() < 0) {
            throw new BusinessException("Giá không được âm.");
        }
        if (price.compareTo(MAX_PRICE) > 0 || price.scale() > 2) {
            throw new BusinessException("Giá tối đa là 99.999.999,99 và chỉ được có 2 chữ số thập phân.");
        }

        MembershipPlan plan = new MembershipPlan();
        plan.setPlanId(id);
        plan.setPlanName(name.trim());
        plan.setDurationMonths(duration);
        plan.setPrice(price.setScale(2));
        plan.setDescription(description == null || description.isBlank() ? null : description.trim());
        return plan;
    }

    private void ensureUniqueName(String name, Integer currentId) {
        Optional<MembershipPlan> existing = planDao.findByName(name);
        if (existing.isPresent() && !existing.get().getPlanId().equals(currentId)) {
            throw new BusinessException("Tên gói tập đã tồn tại. Hãy chọn tên khác.");
        }
    }
}
