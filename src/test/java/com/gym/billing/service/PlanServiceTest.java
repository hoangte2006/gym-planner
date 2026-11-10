package com.gym.billing.service;

import com.gym.billing.dao.BillingQueryDao;
import com.gym.core.dao.MembershipPlanDao;
import com.gym.core.exception.BusinessException;
import com.gym.core.model.MembershipPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TC01–TC05: tạo/sửa gói tập theo báo cáo TV3. */
class PlanServiceTest {

    private FakeMembershipPlanDao planDao;
    private FakeBillingQueryDao queryDao;
    private PlanService planService;

    @BeforeEach
    void setUp() {
        planDao = new FakeMembershipPlanDao();
        queryDao = new FakeBillingQueryDao();
        planService = new PlanService(planDao, queryDao);
    }

    @Test
    void tc01_createValidPlan_persistsNameDurationAndPrice() {
        MembershipPlan created = planService.create("Gói Gold TV3", "3", "1200000", "Gói 3 tháng");

        assertNotNull(created.getPlanId());
        assertEquals("Gói Gold TV3", created.getPlanName());
        assertEquals(3, created.getDurationMonths());
        assertEquals(0, new BigDecimal("1200000.00").compareTo(created.getPrice()));
        assertEquals(2, created.getPrice().scale());
        assertEquals(1, planDao.insertCount);
        assertEquals(1, planDao.plans.size());
    }

    @Test
    void tc02_createDuplicatePlanName_isRejected() {
        MembershipPlan existing = samplePlan(1, "Gói Gold TV3", 3, "1200000.00");
        planDao.plans.add(existing);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> planService.create("Gói Gold TV3", "3", "1500000", null));

        assertTrue(ex.getMessage().contains("Tên gói tập đã tồn tại"));
        assertEquals(0, planDao.insertCount);
        assertEquals(1, planDao.plans.size());
    }

    @Test
    void tc03_createPlanWithNonPositiveDuration_isRejected() {
        BusinessException zero = assertThrows(BusinessException.class,
                () -> planService.create("Gói Zero", "0", "500000", null));
        BusinessException negative = assertThrows(BusinessException.class,
                () -> planService.create("Gói Âm", "-1", "500000", null));

        assertTrue(zero.getMessage().contains("lớn hơn 0") || zero.getMessage().contains("số nguyên dương"));
        assertTrue(negative.getMessage().contains("lớn hơn 0") || negative.getMessage().contains("số nguyên dương"));
        assertEquals(0, planDao.insertCount);
    }

    @Test
    void tc04_createPlanWithInvalidPrice_isRejected() {
        BusinessException negative = assertThrows(BusinessException.class,
                () -> planService.create("Gói Giá Âm", "1", "-100", null));
        BusinessException tooManyDecimals = assertThrows(BusinessException.class,
                () -> planService.create("Gói Giá Lẻ", "1", "100.123", null));

        assertTrue(negative.getMessage().contains("không được âm"));
        assertTrue(tooManyDecimals.getMessage().contains("2 chữ số thập phân")
                || tooManyDecimals.getMessage().contains("99.999.999"));
        assertEquals(0, planDao.insertCount);
    }

    @Test
    void tc05_updatePlanInUse_allowsDescriptionOnly() {
        MembershipPlan existing = samplePlan(10, "Gói Đã Bán", 2, "800000.00");
        existing.setDescription("Mô tả cũ");
        planDao.plans.add(existing);
        queryDao.plansInUse.add(10);

        BusinessException blocked = assertThrows(BusinessException.class,
                () -> planService.update(10, "Tên mới", "3", "900000", "Mô tả mới"));
        assertTrue(blocked.getMessage().contains("chỉ được sửa mô tả"));
        assertEquals(0, planDao.updateCount);

        MembershipPlan updated = planService.update(10, "Gói Đã Bán", "2", "800000.00", "Mô tả mới");
        assertEquals("Mô tả mới", updated.getDescription());
        assertEquals("Gói Đã Bán", updated.getPlanName());
        assertEquals(2, updated.getDurationMonths());
        assertEquals(0, new BigDecimal("800000.00").compareTo(updated.getPrice()));
        assertEquals(1, planDao.updateCount);
    }

    private static MembershipPlan samplePlan(int id, String name, int months, String price) {
        MembershipPlan plan = new MembershipPlan();
        plan.setPlanId(id);
        plan.setPlanName(name);
        plan.setDurationMonths(months);
        plan.setPrice(new BigDecimal(price));
        return plan;
    }

    private static final class FakeMembershipPlanDao extends MembershipPlanDao {
        private final List<MembershipPlan> plans = new ArrayList<>();
        private int nextId = 1;
        private int insertCount;
        private int updateCount;

        @Override
        protected String tableName() {
            return "MEMBERSHIP_PLAN";
        }

        @Override
        protected String idColumn() {
            return "Plan_ID";
        }

        @Override
        protected MembershipPlan mapRow(ResultSet rs) throws SQLException {
            return null;
        }

        @Override
        public Optional<MembershipPlan> findById(Integer id) {
            return plans.stream().filter(p -> p.getPlanId().equals(id)).findFirst();
        }

        @Override
        public Optional<MembershipPlan> findByName(String planName) {
            return plans.stream().filter(p -> p.getPlanName().equals(planName)).findFirst();
        }

        @Override
        public MembershipPlan insert(MembershipPlan plan) {
            insertCount++;
            plan.setPlanId(nextId++);
            MembershipPlan copy = copyOf(plan);
            plans.add(copy);
            return copyOf(copy);
        }

        @Override
        public void update(MembershipPlan plan) {
            updateCount++;
            for (int i = 0; i < plans.size(); i++) {
                if (plans.get(i).getPlanId().equals(plan.getPlanId())) {
                    plans.set(i, copyOf(plan));
                    return;
                }
            }
        }

        private static MembershipPlan copyOf(MembershipPlan source) {
            MembershipPlan copy = new MembershipPlan();
            copy.setPlanId(source.getPlanId());
            copy.setPlanName(source.getPlanName());
            copy.setDurationMonths(source.getDurationMonths());
            copy.setPrice(source.getPrice());
            copy.setDescription(source.getDescription());
            return copy;
        }
    }

    private static final class FakeBillingQueryDao extends BillingQueryDao {
        private final List<Integer> plansInUse = new ArrayList<>();

        @Override
        public boolean isPlanInUse(int planId) {
            return plansInUse.contains(planId);
        }
    }
}
