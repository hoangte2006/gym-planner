package com.gym.dashboard.dao;

import com.gym.core.DBConnection;
import com.gym.core.dao.BillingLogDao;
import com.gym.core.dao.EquipmentDao;
import com.gym.core.dao.MemberDao;
import com.gym.core.dao.MembershipDao;
import com.gym.core.dao.MembershipPlanDao;
import com.gym.core.dao.RoomDao;
import com.gym.core.dao.SessionDao;
import com.gym.core.dao.StaffDao;
import com.gym.core.dao.TrainerDao;
import com.gym.core.dao.VisitorDao;
import com.gym.core.model.BillingLog;
import com.gym.core.model.BillingStatus;
import com.gym.core.model.ClassSession;
import com.gym.core.model.Equipment;
import com.gym.core.model.EquipmentStatus;
import com.gym.core.model.Member;
import com.gym.core.model.Membership;
import com.gym.core.model.MembershipPlan;
import com.gym.core.model.MembershipStatus;
import com.gym.core.model.PtSession;
import com.gym.core.model.Room;
import com.gym.core.model.Session;
import com.gym.core.model.Trainer;
import com.gym.dashboard.model.EquipmentStatusCount;
import com.gym.dashboard.model.MonthlyRevenue;
import com.gym.dashboard.service.DashboardService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Test số liệu Dashboard với DB thật. Cách kiểm chứng KHÔNG phụ thuộc lượng dữ liệu có sẵn:
 * đo số liệu trước, thêm đúng một dữ liệu đã biết, đo lại và so độ chênh (+1, +0), rồi dọn dẹp.
 * Các chỉ số khác của bạn viết theo đúng khuôn này.
 */
class DashboardDaoTest {

    private final DashboardDao dao = new DashboardDao();
    private final MemberDao memberDao = new MemberDao();
    private final MembershipDao membershipDao = new MembershipDao();
    private final VisitorDao visitorDao = new VisitorDao();

    @BeforeAll
    static void requireDatabase() {
        boolean up;
        try {
            DBConnection.test();
            up = true;
        } catch (RuntimeException e) {
            up = false;
        }
        assumeTrue(up, "Không kết nối được DB, bỏ qua kiểm thử Dashboard.");
    }

    @Test
    void activeMembersCountsOnlyActiveAndNotExpiredMemberships() {
        LocalDate today = LocalDate.now();
        MembershipPlan plan = new MembershipPlanDao().findAll().get(0);
        int before = dao.countActiveMembers(today);

        String tag = String.valueOf(System.nanoTime() % 1_000_000L);
        Member member = new Member();
        member.setFirstName("Dash");
        member.setLastName("Test");
        member.setPhone("0974" + String.format("%06d", Long.parseLong(tag)));
        member.setEmail("dash" + tag + "@test.com");
        member.setPhoto(new byte[]{1, 2, 3});           // có ảnh thì mới được mua gói (BR-MEM-03)
        memberDao.insert(member);

        Membership ms = new Membership();
        ms.setVisitorId(member.getVisitorId());
        ms.setPlanId(plan.getPlanId());
        ms.setStartDate(today.minusDays(5));
        ms.setEndDate(today.plusDays(25));
        try {
            membershipDao.insert(ms);                   // gói Active còn hạn
            assertEquals(before + 1, dao.countActiveMembers(today), "Gói Active còn hạn phải được đếm");

            ms.setStartDate(today.minusDays(40));
            ms.setEndDate(today.minusDays(10));         // vẫn Active nhưng đã quá hạn
            membershipDao.update(ms);
            assertEquals(before, dao.countActiveMembers(today), "Gói quá hạn không được đếm");

            ms.setStartDate(today.minusDays(5));
            ms.setEndDate(today.plusDays(25));
            ms.setStatus(MembershipStatus.CANCELLED);
            membershipDao.update(ms);
            assertEquals(before, dao.countActiveMembers(today), "Gói đã hủy không được đếm");
        } finally {
            if (ms.getMembershipId() != null) {
                membershipDao.deleteById(ms.getMembershipId());
            }
            visitorDao.deleteById(member.getVisitorId());
        }
    }

