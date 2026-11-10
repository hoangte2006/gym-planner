package com.gym.billing;

import com.gym.core.DBConnection;
import com.gym.core.ErrorTranslator;
import com.gym.core.dao.BillingLogDao;
import com.gym.core.dao.MemberDao;
import com.gym.core.dao.MembershipDao;
import com.gym.core.dao.MembershipPlanDao;
import com.gym.core.dao.VisitorDao;
import com.gym.core.exception.DataAccessException;
import com.gym.core.model.BillingLog;
import com.gym.core.model.Member;
import com.gym.core.model.Membership;
import com.gym.core.model.MembershipPlan;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Deque;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** TC09: trigger BR-MEM-07 thật — hóa đơn sai chủ Membership bị chặn mã 50006 và rollback. */
class BillingBrMem07IntegrationTest {

    private final MembershipPlanDao planDao = new MembershipPlanDao();
    private final MemberDao memberDao = new MemberDao();
    private final VisitorDao visitorDao = new VisitorDao();
    private final MembershipDao membershipDao = new MembershipDao();
    private final BillingLogDao billingDao = new BillingLogDao();

    @BeforeAll
    static void requireDatabase() {
        boolean up;
        try {
            DBConnection.test();
            up = true;
        } catch (RuntimeException e) {
            up = false;
        }
        assumeTrue(up, "Không kết nối được DB, bỏ qua TC09 integration.");
    }

    @Test
    void tc09_mismatchedBillingVisitor_isBlockedByTrigger50006() {
        long base = System.nanoTime() % 1_000_000L;
        String tag = String.valueOf(base);
        Deque<Runnable> cleanup = new ArrayDeque<>();
        try {
            MembershipPlan plan = new MembershipPlan();
            plan.setPlanName("ZZ-BR07-Plan-" + tag);
            plan.setDurationMonths(1);
            plan.setPrice(new BigDecimal("100000.00"));
            planDao.insert(plan);
            cleanup.push(() -> planDao.deleteById(plan.getPlanId()));

            Member owner = new Member();
            owner.setFirstName("Owner");
            owner.setLastName("Br07");
            owner.setPhone(phone(base, 1));
            owner.setEmail("owner" + tag + "@test.com");
            owner.setBirthDate(LocalDate.of(1995, 1, 1));
            owner.setPhoto(new byte[]{1, 2, 3});
            memberDao.insert(owner);
            cleanup.push(() -> visitorDao.deleteById(owner.getVisitorId()));

            Member other = new Member();
            other.setFirstName("Other");
            other.setLastName("Br07");
            other.setPhone(phone(base, 2));
            other.setEmail("other" + tag + "@test.com");
            other.setBirthDate(LocalDate.of(1996, 2, 2));
            other.setPhoto(new byte[]{4, 5, 6});
            memberDao.insert(other);
            cleanup.push(() -> visitorDao.deleteById(other.getVisitorId()));

            Membership membership = new Membership();
            membership.setVisitorId(owner.getVisitorId());
            membership.setPlanId(plan.getPlanId());
            membership.setStartDate(LocalDate.now());
            membership.setEndDate(LocalDate.now().plusMonths(1));
            membershipDao.insert(membership);
            cleanup.push(() -> membershipDao.deleteById(membership.getMembershipId()));

            int billsBefore = billingDao.findByVisitor(other.getVisitorId()).size()
                    + billingDao.findByVisitor(owner.getVisitorId()).size();

            BillingLog badBill = new BillingLog();
            badBill.setVisitorId(other.getVisitorId());
            badBill.setMembershipId(membership.getMembershipId());
            badBill.setDueDate(LocalDate.now().plusDays(7));
            badBill.setAmountPaid(BigDecimal.ZERO);

            DataAccessException ex = assertThrows(DataAccessException.class, () -> billingDao.insert(badBill));
            String message = ErrorTranslator.toMessage(ex);
            assertTrue(message.contains("chủ của gói tập") || message.contains("Hóa đơn phải thuộc"), message);
            assertEquals(50006, findSqlErrorCode(ex));

            int billsAfter = billingDao.findByVisitor(other.getVisitorId()).size()
                    + billingDao.findByVisitor(owner.getVisitorId()).size();
            assertEquals(billsBefore, billsAfter, "Giao dịch tạo hóa đơn sai chủ không được ghi dở");
        } finally {
            while (!cleanup.isEmpty()) {
                try {
                    cleanup.pop().run();
                } catch (RuntimeException ignored) {
                    // best-effort cleanup
                }
            }
        }
    }

    private static String phone(long base, int suffix) {
        return "08" + String.format("%07d", (base * 10 + suffix) % 10_000_000L);
    }

    private static int findSqlErrorCode(Throwable error) {
        Throwable t = error;
        while (t != null) {
            if (t instanceof java.sql.SQLException sql) {
                return sql.getErrorCode();
            }
            t = t.getCause() == t ? null : t.getCause();
        }
        return -1;
    }
}
