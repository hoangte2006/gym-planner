package com.gym.core.dao;

import com.gym.core.DBConnection;
import com.gym.core.ErrorTranslator;
import com.gym.core.exception.DataAccessException;
import com.gym.core.model.AttendanceStatus;
import com.gym.core.model.BillingLog;
import com.gym.core.model.BillingStatus;
import com.gym.core.model.BookingStatus;
import com.gym.core.model.ClassRegistration;
import com.gym.core.model.ClassRegistrationKey;
import com.gym.core.model.ClassSession;
import com.gym.core.model.DailyPassTicket;
import com.gym.core.model.Equipment;
import com.gym.core.model.EquipmentStatus;
import com.gym.core.model.MaintenanceLog;
import com.gym.core.model.MaintenanceLogKey;
import com.gym.core.model.Manager;
import com.gym.core.model.Member;
import com.gym.core.model.Membership;
import com.gym.core.model.MembershipPlan;
import com.gym.core.model.MembershipStatus;
import com.gym.core.model.PtSession;
import com.gym.core.model.Receptionist;
import com.gym.core.model.Room;
import com.gym.core.model.Session;
import com.gym.core.model.SessionEquipmentUsage;
import com.gym.core.model.SessionType;
import com.gym.core.model.ShiftType;
import com.gym.core.model.Staff;
import com.gym.core.model.StaffRole;
import com.gym.core.model.Trainer;
import com.gym.core.model.Visitor;
import com.gym.scheduling.dao.SessionDao;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.Deque;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Kiểm thử 17 DAO với DB thật (GymPlanner). Tự bỏ qua nếu không kết nối được DB.
 * Mọi dữ liệu tạo ra đều bị xóa lại ở cuối mỗi test.
 */
class DaoIntegrationTest {

    @BeforeAll
    static void requireDatabase() {
        boolean up;
        try {
            DBConnection.test();
            up = true;
        } catch (RuntimeException e) {
            up = false;
        }
        assumeTrue(up, "Không kết nối được DB, bỏ qua kiểm thử DAO.");
    }

    @Test
    void everyDaoReadsAllExistingRows() {
        // Đọc hết dữ liệu mẫu: phát hiện mapRow sai tên cột hoặc setter từ chối dữ liệu có sẵn
        assertFalse(new RoomDao().findAll().isEmpty());
        assertFalse(new MembershipPlanDao().findAll().isEmpty());
        assertFalse(new VisitorDao().findAll().isEmpty());
        assertFalse(new MemberDao().findAll().isEmpty());
        new DailyPassTicketDao().findAll();
        assertFalse(new MembershipDao().findAll().isEmpty());
        new BillingLogDao().findAll();
        assertFalse(new StaffDao().findAll().isEmpty());
        assertFalse(new TrainerDao().findAll().isEmpty());
        assertFalse(new ReceptionistDao().findAll().isEmpty());
        assertFalse(new SessionDao().findAll().isEmpty());
        assertFalse(new ClassSessionDao().findAll().isEmpty());
        assertFalse(new PtSessionDao().findAll().isEmpty());
        new ClassRegistrationDao().findAll();
        assertFalse(new EquipmentDao().findAll().isEmpty());
        assertFalse(new MaintenanceLogDao().findAll().isEmpty());
        new SessionEquipmentUsageDao().findAll();
    }

    @Test
    void pagingWorks() {
        PageResult<Member> page = new MemberDao().findPage(1, 10);
        assertEquals(10, page.items().size());
        assertTrue(page.totalItems() >= 10);
        assertTrue(page.totalPages() >= 1);
        PageResult<Member> page2 = new MemberDao().findPage(2, 10);
        assertFalse(page.items().get(0).getVisitorId().equals(page2.items().get(0).getVisitorId()));
    }

    @Test
    void polymorphicDaosReturnRightSubtype() {
        Staff manager = new StaffDao().findByRole(StaffRole.MANAGER).get(0);
        assertInstanceOf(Manager.class, manager);
        Staff trainer = new StaffDao().findByRole(StaffRole.TRAINER).get(0);
        assertInstanceOf(Trainer.class, trainer);
        assertNotNull(((Trainer) trainer).getSpecialty());

        Visitor member = new MemberDao().findPage(1, 1).items().get(0);
        assertInstanceOf(Member.class, new VisitorDao().findById(member.getVisitorId()).orElseThrow());
    }

