package com.gym.billing.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.gym.billing.model.CancellationResult;
import com.gym.billing.model.ClassImpact;
import com.gym.billing.service.BillingDateUtils;
import com.gym.billing.service.BillingRules;
import com.gym.core.dao.BillingLogDao;
import com.gym.core.dao.Sql;
import com.gym.core.exception.BusinessException;
import com.gym.core.model.BillingStatus;
import com.gym.core.model.MembershipStatus;

/**
 * Các thao tác nhiều bước của TV3. Mọi ghi nhiều bảng được thực hiện cùng
 * transaction.
 */
public class BillingTransactionDao extends BillingLogDao {

    public record SaleResult(int membershipId, int billId, int visitorId, int planId,
            LocalDate startDate, LocalDate endDate, BigDecimal planPrice,
            boolean renewed) {

    }

    private record PlanData(int durationMonths, BigDecimal price, String planName) {

    }

    private record ActiveMembership(int membershipId, LocalDate endDate) {

    }

    private record InvoiceData(BillingStatus status, LocalDate dueDate,
            LocalDateTime invoiceDate, BigDecimal totalAmount) {

    }

    private record MemberData(int visitorId, String name, String phone, MembershipStatus status) {

    }

    /**
     * Bán gói mới: không cho bán chồng lên gói Active chưa hết hạn.
     */
    public SaleResult sellMembership(int visitorId, int planId, LocalDate today) {
        validateSaleInput(visitorId, planId, today);
        return inTransaction(cn -> {
            requireMemberWithPhoto(cn, visitorId);
            PlanData plan = lockPlan(cn, planId);
            List<ActiveMembership> active = findActiveMemberships(cn, visitorId, true);
            for (ActiveMembership old : active) {
                if (old.endDate() != null && old.endDate().isBefore(today)) {
                    exec(cn, "UPDATE MEMBERSHIP SET Status = 'Expired' WHERE Membership_ID = ? AND Status = 'Active'",
                            ps -> ps.setInt(1, old.membershipId()));
                } else {
                    throw new BusinessException("Hội viên đang có gói tập còn hiệu lực đến "
                            + old.endDate() + "; hãy dùng chức năng Gia hạn thay vì bán gói mới.");
                }
            }
            return createMembershipAndInvoice(cn, visitorId, planId, plan, today, today, false);
        });
    }

