package com.gym.dashboard.dao;

import com.gym.core.DBConnection;
import com.gym.core.dao.BillingLogDao;
import com.gym.core.dao.ClassRegistrationDao;
import com.gym.core.dao.EquipmentDao;
import com.gym.core.dao.MaintenanceLogDao;
import com.gym.core.dao.MemberDao;
import com.gym.core.dao.MembershipDao;
import com.gym.core.dao.MembershipPlanDao;
import com.gym.core.dao.RoomDao;
import com.gym.core.dao.SessionDao;
import com.gym.core.dao.StaffDao;
import com.gym.core.dao.TrainerDao;
import com.gym.core.dao.VisitorDao;
import com.gym.core.model.AttendanceStatus;
import com.gym.core.model.BookingStatus;
import com.gym.core.model.BillingLog;
import com.gym.core.model.ClassRegistration;
import com.gym.core.model.BillingStatus;
import com.gym.core.model.ClassSession;
import com.gym.core.model.Equipment;
import com.gym.core.model.EquipmentStatus;
import com.gym.core.model.MaintenanceLog;
import com.gym.core.model.Member;
import com.gym.core.model.Membership;
import com.gym.core.model.MembershipPlan;
import com.gym.core.model.MembershipStatus;
import com.gym.core.model.PtSession;
import com.gym.core.model.Room;
import com.gym.core.model.Session;
import com.gym.core.model.Staff;
import com.gym.core.model.StaffRole;
import com.gym.core.model.Trainer;
import com.gym.dashboard.model.BusiestClass;
import com.gym.dashboard.model.ClassOccupancy;
import com.gym.dashboard.model.DateRange;
import com.gym.dashboard.model.EquipmentStatusCount;
import com.gym.dashboard.model.MonthlyMaintenanceCost;
import com.gym.dashboard.model.MonthlyRevenue;
import com.gym.dashboard.model.TrainerSessionCount;
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

    @Test
    void revenueRangeIncludesBothFirstAndLastDayCompletely() {
        YearMonth month = YearMonth.from(LocalDate.now()).minusMonths(2);
        DateRange range = new DateRange(month.atDay(10), month.atDay(20));          // từ 10 đến hết ngày 20
        BigDecimal before = monthTotal(dao.sumRevenueByMonth(range), month);

        Member member = createMember();
        BillingLogDao billingDao = new BillingLogDao();
        List<BillingLog> created = new ArrayList<>();
        try {
            created.add(billingDao.insert(bill(member, BillingStatus.PAID, "1.00", month.atDay(9).atTime(23, 59, 59))));   // ngay trước ngày đầu: không tính
            created.add(billingDao.insert(bill(member, BillingStatus.PAID, "2.00", month.atDay(10).atStartOfDay())));      // đúng 00:00 ngày đầu: tính
            created.add(billingDao.insert(bill(member, BillingStatus.PAID, "4.00", month.atDay(20).atTime(23, 59, 59))));  // cuối ngày cuối: tính (ngày cuối gồm cả ngày)
            created.add(billingDao.insert(bill(member, BillingStatus.PAID, "8.00", month.atDay(21).atStartOfDay())));      // 00:00 ngày sau ngày cuối: không tính
            created.add(billingDao.insert(bill(member, BillingStatus.PENDING, "16.00", null)));                             // chưa thanh toán: không tính

            assertEquals(0, before.add(new BigDecimal("6.00")).compareTo(monthTotal(dao.sumRevenueByMonth(range), month)),
                    "Chỉ 2.00 và 4.00 được cộng (tổng thêm 6.00)");
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

    // ====================== Chỉ số 5 và 6: top huấn luyện viên, lớp đông nhất ======================

    /** Khoảng ngày trong năm 2099: chắc chắn không có dữ liệu thật, nên kết quả chỉ gồm dữ liệu test (xác định được trước). */
    private static final DateRange FAR_FUTURE = new DateRange(LocalDate.of(2099, 3, 10), LocalDate.of(2099, 3, 20));

    @Test
    void topTrainersCountsSessionsInTheRangeIgnoresCancelledPtAndOrdersByCount() {
        RoomDao roomDao = new RoomDao();
        TrainerDao trainerDao = new TrainerDao();
        SessionDao sessionDao = new SessionDao();
        String tag = String.format("%07d", System.nanoTime() % 10_000_000L);

        Room room = new Room();
        room.setRoomName("ZZ-Top-Room-" + tag);
        room.setCapacity(10);
        roomDao.insert(room);
        Trainer busy = trainer("ZZ Top Busy " + tag, "08" + tag + "3");
        Trainer light = trainer("ZZ Top Light " + tag, "08" + tag + "4");
        trainerDao.insert(busy);
        trainerDao.insert(light);
        Member member = createMember();

        List<Session> created = new ArrayList<>();
        try {
            LocalDate from = FAR_FUTURE.from();
            // "busy": 3 lớp, gồm đúng 00:00 ngày đầu và cuối ngày cuối (đều tính)
            created.add(sessionDao.insert(classSession(room, busy, from.atStartOfDay(), 60)));
            created.add(sessionDao.insert(classSession(room, busy, from.plusDays(2).atTime(10, 0), 60)));
            created.add(sessionDao.insert(classSession(room, busy, FAR_FUTURE.to().atTime(23, 0), 30)));
            // "light": 1 lớp + 1 buổi PT còn hiệu lực (tính) + 1 buổi PT đã hủy (không tính) => 2
            created.add(sessionDao.insert(classSession(room, light, from.plusDays(3).atTime(9, 0), 60)));
            created.add(sessionDao.insert(pt(room, light, member, from.plusDays(4).atTime(9, 0), BookingStatus.SCHEDULED)));
            created.add(sessionDao.insert(pt(room, light, member, from.plusDays(5).atTime(9, 0), BookingStatus.CANCELLED)));
            // ngoài khoảng: 00:00 ngày sau ngày cuối, và ngay trước ngày đầu (không tính)
            created.add(sessionDao.insert(classSession(room, busy, FAR_FUTURE.to().plusDays(1).atStartOfDay(), 60)));
            created.add(sessionDao.insert(classSession(room, busy, from.minusDays(1).atTime(23, 0), 30)));

            List<TrainerSessionCount> top = dao.topTrainersBySessions(FAR_FUTURE, 10);
            assertEquals(List.of("ZZ Top Busy " + tag, "ZZ Top Light " + tag),
                    top.stream().map(TrainerSessionCount::trainerName).toList(), "Xếp từ nhiều đến ít");
            assertEquals(3, top.get(0).sessions(), "busy có 3 buổi trong khoảng (kể cả 2 buổi sát hai đầu)");
            assertEquals(2, top.get(1).sessions(), "light: 1 lớp + 1 PT còn hiệu lực, PT đã hủy không tính");

            assertEquals(1, dao.topTrainersBySessions(FAR_FUTURE, 1).size(), "Giới hạn số dòng trả về");
            assertEquals("ZZ Top Busy " + tag, dao.topTrainersBySessions(FAR_FUTURE, 1).get(0).trainerName());
        } finally {
            for (Session s : created) {
                sessionDao.deleteById(s.getSessionId());
            }
            visitorDao.deleteById(member.getVisitorId());
            new StaffDao().deleteById(busy.getStaffId());
            new StaffDao().deleteById(light.getStaffId());
            roomDao.deleteById(room.getRoomId());
        }
        assertTrue(dao.topTrainersBySessions(FAR_FUTURE, 10).isEmpty(), "Dọn xong thì khoảng này lại trống");
    }

    @Test
    void busiestClassesRanksByRegistrationsIgnoringCancelledAndAppliesTheLimit() {
        RoomDao roomDao = new RoomDao();
        TrainerDao trainerDao = new TrainerDao();
        SessionDao sessionDao = new SessionDao();
        ClassRegistrationDao registrationDao = new ClassRegistrationDao();
        String tag = String.format("%07d", System.nanoTime() % 10_000_000L);

        Room room = new Room();
        room.setRoomName("ZZ-Busy-Room-" + tag);
        room.setCapacity(10);
        roomDao.insert(room);
        Trainer trainer = trainer("ZZ Busy Trainer " + tag, "08" + tag + "5");
        trainerDao.insert(trainer);

        List<Member> members = new ArrayList<>();
        List<Membership> memberships = new ArrayList<>();
        List<Session> created = new ArrayList<>();
        try {
            for (int i = 0; i < 3; i++) {
                Member m = createMember();
                members.add(m);
                memberships.add(giveActiveMembership(m));     // luật BR-SCH-05: cần gói đang hoạt động mới đăng ký lớp
            }
            LocalDate from = FAR_FUTURE.from();
            ClassSession crowded = classSession(room, trainer, from.plusDays(1).atTime(18, 0), 60);
            crowded.setClassName("ZZ Crowded");
            ClassSession quiet = classSession(room, trainer, from.plusDays(2).atTime(18, 0), 60);
            quiet.setClassName("ZZ Quiet");
            ClassSession outside = classSession(room, trainer, FAR_FUTURE.to().plusDays(1).atTime(18, 0), 60);
            outside.setClassName("ZZ Outside");
            created.add(sessionDao.insert(crowded));
            created.add(sessionDao.insert(quiet));
            created.add(sessionDao.insert(outside));

            for (Member m : members) {                                         // crowded: 3 người
                register(registrationDao, crowded, m, AttendanceStatus.CONFIRMED);
            }
            register(registrationDao, quiet, members.get(0), AttendanceStatus.CONFIRMED);    // quiet: 1 người + 1 đã hủy
            register(registrationDao, quiet, members.get(1), AttendanceStatus.CANCELLED);
            for (Member m : members) {                                         // outside: đông nhất nhưng nằm ngoài khoảng
                register(registrationDao, outside, m, AttendanceStatus.CONFIRMED);
            }

            List<BusiestClass> top = dao.busiestClasses(FAR_FUTURE, 10);
            assertEquals(List.of("ZZ Crowded", "ZZ Quiet"), top.stream().map(BusiestClass::className).toList(),
                    "Chỉ 2 lớp trong khoảng, lớp đông hơn đứng trước; lớp ngoài khoảng không có");
            assertEquals(3, top.get(0).registrations());
            assertEquals(1, top.get(1).registrations(), "Đăng ký đã hủy không tính");
            assertEquals(5, top.get(0).capacity(), "Sức chứa là Max_Capacity của lớp");
            assertEquals(from.plusDays(1).atTime(18, 0), top.get(0).start());

            assertEquals(1, dao.busiestClasses(FAR_FUTURE, 1).size(), "Giới hạn số dòng trả về");
        } finally {
            for (Session s : created) {
                sessionDao.deleteById(s.getSessionId());       // xóa lớp thì đăng ký tự bị xóa theo (CASCADE)
            }
            for (Membership ms : memberships) {
                membershipDao.deleteById(ms.getMembershipId());
            }
            for (Member m : members) {
                visitorDao.deleteById(m.getVisitorId());
            }
            new StaffDao().deleteById(trainer.getStaffId());
            roomDao.deleteById(room.getRoomId());
        }
        assertTrue(dao.busiestClasses(FAR_FUTURE, 10).isEmpty(), "Dọn xong thì khoảng này lại trống");
    }

    private static Trainer trainer(String fullName, String phone) {
        Trainer t = new Trainer();
        t.setFullName(fullName);
        t.setPhone(phone);
        t.setSalary(new BigDecimal("700"));
        t.setSpecialty("Yoga");
        return t;
    }

    /** Tạo một buổi PT (chưa lưu) với trạng thái đặt chỗ cho trước. */
    private static PtSession pt(Room room, Trainer trainer, Member member, LocalDateTime start, BookingStatus status) {
        PtSession p = new PtSession();
        p.setRoomId(room.getRoomId());
        p.setTrainerId(trainer.getStaffId());
        p.setStartTime(start);
        p.setEndTime(start.plusMinutes(60));
        p.setMemberId(member.getVisitorId());
        p.setBookingStatus(status);
        return p;
    }

    // ====================== Biểu đồ 4: chi phí bảo trì theo tháng ======================

    @Test
    void maintenanceCostSumsPerMonthAndRespectsTheWindowBoundaries() {
        LocalDate today = LocalDate.now();
        int months = 3;                                                  // cửa sổ: tháng này và 2 tháng trước
        YearMonth thisMonth = YearMonth.from(today);
        YearMonth oldestIn = thisMonth.minusMonths(2);
        List<MonthlyMaintenanceCost> before = dao.sumMaintenanceCostByMonth(today, months);

        StaffDao staffDao = new StaffDao();
        Staff signer = staffDao.findByRole(StaffRole.MANAGER).get(0);    // người ký phiếu phải là Quản lý hoặc Lễ tân
        EquipmentDao equipmentDao = new EquipmentDao();
        MaintenanceLogDao logDao = new MaintenanceLogDao();

        Equipment equipment = new Equipment();
        equipment.setEquipmentName("ZZ Dash Equipment " + System.nanoTime() % 1_000_000L);
        equipment.setOperationalStatus(EquipmentStatus.ACTIVE);
        equipmentDao.insert(equipment);
        try {
            // TÍNH: một phiếu trong tháng này, một phiếu đúng ngày đầu của tháng cũ nhất trong cửa sổ (đầu khoảng)
            addLog(logDao, equipment, signer, today.withDayOfMonth(1), "100.50");
            addLog(logDao, equipment, signer, oldestIn.atDay(1), "20.25");
            // KHÔNG tính: một ngày trước cửa sổ, và phiếu ngày tháng sau (ngoài tương lai)
            addLog(logDao, equipment, signer, oldestIn.atDay(1).minusDays(1), "999.00");
            addLog(logDao, equipment, signer, thisMonth.plusMonths(1).atDay(1), "888.00");

            List<MonthlyMaintenanceCost> after = dao.sumMaintenanceCostByMonth(today, months);
            assertEquals(0, costOf(before, thisMonth).add(new BigDecimal("100.50")).compareTo(costOf(after, thisMonth)),
                    "Tháng này tăng đúng 100.50");
            assertEquals(0, costOf(before, oldestIn).add(new BigDecimal("20.25")).compareTo(costOf(after, oldestIn)),
                    "Ngày đầu của tháng cũ nhất vẫn được tính (biên)");
            assertEquals(0, costOf(before, oldestIn.minusMonths(1)).compareTo(costOf(after, oldestIn.minusMonths(1))),
                    "Phiếu trước cửa sổ không được tính");
            assertEquals(0, costOf(before, thisMonth.plusMonths(1)).compareTo(costOf(after, thisMonth.plusMonths(1))),
                    "Phiếu của tháng sau (tương lai) không được tính");
        } finally {
            equipmentDao.deleteById(equipment.getEquipmentId());         // xóa thiết bị thì phiếu bảo trì tự bị xóa theo (CASCADE)
        }
        assertEquals(0, costOf(before, thisMonth).compareTo(costOf(dao.sumMaintenanceCostByMonth(today, months), thisMonth)),
                "Dọn xong thì về như cũ");
    }

    private static void addLog(MaintenanceLogDao logDao, Equipment equipment, Staff signer, LocalDate date, String cost) {
        MaintenanceLog log = new MaintenanceLog();
        log.setEquipmentId(equipment.getEquipmentId());
        log.setMaintenanceDate(date);
        log.setCost(new BigDecimal(cost));
        log.setDescription("ZZ dashboard test");
        log.setLoggedBy(signer.getStaffId());
        logDao.insert(log);
    }

    /** Chi phí của một tháng trong kết quả (0 nếu tháng đó không xuất hiện). */
    private static BigDecimal costOf(List<MonthlyMaintenanceCost> rows, YearMonth month) {
        return rows.stream().filter(r -> r.month().equals(month)).map(MonthlyMaintenanceCost::cost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ====================== Biểu đồ 3: lấp đầy lớp theo ngày trong tuần ======================

    @Test
    void classOccupancyCountsSeatsAndCapacityPerDayAndSkipsCancelled() {
        LocalDate weekStart = DashboardService.mondayOf(LocalDate.now());
        LocalDate wednesday = weekStart.plusDays(2);
        ClassOccupancy before = occupancyOf(dao.classOccupancyByDay(weekStart), wednesday);
        ClassOccupancy beforeNextWeekMonday = occupancyOf(dao.classOccupancyByDay(weekStart.plusDays(7)), weekStart.plusDays(7));

        RoomDao roomDao = new RoomDao();
        TrainerDao trainerDao = new TrainerDao();
        SessionDao sessionDao = new SessionDao();
        ClassRegistrationDao registrationDao = new ClassRegistrationDao();
        String tag = String.format("%07d", System.nanoTime() % 10_000_000L);

        Room room = new Room();
        room.setRoomName("ZZ-Occ-Room-" + tag);
        room.setCapacity(10);
        roomDao.insert(room);

        Trainer trainer = new Trainer();
        trainer.setFullName("ZZ Occ Trainer " + tag);
        trainer.setPhone("08" + tag + "2");
        trainer.setSalary(new BigDecimal("700"));
        trainer.setSpecialty("Yoga");
        trainerDao.insert(trainer);

        List<Member> members = new ArrayList<>();
        List<Membership> memberships = new ArrayList<>();
        List<Session> created = new ArrayList<>();
        try {
            for (int i = 0; i < 3; i++) {
                Member m = createMember();
                members.add(m);
                memberships.add(giveActiveMembership(m));      // luật BR-SCH-05: chỉ hội viên có gói đang hoạt động mới đăng ký lớp
            }
            // Lớp sức chứa 5 vào thứ Tư: 2 người xác nhận + 1 người đã hủy -> chỉ 2 chỗ được tính
            Session wed = sessionDao.insert(classSession(room, trainer, wednesday.atTime(10, 0), 60));
            created.add(wed);
            register(registrationDao, wed, members.get(0), AttendanceStatus.CONFIRMED);
            register(registrationDao, wed, members.get(1), AttendanceStatus.ATTENDED);
            register(registrationDao, wed, members.get(2), AttendanceStatus.CANCELLED);

            ClassOccupancy after = occupancyOf(dao.classOccupancyByDay(weekStart), wednesday);
            assertEquals(before.capacity() + 5, after.capacity(), "Sức chứa ngày thứ Tư tăng đúng 5");
            assertEquals(before.seatsTaken() + 2, after.seatsTaken(), "Chỉ 2 chỗ được tính, đăng ký đã hủy bị bỏ qua");

            // Biên của tuần: 00:00 thứ Hai đầu tuần thì TÍNH; 00:00 thứ Hai tuần sau thì KHÔNG tính vào tuần này
            ClassOccupancy mondayBefore = occupancyOf(dao.classOccupancyByDay(weekStart), weekStart);
            created.add(sessionDao.insert(classSession(room, trainer, weekStart.atStartOfDay(), 60)));
            created.add(sessionDao.insert(classSession(room, trainer, weekStart.plusDays(7).atStartOfDay(), 60)));
            List<ClassOccupancy> week = dao.classOccupancyByDay(weekStart);
            assertEquals(mondayBefore.capacity() + 5, occupancyOf(week, weekStart).capacity(),
                    "Lớp lúc 00:00 thứ Hai đầu tuần phải được tính");
            assertTrue(week.stream().noneMatch(r -> r.date().equals(weekStart.plusDays(7))),
                    "Tuần này không được chứa ngày của tuần sau");
            assertEquals(beforeNextWeekMonday.capacity() + 5,
                    occupancyOf(dao.classOccupancyByDay(weekStart.plusDays(7)), weekStart.plusDays(7)).capacity(),
                    "Lớp 00:00 thứ Hai tuần sau thuộc về tuần sau");
        } finally {
            for (Session s : created) {
                sessionDao.deleteById(s.getSessionId());     // xóa lớp thì đăng ký tự bị xóa theo (CASCADE)
            }
            for (Membership ms : memberships) {
                membershipDao.deleteById(ms.getMembershipId());
            }
            for (Member m : members) {
                visitorDao.deleteById(m.getVisitorId());
            }
            new StaffDao().deleteById(trainer.getStaffId());
            roomDao.deleteById(room.getRoomId());
        }
        ClassOccupancy cleaned = occupancyOf(dao.classOccupancyByDay(weekStart), wednesday);
        assertEquals(before.capacity(), cleaned.capacity(), "Dọn xong thì về như cũ");
        assertEquals(before.seatsTaken(), cleaned.seatsTaken(), "Dọn xong thì về như cũ");
    }

    /** Cấp cho hội viên một gói đang hoạt động (từ hôm qua đến 30 ngày sau). */
    private Membership giveActiveMembership(Member member) {
        Membership ms = new Membership();
        ms.setVisitorId(member.getVisitorId());
        ms.setPlanId(new MembershipPlanDao().findAll().get(0).getPlanId());
        ms.setStartDate(LocalDate.now().minusDays(1));
        ms.setEndDate(LocalDate.now().plusDays(30));
        return membershipDao.insert(ms);
    }

    private static void register(ClassRegistrationDao dao, Session session, Member member, AttendanceStatus status) {
        ClassRegistration r = new ClassRegistration();
        r.setSessionId(session.getSessionId());
        r.setVisitorId(member.getVisitorId());
        r.setAttendanceStatus(status);
        dao.insert(r);
    }

    /** Mức lấp đầy của một ngày trong kết quả (0/0 nếu ngày đó không có dòng). */
    private static ClassOccupancy occupancyOf(List<ClassOccupancy> rows, LocalDate day) {
        return rows.stream().filter(r -> r.date().equals(day)).findFirst().orElse(new ClassOccupancy(day, 0, 0));
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
