package com.gym.core;

import com.gym.core.dao.BillingLogDao;
import com.gym.core.dao.DailyPassTicketDao;
import com.gym.core.dao.EquipmentDao;
import com.gym.core.dao.MemberDao;
import com.gym.core.dao.RoomDao;
import com.gym.core.dao.VisitorDao;
import com.gym.core.exception.DataAccessException;
import com.gym.core.model.BillingLog;
import com.gym.core.model.DailyPassTicket;
import com.gym.core.model.Equipment;
import com.gym.core.model.Member;
import com.gym.core.model.Room;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Kiểm thử Tx: giao dịch chung cho nhiều DAO (cần DB thật; tự bỏ qua nếu không nối được). */
class TxTest {

    private final RoomDao roomDao = new RoomDao();
    private final EquipmentDao equipmentDao = new EquipmentDao();
    private final DailyPassTicketDao passDao = new DailyPassTicketDao();
    private final BillingLogDao billingDao = new BillingLogDao();
    private final MemberDao memberDao = new MemberDao();
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
        assumeTrue(up, "Không kết nối được DB, bỏ qua kiểm thử Tx.");
    }

    private static String tag() {
        return String.valueOf(System.nanoTime() % 10_000_000L);
    }

    private static Room newRoom(String name) {
        Room r = new Room();
        r.setRoomName(name);
        r.setCapacity(10);
        return r;
    }

    @Test
    void notActiveOutsideTransaction() {
        assertFalse(Tx.isActive());
        assertEquals(null, Tx.current());
    }

    @Test
    void commitsAllWorkOfSeveralDaosWhenNothingFails() {
        Room room = newRoom("ZZ-Tx-Room-" + tag());
        Equipment eq = new Equipment();
        eq.setEquipmentName("ZZ-Tx-Equip-" + tag());
        try {
            Tx.run(() -> {
                roomDao.insert(room);                    // DAO thứ nhất
                eq.setRoomId(room.getRoomId());
                equipmentDao.insert(eq);                 // DAO thứ hai, cùng giao dịch
            });
            assertTrue(roomDao.findById(room.getRoomId()).isPresent());
            assertTrue(equipmentDao.findById(eq.getEquipmentId()).isPresent());
        } finally {
            if (eq.getEquipmentId() != null) {
                equipmentDao.deleteById(eq.getEquipmentId());
            }
            if (room.getRoomId() != null) {
                roomDao.deleteById(room.getRoomId());
            }
        }
    }

    @Test
    void rollsBackEarlierDaoWhenALaterDaoFails_sellDailyPass() {
        String tag = tag();
        String phone = "0977" + String.format("%06d", Long.parseLong(tag) % 1_000_000L);
        DailyPassTicket ticket = new DailyPassTicket();
        ticket.setFirstName("Tx");
        ticket.setLastName("Test");
        ticket.setPhone(phone);

        BillingLog badBill = new BillingLog();
        badBill.setDueDate(LocalDate.now());
        badBill.setAmountPaid(new BigDecimal("15.00"));
        badBill.setMembershipId(999_999_999);            // gói không tồn tại -> khóa ngoại từ chối

        DataAccessException error = assertThrows(DataAccessException.class, () -> Tx.run(() -> {
            passDao.insert(ticket);                      // bước 1 thành công
            badBill.setVisitorId(ticket.getVisitorId());
            billingDao.insert(badBill);                  // bước 2 lỗi
        }));
        assertNotNull(error.getCause());

        // Khách và vé ở bước 1 phải biến mất theo
        assertFalse(visitorDao.existsPhone(phone), "Khách vé ngày không được còn sau khi hóa đơn lỗi");
    }

    @Test
    void withoutTxTheEarlierDaoStaysSaved_provesWhyTxIsNeeded() {
        String phone = "0976" + String.format("%06d", Long.parseLong(tag()) % 1_000_000L);
        DailyPassTicket ticket = new DailyPassTicket();
        ticket.setFirstName("NoTx");
        ticket.setLastName("Test");
        ticket.setPhone(phone);
        BillingLog badBill = new BillingLog();
        badBill.setDueDate(LocalDate.now());
        badBill.setMembershipId(999_999_999);
        try {
            assertThrows(DataAccessException.class, () -> {
                passDao.insert(ticket);                  // không có Tx: đã chốt ngay
                badBill.setVisitorId(ticket.getVisitorId());
                billingDao.insert(badBill);
            });
            assertTrue(visitorDao.existsPhone(phone), "Không có Tx thì bước 1 vẫn nằm lại trong DB");
        } finally {
            if (ticket.getVisitorId() != null) {
                visitorDao.deleteById(ticket.getVisitorId());
            }
        }
    }

    @Test
    void readsItsOwnUncommittedWritesInsideTheTransaction() {
        String name = "ZZ-Tx-Read-" + tag();
        AtomicBoolean seenInside = new AtomicBoolean();
        assertThrows(IllegalStateException.class, () -> Tx.run(() -> {
            roomDao.insert(newRoom(name));
            seenInside.set(roomDao.findByName(name).isPresent());   // thấy được dữ liệu chưa chốt của chính mình
            throw new IllegalStateException("ép hủy giao dịch");
        }));
        assertTrue(seenInside.get(), "Trong Tx phải đọc lại được thứ vừa ghi");
        assertFalse(roomDao.findByName(name).isPresent(), "Sau khi hủy thì phòng không còn");
    }

    @Test
    void daoOwnTransactionJoinsTheOuterOne() {
        // MemberDao.insert tự dùng một transaction hai bảng (VISITOR + MEMBER); bên trong Tx nó phải gộp vào Tx ngoài
        String phone = "0975" + String.format("%06d", Long.parseLong(tag()) % 1_000_000L);
        Member m = new Member();
        m.setFirstName("Join");
        m.setLastName("Test");
        m.setPhone(phone);
        m.setEmail("join" + tag() + "@test.com");
        assertThrows(IllegalStateException.class, () -> Tx.run(() -> {
            memberDao.insert(m);
            throw new IllegalStateException("ép hủy giao dịch");
        }));
        assertFalse(visitorDao.existsPhone(phone), "Hội viên đã ghi trong Tx phải bị hủy theo Tx ngoài");
    }

    @Test
    void nestedTxJoinsTheOutermostOne() {
        String name = "ZZ-Tx-Nested-" + tag();
        assertThrows(IllegalStateException.class, () -> Tx.run(() -> {
            Tx.run(() -> roomDao.insert(newRoom(name)));   // Tx lồng: không tự chốt
            assertTrue(Tx.isActive(), "Tx lồng không được kết thúc giao dịch ngoài");
            throw new IllegalStateException("ép hủy giao dịch");
        }));
        assertFalse(roomDao.findByName(name).isPresent(), "Tx ngoài hủy thì phần của Tx lồng cũng hủy");
    }

    @Test
    void callReturnsTheValueAndCommits() {
        Room room = newRoom("ZZ-Tx-Call-" + tag());
        try {
            Room saved = Tx.call(() -> roomDao.insert(room));
            assertNotNull(saved.getRoomId());
            assertTrue(roomDao.findById(saved.getRoomId()).isPresent());
        } finally {
            if (room.getRoomId() != null) {
                roomDao.deleteById(room.getRoomId());
            }
        }
    }

    @Test
    void cleansThreadStateEvenAfterFailure() {
        assertThrows(IllegalStateException.class, () -> Tx.run(() -> {
            throw new IllegalStateException("lỗi giữa chừng");
        }));
        assertFalse(Tx.isActive(), "Sau lỗi không được để sót giao dịch trên luồng");
        assertFalse(roomDao.findAll().isEmpty());          // DAO ngoài Tx vẫn tự mở kết nối và chạy bình thường
    }
}