    /**
     * Gia hạn: đổi gói Active cũ sang Expired trước, sau đó mới tạo gói Active
     * mới. Nếu không còn Active thì lấy gói Expired gần nhất làm mốc giữ ngày
     * chưa dùng.
     */
    public SaleResult renewMembership(int visitorId, int planId, LocalDate today) {
        validateSaleInput(visitorId, planId, today);
        return inTransaction(cn -> {
            requireMemberWithPhoto(cn, visitorId);
            PlanData plan = lockPlan(cn, planId);
            LocalDate renewalBase = today;
            List<ActiveMembership> active = findActiveMemberships(cn, visitorId, true);
            if (!active.isEmpty()) {
                ActiveMembership old = active.get(0);
                if (old.endDate() != null && old.endDate().isAfter(today)) {
                    renewalBase = old.endDate();
                }
                // Bắt buộc trước INSERT mới do UX_MEMBERSHIP_One_Active.
                int changed = exec(cn,
                        "UPDATE MEMBERSHIP SET Status = 'Expired' WHERE Membership_ID = ? AND Status = 'Active'",
                        ps -> ps.setInt(1, old.membershipId()));
                if (changed != 1) {
                    throw new BusinessException("Gói cũ vừa thay đổi. Hãy tải lại danh sách rồi thử lại.");
                }
            } else {
                boolean foundExpiredMembership = false;
                try (PreparedStatement ps = cn.prepareStatement(
                        "SELECT TOP (1) End_Date FROM MEMBERSHIP WITH (UPDLOCK, HOLDLOCK) "
                        + "WHERE Visitor_ID = ? AND Status = 'Expired' "
                        + "ORDER BY Start_Date DESC, Membership_ID DESC")) {
                    ps.setInt(1, visitorId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            foundExpiredMembership = true;
                            LocalDate previousEnd = Sql.date(rs, "End_Date");
                            if (previousEnd != null && previousEnd.isAfter(today)) {
                                renewalBase = previousEnd;
                            }
                        }
                    }
                }
                if (!foundExpiredMembership) {
                    throw new BusinessException("Hội viên chưa có gói đang hiệu lực hoặc gói đã hết hạn để gia hạn. Hãy chọn Bán gói mới.");
                }
            }
            LocalDate start = BillingDateUtils.calculateRenewalStartDate(today, renewalBase);
            return createMembershipAndInvoice(cn, visitorId, planId, plan, today, start, true);
        });
    }

    private SaleResult createMembershipAndInvoice(Connection cn, int visitorId, int planId,
            PlanData plan, LocalDate invoiceDay,
            LocalDate startDate, boolean renewed) throws SQLException {
        LocalDate endDate = BillingDateUtils.calculateEndDate(startDate, plan.durationMonths());
        if (!endDate.isAfter(startDate)) {
            throw new BusinessException("Ngày kết thúc gói phải sau ngày bắt đầu.");
        }
        int membershipId = insertKey(cn,
                "INSERT INTO MEMBERSHIP (Visitor_ID, Plan_ID, Start_Date, End_Date, Status) VALUES (?, ?, ?, ?, 'Active')",
                ps -> {
                    ps.setInt(1, visitorId);
                    ps.setInt(2, planId);
                    Sql.setDate(ps, 3, startDate);
                    Sql.setDate(ps, 4, endDate);
                });
        LocalDate dueDate = invoiceDay.plusDays(7);
        int billId = insertKey(cn,
                "INSERT INTO BILLING_LOG (Visitor_ID, Membership_ID, Invoice_Date, Due_Date, Payment_Date, Amount_Paid, Billing_Status) "
                + "VALUES (?, ?, CURRENT_TIMESTAMP, ?, NULL, 0.00, 'Pending')",
                ps -> {
                    ps.setInt(1, visitorId);
                    ps.setInt(2, membershipId);
                    Sql.setDate(ps, 3, dueDate);
                });
        return new SaleResult(membershipId, billId, visitorId, planId, startDate, endDate,
                BillingRules.normalizeMoney(plan.price()), renewed);
    }

    /**
     * Xem trước để cảnh báo đăng ký lớp tương lai còn lại khi hủy gói.
     */
    public CancellationResult previewCancellation(int membershipId, LocalDateTime now) {
        if (membershipId <= 0 || now == null) {
            throw new BusinessException("Mã gói không hợp lệ.");
        }
        return inTransaction(cn -> {
            MemberData member = readMembershipOwner(cn, membershipId, false);
            if (member.status() != MembershipStatus.ACTIVE) {
                throw new BusinessException("Chỉ có thể hủy gói đang ở trạng thái Đang hiệu lực.");
            }
            return new CancellationResult(membershipId, member.visitorId(), member.name(), member.phone(),
                    findFutureClassImpacts(cn, member.visitorId(), now));
        });
    }

    /**
     * Hủy gói Active và trả về các lớp trong tương lai để lễ tân xử lý riêng.
     */
    public CancellationResult cancelMembership(int membershipId, LocalDateTime now) {
        if (membershipId <= 0 || now == null) {
            throw new BusinessException("Mã gói không hợp lệ.");
        }
        return inTransaction(cn -> {
            MemberData member = readMembershipOwner(cn, membershipId, true);
            if (member.status() != MembershipStatus.ACTIVE) {
                throw new BusinessException("Gói này không còn ở trạng thái Đang hiệu lực.");
            }
            List<ClassImpact> impacted = findFutureClassImpacts(cn, member.visitorId(), now);
            int changed = exec(cn, "UPDATE MEMBERSHIP SET Status = 'Cancelled' "
                    + "WHERE Membership_ID = ? AND Status = 'Active'",
                    ps -> ps.setInt(1, membershipId));
            if (changed != 1) {
                throw new BusinessException("Gói vừa thay đổi bởi thao tác khác. Hãy tải lại dữ liệu.");
            }
            return new CancellationResult(membershipId, member.visitorId(), member.name(), member.phone(), impacted);
        });
    }

    /**
     * Thu đúng số tiền của hóa đơn. Không nhận số tiền từ giao diện, tránh thu
     * thiếu/thừa.
     */
    public void markInvoicePaid(int billId) {
        if (billId <= 0) {
            throw new BusinessException("Mã hóa đơn không hợp lệ.");
        }
        inTransaction(cn -> {
            InvoiceData bill = lockInvoice(cn, billId);
            BillingRules.requireCanMarkPaid(bill.status());
            BillingRules.requireDueDateNotBeforeInvoice(bill.dueDate(), bill.invoiceDate());
            BigDecimal amount = BillingRules.normalizeMoney(bill.totalAmount());
            int changed = exec(cn, "UPDATE BILLING_LOG SET Billing_Status = 'Paid', Payment_Date = CURRENT_TIMESTAMP, "
                    + "Amount_Paid = ? WHERE Bill_ID = ? AND Billing_Status = 'Pending'",
                    ps -> {
                        Sql.setDecimal(ps, 1, amount);
                        ps.setInt(2, billId);
                    });
            if (changed != 1) {
                throw new BusinessException("Hóa đơn vừa được cập nhật bởi thao tác khác. Hãy tải lại.");
            }
            return null;
        });
    }

    public void markInvoiceFailed(int billId) {
        changePendingInvoice(billId, "Failed");
    }

    /**
     * Chỉ cho chuyển Failed -> Pending; Paid là trạng thái cuối.
     */
    public void retryFailedInvoice(int billId) {
        if (billId <= 0) {
            throw new BusinessException("Mã hóa đơn không hợp lệ.");
        }
        inTransaction(cn -> {
            InvoiceData bill = lockInvoice(cn, billId);
            BillingRules.requireCanRetry(bill.status());
            int changed = exec(cn, "UPDATE BILLING_LOG SET Billing_Status = 'Pending', Payment_Date = NULL, Amount_Paid = 0.00 "
                    + "WHERE Bill_ID = ? AND Billing_Status = 'Failed'", ps -> ps.setInt(1, billId));
            if (changed != 1) {
                throw new BusinessException("Hóa đơn vừa thay đổi. Hãy tải lại dữ liệu.");
            }
            return null;
        });
    }

    private void changePendingInvoice(int billId, String targetStatus) {
        if (billId <= 0) {
            throw new BusinessException("Mã hóa đơn không hợp lệ.");
        }
        inTransaction(cn -> {
            InvoiceData bill = lockInvoice(cn, billId);
            BillingRules.requireCanMarkFailed(bill.status());
            int changed = exec(cn, "UPDATE BILLING_LOG SET Billing_Status = ?, Payment_Date = NULL, Amount_Paid = 0.00 "
                    + "WHERE Bill_ID = ? AND Billing_Status = 'Pending'",
                    ps -> {
                        ps.setString(1, targetStatus);
                        ps.setInt(2, billId);
                    });
            if (changed != 1) {
                throw new BusinessException("Hóa đơn vừa thay đổi. Hãy tải lại dữ liệu.");
            }
            return null;
        });
    }

    /**
     * Tác vụ an toàn, một câu UPDATE; trạng thái Active có End_Date < hôm nay
     * chuyển sang Expired.
     */
    public int expirePastMemberships(LocalDate today) {
        if (today == null) {
            throw new BusinessException("Ngày chạy tác vụ hết hạn không hợp lệ.");
        }
        return execute("UPDATE MEMBERSHIP SET Status = 'Expired' WHERE Status = 'Active' AND End_Date < ?",
                ps -> Sql.setDate(ps, 1, today));
    }

    private void requireMemberWithPhoto(Connection cn, int visitorId) throws SQLException {
        int memberCount = queryInt(cn,
                "SELECT COUNT(*) FROM MEMBER WITH (UPDLOCK, HOLDLOCK) WHERE Visitor_ID = ?",
                ps -> ps.setInt(1, visitorId));
        if (memberCount == 0) {
            throw new BusinessException("Không tìm thấy hội viên. Hãy tải lại danh sách.");
        }
        int hasPhoto = queryInt(cn,
                "SELECT CASE WHEN Photo IS NULL THEN 0 ELSE 1 END FROM MEMBER WITH (UPDLOCK, HOLDLOCK) WHERE Visitor_ID = ?",
                ps -> ps.setInt(1, visitorId));
        if (hasPhoto == 0) {
            throw new BusinessException("Hội viên chưa có ảnh. Hãy mở Hồ sơ hội viên → Thêm ảnh ngay trước khi bán/gia hạn gói.");
        }
    }

    private PlanData lockPlan(Connection cn, int planId) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(
                "SELECT Plan_Name, Duration_Months, Price FROM MEMBERSHIP_PLAN WITH (UPDLOCK, HOLDLOCK) WHERE Plan_ID = ?")) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new BusinessException("Gói tập không còn tồn tại. Hãy tải lại danh sách.");
                }
                int months = rs.getInt("Duration_Months");
                BigDecimal price = rs.getBigDecimal("Price");
                if (months <= 0 || price == null || price.signum() < 0) {
                    throw new BusinessException("Thông tin thời hạn hoặc giá gói không hợp lệ.");
                }
                return new PlanData(months, price, rs.getString("Plan_Name"));
            }
        }
    }

    private List<ActiveMembership> findActiveMemberships(Connection cn, int visitorId, boolean lock) throws SQLException {
        String hint = lock ? " WITH (UPDLOCK, HOLDLOCK) " : " ";
        List<ActiveMembership> found = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement("SELECT Membership_ID, End_Date FROM MEMBERSHIP" + hint
                + "WHERE Visitor_ID = ? AND Status = 'Active' ORDER BY Membership_ID")) {
            ps.setInt(1, visitorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    found.add(new ActiveMembership(rs.getInt("Membership_ID"), Sql.date(rs, "End_Date")));
                }
            }
        }
        return found;
    }

    private InvoiceData lockInvoice(Connection cn, int billId) throws SQLException {
        String sql = "SELECT b.Billing_Status, b.Invoice_Date, b.Due_Date, "
                + "CAST(COALESCE(p.Price, d.Daily_Rate) AS DECIMAL(10,2)) AS Total_Amount "
                + "FROM BILLING_LOG b WITH (UPDLOCK, HOLDLOCK) "
                + "LEFT JOIN MEMBERSHIP ms ON ms.Membership_ID = b.Membership_ID "
                + "LEFT JOIN MEMBERSHIP_PLAN p ON p.Plan_ID = ms.Plan_ID "
                + "LEFT JOIN DAILY_PASS_TICKET d ON d.Visitor_ID = b.Visitor_ID WHERE b.Bill_ID = ?";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, billId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new BusinessException("Không tìm thấy hóa đơn.");
                }
                BillingStatus status = BillingStatus.fromDb(rs.getString("Billing_Status"));
                LocalDateTime invoiceDate = Sql.dateTime(rs, "Invoice_Date");
                LocalDate dueDate = Sql.date(rs, "Due_Date");
                BigDecimal total = rs.getBigDecimal("Total_Amount");
                if (total == null) {
                    throw new BusinessException("Không xác định được số tiền hóa đơn. Hãy kiểm tra gói hoặc vé ngày liên quan.");
                }
                return new InvoiceData(status, dueDate, invoiceDate, total);
            }
        }
    }

    private MemberData readMembershipOwner(Connection cn, int membershipId, boolean lock) throws SQLException {
        String hint = lock ? " WITH (UPDLOCK, HOLDLOCK) " : " ";
        String sql = "SELECT ms.Visitor_ID, ms.Status, LTRIM(RTRIM(v.Last_Name + ' ' + v.First_Name)) AS Member_Name, v.Phone "
                + "FROM MEMBERSHIP ms" + hint + "JOIN VISITOR v ON v.Visitor_ID = ms.Visitor_ID WHERE ms.Membership_ID = ?";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, membershipId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new BusinessException("Không tìm thấy gói tập.");
                }
                return new MemberData(rs.getInt("Visitor_ID"), rs.getString("Member_Name"), rs.getString("Phone"),
                        MembershipStatus.fromDb(rs.getString("Status")));
            }
        }
    }

    private List<ClassImpact> findFutureClassImpacts(Connection cn, int visitorId, LocalDateTime now) throws SQLException {
        String sql = "SELECT c.Session_ID, c.Class_Name, s.Start_Time, s.End_Time, r.Attendance_Status "
                + "FROM CLASS_REGISTRATION r JOIN CLASS c ON c.Session_ID = r.Session_ID "
                + "JOIN [SESSION] s ON s.Session_ID = c.Session_ID "
                + "WHERE r.Visitor_ID = ? AND s.Start_Time > ? AND r.Attendance_Status <> 'Cancelled' "
                + "ORDER BY s.Start_Time, c.Session_ID";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, visitorId);
            ps.setTimestamp(2, Timestamp.valueOf(now));
            try (ResultSet rs = ps.executeQuery()) {
                List<ClassImpact> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(new ClassImpact(rs.getInt("Session_ID"), rs.getString("Class_Name"),
                            Sql.dateTime(rs, "Start_Time"), Sql.dateTime(rs, "End_Time"), rs.getString("Attendance_Status")));
                }
                return rows;
            }
        }
    }

    private static void validateSaleInput(int visitorId, int planId, LocalDate today) {
        if (visitorId <= 0 || planId <= 0 || today == null) {
            throw new BusinessException("Thông tin hội viên, gói tập hoặc ngày giao dịch không hợp lệ.");
        }
    }
}