    @Test
    void equipmentByStatusCountsEachStatus() {
        EquipmentDao equipmentDao = new EquipmentDao();
        int before = countOf(dao.countEquipmentByStatus(), EquipmentStatus.BROKEN);
        int otherBefore = countOf(dao.countEquipmentByStatus(), EquipmentStatus.ACTIVE);

        Equipment eq = new Equipment();
        eq.setEquipmentName("ZZ-Dash-" + System.nanoTime() % 1_000_000L);
        eq.setOperationalStatus(EquipmentStatus.BROKEN);
        try {
            equipmentDao.insert(eq);
            List<EquipmentStatusCount> after = dao.countEquipmentByStatus();
            assertEquals(before + 1, countOf(after, EquipmentStatus.BROKEN), "Thêm một thiết bị hỏng thì số Hỏng tăng 1");
            assertEquals(otherBefore, countOf(after, EquipmentStatus.ACTIVE), "Các trạng thái khác không đổi");
        } finally {
            if (eq.getEquipmentId() != null) {
                equipmentDao.deleteById(eq.getEquipmentId());
            }
        }
        assertEquals(before, countOf(dao.countEquipmentByStatus(), EquipmentStatus.BROKEN), "Xóa xong thì về như cũ");
    }

    @Test
    void revenueByMonthSumsOnlyPaidInvoicesOfTheMonthWithinTheRange() {
        LocalDate today = LocalDate.now();
        YearMonth thisMonth = YearMonth.from(today);
        BigDecimal before = monthTotal(dao.sumRevenueByMonth(today, 12), thisMonth);

        String tag = String.valueOf(System.nanoTime() % 1_000_000L);
        Member member = new Member();
        member.setFirstName("Rev");
        member.setLastName("Test");
        member.setPhone("0972" + String.format("%06d", Long.parseLong(tag)));
        member.setEmail("rev" + tag + "@test.com");
        memberDao.insert(member);

        BillingLogDao billingDao = new BillingLogDao();
        List<BillingLog> created = new ArrayList<>();
        try {
            // 1) Hóa đơn Paid tháng này, 123.45 -> doanh thu tháng này tăng đúng 123.45
            created.add(billingDao.insert(bill(member, BillingStatus.PAID, "123.45", LocalDateTime.now())));
            assertEquals(0, before.add(new BigDecimal("123.45"))
                    .compareTo(monthTotal(dao.sumRevenueByMonth(today, 12), thisMonth)), "Hóa đơn Paid phải được cộng");

            // 2) Hóa đơn Pending tháng này -> không tăng
            created.add(billingDao.insert(bill(member, BillingStatus.PENDING, "500.00", null)));
            assertEquals(0, before.add(new BigDecimal("123.45"))
                    .compareTo(monthTotal(dao.sumRevenueByMonth(today, 12), thisMonth)), "Hóa đơn Pending không được cộng");

            // 3) Hóa đơn Paid của 13 tháng trước (ngoài 12 tháng) -> không xuất hiện trong kết quả
            YearMonth tooOld = thisMonth.minusMonths(13);
            created.add(billingDao.insert(bill(member, BillingStatus.PAID, "999.00", tooOld.atDay(15).atTime(10, 0))));
            assertTrue(dao.sumRevenueByMonth(today, 12).stream().noneMatch(r -> r.month().equals(tooOld)),
                    "Tháng ngoài khoảng không được xuất hiện");
        } finally {
            for (BillingLog b : created) {
                billingDao.deleteById(b.getBillId());
            }
            visitorDao.deleteById(member.getVisitorId());
        }
    }

    // ====================== Thẻ 2: doanh thu tháng này ======================