    @Test
    void fullLifeCycleOfEveryTable() {
        RoomDao roomDao = new RoomDao();
        MembershipPlanDao planDao = new MembershipPlanDao();
        TrainerDao trainerDao = new TrainerDao();
        ReceptionistDao receptionistDao = new ReceptionistDao();
        StaffDao staffDao = new StaffDao();
        MemberDao memberDao = new MemberDao();
        DailyPassTicketDao passDao = new DailyPassTicketDao();
        VisitorDao visitorDao = new VisitorDao();
        MembershipDao membershipDao = new MembershipDao();
        BillingLogDao billingDao = new BillingLogDao();
        ClassSessionDao classDao = new ClassSessionDao();
        PtSessionDao ptDao = new PtSessionDao();
        SessionDao sessionDao = new SessionDao();
        ClassRegistrationDao regDao = new ClassRegistrationDao();
        EquipmentDao equipmentDao = new EquipmentDao();
        MaintenanceLogDao logDao = new MaintenanceLogDao();
        SessionEquipmentUsageDao usageDao = new SessionEquipmentUsageDao();

        long base = System.nanoTime() % 10_000_000L;
        String tag = String.valueOf(base);
        Deque<Runnable> cleanup = new ArrayDeque<>();     // push vào đầu, chạy từ đầu = ngược thứ tự tạo
        try {
            // ---- ROOM
            Room room = new Room();
            room.setRoomName("ZZ-Room-" + tag);
            room.setCapacity(10);
            room.setFloorLocation("Floor 9");
            roomDao.insert(room);
            cleanup.push(() -> roomDao.deleteById(room.getRoomId()));
            assertNotNull(room.getRoomId());
            room.setCapacity(25);
            roomDao.update(room);
            assertEquals(25, roomDao.findById(room.getRoomId()).orElseThrow().getCapacity());

            // ---- MEMBERSHIP_PLAN
            MembershipPlan plan = new MembershipPlan();
            plan.setPlanName("ZZ-Plan-" + tag);
            plan.setDurationMonths(1);
            plan.setPrice(new BigDecimal("99.00"));
            planDao.insert(plan);
            cleanup.push(() -> planDao.deleteById(plan.getPlanId()));
            assertEquals(0, new BigDecimal("99.00").compareTo(
                    planDao.findByName(plan.getPlanName()).orElseThrow().getPrice()));

            // ---- STAFF: Trainer, Receptionist, Manager
            Trainer trainer = new Trainer();
            trainer.setFullName("ZZ Trainer " + tag);
            trainer.setPhone("08" + String.format("%07d", base) + "1");
            trainer.setSalary(new BigDecimal("700"));
            trainer.setSpecialty("Yoga");
            trainerDao.insert(trainer);
            cleanup.push(() -> staffDao.deleteById(trainer.getStaffId()));
            Staff reread = staffDao.findById(trainer.getStaffId()).orElseThrow();
            assertInstanceOf(Trainer.class, reread);
            assertEquals(StaffRole.TRAINER, reread.getRole());
            trainer.setSpecialty("Pilates");
            trainerDao.update(trainer);
            assertEquals("Pilates", trainerDao.findById(trainer.getStaffId()).orElseThrow().getSpecialty());

            Receptionist rec = new Receptionist();
            rec.setFullName("ZZ Recep " + tag);
            rec.setPhone("08" + String.format("%07d", base) + "2");
            rec.setSalary(new BigDecimal("600"));
            rec.setShiftType(ShiftType.EVENING);
            staffDao.insert(rec);                              // đi qua lớp cha: đa hình
            cleanup.push(() -> staffDao.deleteById(rec.getStaffId()));
            assertEquals(ShiftType.EVENING, receptionistDao.findById(rec.getStaffId()).orElseThrow().getShiftType());
            assertTrue(staffDao.findByPhone(rec.getPhone()).isPresent());

            Manager manager = new Manager();
            manager.setFullName("ZZ Manager " + tag);
            manager.setPhone("08" + String.format("%07d", base) + "3");
            manager.setSalary(new BigDecimal("900"));
            staffDao.insert(manager);
            cleanup.push(() -> staffDao.deleteById(manager.getStaffId()));
            assertInstanceOf(Manager.class, staffDao.findById(manager.getStaffId()).orElseThrow());

            // ---- VISITOR: Member (có ảnh), Member (không ảnh), DailyPass
            Member member = new Member();
            member.setFirstName("Zed");
            member.setLastName("Test");
            member.setPhone("08" + String.format("%07d", base) + "4");
            member.setEmail("zz" + tag + "@test.com");
            member.setBirthDate(LocalDate.of(2000, 1, 1));
            member.setPhoto(new byte[]{1, 2, 3});
            memberDao.insert(member);
            cleanup.push(() -> visitorDao.deleteById(member.getVisitorId()));
            assertTrue(memberDao.findById(member.getVisitorId()).orElseThrow().hasPhoto());
            assertEquals(3, memberDao.findPhoto(member.getVisitorId()).orElseThrow().length);
            assertTrue(memberDao.existsEmail(member.getEmail()));
            assertEquals(1, memberDao.search(tag, 1, 10).totalItems());
            assertTrue(visitorDao.existsPhone(member.getPhone()));

            Member noPhoto = new Member();
            noPhoto.setFirstName("No");
            noPhoto.setLastName("Photo");
            noPhoto.setPhone("08" + String.format("%07d", base) + "5");
            noPhoto.setEmail("nophoto" + tag + "@test.com");
            memberDao.insert(noPhoto);
            cleanup.push(() -> visitorDao.deleteById(noPhoto.getVisitorId()));

            DailyPassTicket pass = new DailyPassTicket();
            pass.setFirstName("Day");
            pass.setLastName("Pass");
            pass.setPhone("08" + String.format("%07d", base) + "6");
            visitorDao.insert(pass);                           // đi qua lớp cha
            cleanup.push(() -> visitorDao.deleteById(pass.getVisitorId()));
            DailyPassTicket passBack = assertInstanceOf(DailyPassTicket.class,
                    visitorDao.findById(pass.getVisitorId()).orElseThrow());
            assertEquals(0, new BigDecimal("15.00").compareTo(passBack.getDailyRate()));   // giá mặc định của DB

            // Luật BR-MEM-03: hội viên chưa có ảnh không thể đăng ký gói (trigger 50003)
            Membership blocked = new Membership();
            blocked.setVisitorId(noPhoto.getVisitorId());
            blocked.setPlanId(plan.getPlanId());
            blocked.setStartDate(LocalDate.now());
            blocked.setEndDate(LocalDate.now().plusMonths(1));
            DataAccessException ex = assertThrows(DataAccessException.class, () -> membershipDao.insert(blocked));
            assertTrue(ErrorTranslator.toMessage(ex).contains("chưa có ảnh"), ErrorTranslator.toMessage(ex));

            // ---- MEMBERSHIP + BILLING_LOG
            Membership ms = new Membership();
            ms.setVisitorId(member.getVisitorId());
            ms.setPlanId(plan.getPlanId());
            ms.setStartDate(LocalDate.now());
            ms.setEndDate(LocalDate.now().plusMonths(1));
            membershipDao.insert(ms);
            cleanup.push(() -> membershipDao.deleteById(ms.getMembershipId()));
            assertEquals(MembershipStatus.ACTIVE, ms.getStatus());
            assertEquals(1, membershipDao.findByVisitor(member.getVisitorId()).size());
            assertTrue(membershipDao.findActiveEndingBetween(LocalDate.now(), LocalDate.now().plusMonths(2))
                    .stream().anyMatch(x -> x.getMembershipId().equals(ms.getMembershipId())));

            BillingLog bill = new BillingLog();
            bill.setVisitorId(member.getVisitorId());
            bill.setMembershipId(ms.getMembershipId());
            bill.setDueDate(LocalDate.now().plusDays(7));
            bill.setAmountPaid(new BigDecimal("99.00"));
            billingDao.insert(bill);
            cleanup.push(() -> billingDao.deleteById(bill.getBillId()));
            assertEquals(BillingStatus.PENDING, bill.getBillingStatus());     // mặc định của DB
            bill.setBillingStatus(BillingStatus.PAID);
            bill.setPaymentDate(LocalDateTime.now());
            billingDao.update(bill);
            assertEquals(BillingStatus.PAID, billingDao.findById(bill.getBillId()).orElseThrow().getBillingStatus());
            assertEquals(1, billingDao.findByVisitor(member.getVisitorId()).size());

            // ---- SESSION: Class + PT, đăng ký lớp
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            ClassSession cls = new ClassSession();
            cls.setRoomId(room.getRoomId());
            cls.setTrainerId(trainer.getStaffId());
            cls.setStartTime(tomorrow.atTime(LocalTime.of(10, 0)));
            cls.setEndTime(tomorrow.atTime(LocalTime.of(11, 0)));
            cls.setClassName("ZZ Yoga " + tag);
            cls.setMaxCapacity(5);
            sessionDao.insert(cls);                            // đi qua lớp cha
            cleanup.push(() -> sessionDao.deleteById(cls.getSessionId()));
            Session clsBack = sessionDao.findById(cls.getSessionId()).orElseThrow();
            assertInstanceOf(ClassSession.class, clsBack);
            assertEquals(SessionType.CLASS, clsBack.getSessionType());
            assertEquals(60, clsBack.getDurationMinutes());
            assertEquals("ZZ Yoga " + tag, clsBack.getSessionTitle());

            ClassRegistration reg = new ClassRegistration();
            reg.setSessionId(cls.getSessionId());
            reg.setVisitorId(member.getVisitorId());
            regDao.insert(reg);
            ClassRegistrationKey key = new ClassRegistrationKey(cls.getSessionId(), member.getVisitorId());
            cleanup.push(() -> regDao.deleteById(key));
            assertEquals(AttendanceStatus.CONFIRMED, regDao.findById(key).orElseThrow().getAttendanceStatus());
            reg.setAttendanceStatus(AttendanceStatus.CANCELLED);
            regDao.update(reg);
            assertEquals(AttendanceStatus.CANCELLED, regDao.findById(key).orElseThrow().getAttendanceStatus());
            assertEquals(1, regDao.findBySession(cls.getSessionId()).size());

            PtSession pt = new PtSession();
            pt.setRoomId(room.getRoomId());
            pt.setTrainerId(trainer.getStaffId());
            pt.setStartTime(tomorrow.atTime(LocalTime.of(12, 0)));
            pt.setEndTime(tomorrow.atTime(LocalTime.of(13, 0)));
            pt.setMemberId(member.getVisitorId());
            ptDao.insert(pt);
            cleanup.push(() -> ptDao.deleteById(pt.getSessionId()));
            assertEquals(BookingStatus.SCHEDULED, ptDao.findById(pt.getSessionId()).orElseThrow().getBookingStatus());
            assertEquals(1, ptDao.findByMember(member.getVisitorId()).size());
            assertEquals(2, sessionDao.findStartingBetween(tomorrow.atStartOfDay(), tomorrow.plusDays(1).atStartOfDay())
                    .stream().filter(s -> s.getRoomId().equals(room.getRoomId())).count());

            // Luật BR-SCH-03: cùng phòng, giờ trùng (trigger 50013)
            ClassSession clash = new ClassSession();
            clash.setRoomId(room.getRoomId());
            clash.setTrainerId(trainer.getStaffId());
            clash.setStartTime(tomorrow.atTime(LocalTime.of(10, 30)));
            clash.setEndTime(tomorrow.atTime(LocalTime.of(11, 30)));
            clash.setClassName("Clash");
            clash.setMaxCapacity(5);
            DataAccessException clashEx = assertThrows(DataAccessException.class, () -> classDao.insert(clash));
            assertTrue(ErrorTranslator.toMessage(clashEx).contains("Phòng đã có buổi khác"));

            // ---- EQUIPMENT + MAINTENANCE_LOG + SESSION_EQUIPMENT_USAGE
            Equipment eq = new Equipment();
            eq.setEquipmentName("ZZ-Equip-" + tag);
            eq.setRoomId(room.getRoomId());
            equipmentDao.insert(eq);
            cleanup.push(() -> equipmentDao.deleteById(eq.getEquipmentId()));
            assertEquals(EquipmentStatus.ACTIVE, eq.getOperationalStatus());
            assertEquals(1, equipmentDao.findByRoom(room.getRoomId()).size());

            MaintenanceLog log1 = new MaintenanceLog();
            log1.setEquipmentId(eq.getEquipmentId());
            log1.setDescription("Kiem tra dinh ky");
            log1.setCost(new BigDecimal("5.00"));
            log1.setLoggedBy(rec.getStaffId());
            logDao.insert(log1);
            MaintenanceLog log2 = new MaintenanceLog();
            log2.setEquipmentId(eq.getEquipmentId());
            log2.setDescription("Thay day cap");
            log2.setLoggedBy(manager.getStaffId());
            logDao.insert(log2);
            assertEquals(1, log1.getLogNo());
            assertEquals(2, log2.getLogNo());
            assertEquals(2, logDao.findByEquipment(eq.getEquipmentId()).size());
            assertEquals("Thay day cap",
                    logDao.findById(new MaintenanceLogKey(eq.getEquipmentId(), 2)).orElseThrow().getDescription());

            SessionEquipmentUsage usage = new SessionEquipmentUsage();
            usage.setEquipmentId(eq.getEquipmentId());
            usage.setSessionId(cls.getSessionId());
            usage.setReservedFrom(tomorrow.atTime(LocalTime.of(10, 15)));
            usage.setReservedTo(tomorrow.atTime(LocalTime.of(10, 45)));
            usageDao.insert(usage);
            cleanup.push(() -> usageDao.deleteById(usage.getUsageId()));
            assertEquals(1, usageDao.findBySession(cls.getSessionId()).size());
        } finally {
            RuntimeException firstFailure = null;
            while (!cleanup.isEmpty()) {
                try {
                    cleanup.pop().run();
                } catch (RuntimeException e) {
                    if (firstFailure == null) {
                        firstFailure = e;
                    }
                }
            }
            if (firstFailure != null) {
                throw firstFailure;
            }
        }
    }
}