    @Test
    void monthlyRevenueSumsOnlyPaidInvoicesPaidWithinThisMonth() {
        LocalDate today = LocalDate.now();
        YearMonth thisMonth = YearMonth.from(today);
        BigDecimal before = dao.sumMonthlyRevenue(today);
        Member member = createMember();
        BillingLogDao billingDao = new BillingLogDao();
        List<BillingLog> created = new ArrayList<>();
        try {
            // Paid ngay đầu tháng (00:00 ngày 1): TÍNH
            created.add(billingDao.insert(bill(member, BillingStatus.PAID, "100.00", thisMonth.atDay(1).atStartOfDay())));
            assertEquals(0, before.add(new BigDecimal("100.00")).compareTo(dao.sumMonthlyRevenue(today)),
                    "Paid đầu tháng phải được cộng");

            // Paid cuối tháng (23:59:59 ngày cuối): TÍNH
            created.add(billingDao.insert(bill(member, BillingStatus.PAID, "25.50",
                    thisMonth.atEndOfMonth().atTime(23, 59, 59))));
            BigDecimal expected = before.add(new BigDecimal("125.50"));
            assertEquals(0, expected.compareTo(dao.sumMonthlyRevenue(today)), "Paid cuối tháng phải được cộng");

            // KHÔNG tính: Paid đúng 00:00 ngày 1 tháng sau, Paid ngay cuối tháng trước, Pending, Failed
            created.add(billingDao.insert(bill(member, BillingStatus.PAID, "7.00",
                    thisMonth.plusMonths(1).atDay(1).atStartOfDay())));
            created.add(billingDao.insert(bill(member, BillingStatus.PAID, "9.00",
                    thisMonth.minusMonths(1).atEndOfMonth().atTime(23, 59, 59))));
            created.add(billingDao.insert(bill(member, BillingStatus.PENDING, "500.00", null)));
            created.add(billingDao.insert(bill(member, BillingStatus.FAILED, "300.00", null)));
            assertEquals(0, expected.compareTo(dao.sumMonthlyRevenue(today)),
                    "Tháng khác, Pending, Failed không được cộng");
        } finally {
            for (BillingLog b : created) {
                billingDao.deleteById(b.getBillId());
            }
            visitorDao.deleteById(member.getVisitorId());
        }
        assertEquals(0, before.compareTo(dao.sumMonthlyRevenue(today)), "Dọn xong thì về như cũ");
    }

    // ====================== Thẻ 3: gói sắp hết hạn ======================

    @Test
    void expiringMembershipsCountsFromTodayThroughTheLastDayOfTheRange() {
        LocalDate today = LocalDate.now();
        int days = DashboardService.EXPIRING_DAYS;
        MembershipPlan plan = new MembershipPlanDao().findAll().get(0);
        Member member = createMember();

        Membership ms = new Membership();
        ms.setVisitorId(member.getVisitorId());
        ms.setPlanId(plan.getPlanId());
        ms.setStartDate(today.minusDays(100));
        ms.setEndDate(today.plusDays(60));                    // còn rất lâu: chưa được tính
        try {
            membershipDao.insert(ms);
            int base = dao.countExpiringMemberships(today, days);

            record Case(String name, int endOffsetDays, MembershipStatus status, int expectedDelta) {
            }
            List<Case> cases = List.of(
                    new Case("hết hạn HÔM NAY (còn 0 ngày)", 0, MembershipStatus.ACTIVE, 1),
                    new Case("hết hạn sau 1 ngày", 1, MembershipStatus.ACTIVE, 1),
                    new Case("hết hạn sau ĐÚNG " + days + " ngày (ngày cuối của khoảng)", days, MembershipStatus.ACTIVE, 1),
                    new Case("hết hạn sau " + (days + 1) + " ngày (ngay ngoài khoảng)", days + 1, MembershipStatus.ACTIVE, 0),
                    new Case("đã hết hạn hôm qua", -1, MembershipStatus.ACTIVE, 0),
                    new Case("trong khoảng nhưng đã HỦY", 3, MembershipStatus.CANCELLED, 0),
                    new Case("trong khoảng nhưng trạng thái Expired", 3, MembershipStatus.EXPIRED, 0));

            for (Case c : cases) {
                ms.setEndDate(today.plusDays(c.endOffsetDays()));
                ms.setStatus(c.status());
                membershipDao.update(ms);
                assertEquals(base + c.expectedDelta(), dao.countExpiringMemberships(today, days), c.name());
            }
        } finally {
            if (ms.getMembershipId() != null) {
                membershipDao.deleteById(ms.getMembershipId());
            }
            visitorDao.deleteById(member.getVisitorId());
        }
    }

    // ====================== Thẻ 4: lớp hôm nay ======================

    @Test
    void classTodayCountsOnlyClassesStartingToday() {
        LocalDate today = LocalDate.now();
        int before = dao.countClassToday(today);

        RoomDao roomDao = new RoomDao();
        TrainerDao trainerDao = new TrainerDao();
        SessionDao sessionDao = new SessionDao();
        String tag = String.format("%07d", System.nanoTime() % 10_000_000L);

        Room room = new Room();
        room.setRoomName("ZZ-Dash-Room-" + tag);
        room.setCapacity(10);
        roomDao.insert(room);

        Trainer trainer = new Trainer();
        trainer.setFullName("ZZ Dash Trainer " + tag);
        trainer.setPhone("08" + tag + "1");
        trainer.setSalary(new BigDecimal("700"));
        trainer.setSpecialty("Yoga");
        trainerDao.insert(trainer);

        Member member = createMember();                       // cần cho buổi PT (hội viên phải có ảnh)
        List<Session> created = new ArrayList<>();
        try {
            // TÍNH: lớp lúc 10:00 hôm nay, và lớp bắt đầu đúng 00:00 hôm nay (đầu khoảng)
            created.add(sessionDao.insert(classSession(room, trainer, today.atTime(10, 0), 60)));
            created.add(sessionDao.insert(classSession(room, trainer, today.atStartOfDay(), 60)));
            assertEquals(before + 2, dao.countClassToday(today), "Hai lớp bắt đầu hôm nay phải được đếm");

            // KHÔNG tính: lớp bắt đầu đúng 00:00 ngày mai (đầu ngày sau, không gồm), lớp hôm qua, buổi PT hôm nay
            created.add(sessionDao.insert(classSession(room, trainer, today.plusDays(1).atStartOfDay(), 60)));
            created.add(sessionDao.insert(classSession(room, trainer, today.minusDays(1).atTime(22, 0), 60)));
            PtSession pt = new PtSession();
            pt.setRoomId(room.getRoomId());
            pt.setTrainerId(trainer.getStaffId());
            pt.setStartTime(today.atTime(14, 0));
            pt.setEndTime(today.atTime(15, 0));
            pt.setMemberId(member.getVisitorId());
            created.add(sessionDao.insert(pt));
            assertEquals(before + 2, dao.countClassToday(today),
                    "Lớp ngày mai, lớp hôm qua và buổi PT không được đếm");
        } finally {
            for (Session s : created) {
                sessionDao.deleteById(s.getSessionId());
            }
            visitorDao.deleteById(member.getVisitorId());
            new StaffDao().deleteById(trainer.getStaffId());
            roomDao.deleteById(room.getRoomId());
        }
        assertEquals(before, dao.countClassToday(today), "Dọn xong thì về như cũ");
    }

    /** Tạo một lớp (chưa lưu) bắt đầu lúc "start", kéo dài "minutes" phút. */
    private static ClassSession classSession(Room room, Trainer trainer, LocalDateTime start, int minutes) {
        ClassSession c = new ClassSession();
        c.setRoomId(room.getRoomId());
        c.setTrainerId(trainer.getStaffId());
        c.setStartTime(start);
        c.setEndTime(start.plusMinutes(minutes));
        c.setClassName("ZZ Dash Class");
        c.setMaxCapacity(5);
        return c;
    }

    /** Tạo và lưu một hội viên có ảnh (có ảnh thì mới được mua gói / đặt PT theo luật BR-MEM-03). */
    private Member createMember() {
        String tag = String.format("%07d", System.nanoTime() % 10_000_000L);
        Member m = new Member();
        m.setFirstName("Dash");
        m.setLastName("Test");
        m.setPhone("096" + tag);
        m.setEmail("dash" + tag + "@test.com");
        m.setPhoto(new byte[]{1, 2, 3});
        return memberDao.insert(m);
    }

    /** Tạo một hóa đơn của hội viên (chưa lưu). paymentDate = null cho hóa đơn chưa thanh toán. */
    private static BillingLog bill(Member member, BillingStatus status, String amount, LocalDateTime paymentDate) {
        BillingLog b = new BillingLog();
        b.setVisitorId(member.getVisitorId());
        b.setDueDate(LocalDate.now().plusDays(7));
        b.setAmountPaid(new BigDecimal(amount));
        b.setBillingStatus(status);
        b.setPaymentDate(paymentDate);
        return b;
    }

    /** Doanh thu của một tháng trong kết quả (0 nếu tháng đó không xuất hiện). */
    private static BigDecimal monthTotal(List<MonthlyRevenue> rows, YearMonth month) {
        return rows.stream().filter(r -> r.month().equals(month)).map(MonthlyRevenue::revenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Số thiết bị của một trạng thái trong kết quả (0 nếu trạng thái đó không xuất hiện). */
    private static int countOf(List<EquipmentStatusCount> rows, EquipmentStatus status) {
        return rows.stream().filter(r -> r.status() == status).mapToInt(EquipmentStatusCount::count).sum();
    }
}
