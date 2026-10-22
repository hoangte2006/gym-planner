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
import java.util.Optional;

import com.gym.billing.model.ExpiringMembershipView;
import com.gym.billing.model.InvoiceView;
import com.gym.billing.model.MemberChoice;
import com.gym.billing.model.MembershipView;
import com.gym.billing.model.RenewalBase;
import com.gym.billing.model.RevenueByPlan;
import com.gym.core.DBConnection;
import com.gym.core.dao.BillingLogDao;
import com.gym.core.dao.PageResult;
import com.gym.core.dao.Sql;
import com.gym.core.dao.StatementBinder;
import com.gym.core.exception.DataAccessException;
import com.gym.core.model.BillingStatus;
import com.gym.core.model.MembershipStatus;

/**
 * Các truy vấn dạng view và báo cáo của module billing; không sửa DAO thuộc
 * Core.
 */
public class BillingQueryDao extends BillingLogDao {

    @FunctionalInterface
    private interface RowMapper<T> {

        T map(ResultSet rs) throws SQLException;
    }

    private static final String INVOICE_FROM = " FROM BILLING_LOG b "
            + "JOIN VISITOR v ON v.Visitor_ID = b.Visitor_ID "
            + "LEFT JOIN MEMBERSHIP ms ON ms.Membership_ID = b.Membership_ID "
            + "LEFT JOIN MEMBERSHIP_PLAN p ON p.Plan_ID = ms.Plan_ID "
            + "LEFT JOIN DAILY_PASS_TICKET d ON d.Visitor_ID = b.Visitor_ID ";

    private static final String INVOICE_SELECT = "SELECT b.Bill_ID, b.Visitor_ID, "
            + "LTRIM(RTRIM(v.Last_Name + ' ' + v.First_Name)) AS Customer_Name, v.Phone, "
            + "b.Membership_ID, COALESCE(p.Plan_Name, 'Vé ngày') AS Plan_Name, "
            + "b.Invoice_Date, b.Due_Date, b.Payment_Date, "
            + "CAST(COALESCE(p.Price, d.Daily_Rate, 0.00) AS DECIMAL(10,2)) AS Total_Amount, "
            + "b.Amount_Paid, b.Billing_Status ";

    public PageResult<InvoiceView> findInvoicePage(int page, int pageSize, String keyword,
            BillingStatus status, LocalDate from, LocalDate to) {
        return findInvoicePageInternal(page, pageSize, keyword, status, from, to, false);
    }

    public PageResult<InvoiceView> findDebtPage(int page, int pageSize, String keyword,
            LocalDate from, LocalDate to) {
        return findInvoicePageInternal(page, pageSize, keyword, null, from, to, true);
    }

    private PageResult<InvoiceView> findInvoicePageInternal(int page, int pageSize, String keyword,
            BillingStatus status, LocalDate from, LocalDate to,
            boolean debtOnly) {
        validatePage(page, pageSize);
        validateDateRange(from, to);
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        addKeyword(where, params, keyword, "CAST(b.Bill_ID AS VARCHAR(20))", "v.Last_Name + ' ' + v.First_Name", "v.Phone", "p.Plan_Name");
        if (status != null) {
            where.append(" AND b.Billing_Status = ? ");
            params.add(status.getDbValue());
        }
        if (debtOnly) {
            where.append(" AND b.Billing_Status IN ('Pending','Failed') ");
        }
        if (from != null) {
            where.append(" AND b.Invoice_Date >= ? ");
            params.add(Timestamp.valueOf(from.atStartOfDay()));
        }
        if (to != null) {
            where.append(" AND b.Invoice_Date < ? ");
            params.add(Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
        }
        String whereSql = where.toString();
        long total = countRows("SELECT COUNT(*)" + INVOICE_FROM + whereSql, params);
        List<InvoiceView> rows = queryRows(INVOICE_SELECT + INVOICE_FROM + whereSql
                + " ORDER BY b.Bill_ID DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                ps -> {
                    bindValues(ps, params);
                    ps.setInt(params.size() + 1, (page - 1) * pageSize);
                    ps.setInt(params.size() + 2, pageSize);
                }, BillingQueryDao::mapInvoice);
        return new PageResult<>(rows, page, pageSize, total);
    }

    public Optional<InvoiceView> findInvoiceById(int billId) {
        return queryOneRows(INVOICE_SELECT + INVOICE_FROM + " WHERE b.Bill_ID = ?",
                ps -> ps.setInt(1, billId), BillingQueryDao::mapInvoice);
    }

    /**
     * Tìm tối đa 100 hội viên theo tên/SĐT; chỉ tải cờ ảnh, không kéo ảnh nhị
     * phân theo danh sách.
     */
    public List<MemberChoice> searchMembers(String keyword) {
        String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        String sql = "SELECT v.Visitor_ID, LTRIM(RTRIM(v.Last_Name + ' ' + v.First_Name)) AS Member_Name, "
                + "v.Phone, CASE WHEN m.Photo IS NULL THEN 0 ELSE 1 END AS Has_Photo, "
                + "a.Plan_Name AS Active_Plan, a.Start_Date AS Active_Start, a.End_Date AS Active_End "
                + "FROM VISITOR v JOIN MEMBER m ON m.Visitor_ID = v.Visitor_ID "
                + "OUTER APPLY (SELECT TOP (1) p.Plan_Name, ms.Start_Date, ms.End_Date "
                + "FROM MEMBERSHIP ms JOIN MEMBERSHIP_PLAN p ON p.Plan_ID = ms.Plan_ID "
                + "WHERE ms.Visitor_ID = v.Visitor_ID AND ms.Status = 'Active' "
                + "ORDER BY ms.End_Date DESC, ms.Membership_ID DESC) a "
                + "WHERE (v.Last_Name + ' ' + v.First_Name) LIKE ? OR v.Phone LIKE ? OR CAST(v.Visitor_ID AS VARCHAR(20)) LIKE ? "
                + "ORDER BY v.Last_Name, v.First_Name, v.Visitor_ID OFFSET 0 ROWS FETCH NEXT 100 ROWS ONLY";
        return queryRows(sql, ps -> {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
        }, rs -> new MemberChoice(rs.getInt("Visitor_ID"), rs.getString("Member_Name"), rs.getString("Phone"),
                rs.getInt("Has_Photo") == 1, rs.getString("Active_Plan"), Sql.date(rs, "Active_Start"),
                Sql.date(rs, "Active_End")));
    }

    /**
     * Đếm việc dùng bảng giá trong MEMBERSHIP để giữ nguyên lịch sử giá đã bán.
     */
    public boolean isPlanInUse(int planId) {
        return countRows("SELECT COUNT(*) FROM MEMBERSHIP WHERE Plan_ID = ?", List.of(planId)) > 0;
    }

    /**
     * Gói Active hiện tại; nếu không có thì gói Expired gần nhất làm mốc gia
     * hạn.
     */
    public Optional<RenewalBase> findRenewalBase(int visitorId) {
        String sql = "SELECT TOP (1) Membership_ID, Status, End_Date FROM MEMBERSHIP "
                + "WHERE Visitor_ID = ? AND Status IN ('Active','Expired') "
                + "ORDER BY CASE WHEN Status = 'Active' THEN 0 ELSE 1 END, Start_Date DESC, Membership_ID DESC";
        return queryOneRows(sql, ps -> ps.setInt(1, visitorId), rs -> new RenewalBase(
                rs.getInt("Membership_ID"), MembershipStatus.fromDb(rs.getString("Status")), Sql.date(rs, "End_Date")));
    }

    /**
     * Danh sách gói theo trạng thái, có tìm kiếm và phân trang DB-side.
     */
    public PageResult<MembershipView> findMembershipPage(int page, int pageSize, String keyword,
            MembershipStatus status) {
        validatePage(page, pageSize);
        String fromClause = " FROM MEMBERSHIP ms JOIN VISITOR v ON v.Visitor_ID = ms.Visitor_ID "
                + "JOIN MEMBERSHIP_PLAN p ON p.Plan_ID = ms.Plan_ID ";
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        addKeyword(where, params, keyword, "v.Last_Name + ' ' + v.First_Name", "v.Phone", "p.Plan_Name");
        if (status != null) {
            where.append(" AND ms.Status = ? ");
            params.add(status.getDbValue());
        }
        String select = "SELECT ms.Membership_ID, ms.Visitor_ID, "
                + "LTRIM(RTRIM(v.Last_Name + ' ' + v.First_Name)) AS Member_Name, v.Phone, p.Plan_Name, "
                + "ms.Start_Date, ms.End_Date, ms.Status ";
        long total = countRows("SELECT COUNT(*)" + fromClause + where, params);
        List<MembershipView> rows = queryRows(select + fromClause + where
                + " ORDER BY ms.Start_Date DESC, ms.Membership_ID DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                ps -> {
                    bindValues(ps, params);
                    ps.setInt(params.size() + 1, (page - 1) * pageSize);
                    ps.setInt(params.size() + 2, pageSize);
                }, rs -> new MembershipView(rs.getInt("Membership_ID"), rs.getInt("Visitor_ID"),
                        rs.getString("Member_Name"), rs.getString("Phone"), rs.getString("Plan_Name"),
                        Sql.date(rs, "Start_Date"), Sql.date(rs, "End_Date"),
                        MembershipStatus.fromDb(rs.getString("Status"))));
        return new PageResult<>(rows, page, pageSize, total);
    }

    /**
     * Các gói Active còn từ 0 đến N ngày; End_Date bằng hôm nay vẫn được tô đỏ
     * ở giao diện.
     */
    public List<ExpiringMembershipView> findExpiringMemberships(LocalDate today, int days) {
        if (today == null || days < 0) {
            throw new IllegalArgumentException("Ngày hoặc số ngày không hợp lệ.");
        }
        String sql = "SELECT ms.Membership_ID, ms.Visitor_ID, "
                + "LTRIM(RTRIM(v.Last_Name + ' ' + v.First_Name)) AS Member_Name, v.Phone, p.Plan_Name, ms.End_Date "
                + "FROM MEMBERSHIP ms JOIN VISITOR v ON v.Visitor_ID = ms.Visitor_ID "
                + "JOIN MEMBERSHIP_PLAN p ON p.Plan_ID = ms.Plan_ID "
                + "WHERE ms.Status = 'Active' AND ms.End_Date >= ? AND ms.End_Date <= ? "
                + "ORDER BY ms.End_Date, v.Last_Name, v.First_Name";
        return queryRows(sql, ps -> {
            Sql.setDate(ps, 1, today);
            Sql.setDate(ps, 2, today.plusDays(days));
        }, rs -> {
            LocalDate endDate = Sql.date(rs, "End_Date");
            long left = java.time.temporal.ChronoUnit.DAYS.between(today, endDate);
            return new ExpiringMembershipView(rs.getInt("Membership_ID"), rs.getInt("Visitor_ID"),
                    rs.getString("Member_Name"), rs.getString("Phone"), rs.getString("Plan_Name"), endDate, left);
        });
    }

    /**
     * Doanh thu chỉ tính hóa đơn Paid, nhóm theo tên gói, dựa trên
     * Payment_Date.
     */
    public List<RevenueByPlan> revenueByPlan(LocalDate from, LocalDate to) {
        validateDateRange(from, to);
        if (from == null || to == null) {
            throw new IllegalArgumentException("Báo cáo cần đủ ngày bắt đầu và kết thúc.");
        }
        String sql = "SELECT COALESCE(p.Plan_Name, 'Vé ngày') AS Plan_Name, COUNT(*) AS Paid_Count, "
                + "CAST(SUM(b.Amount_Paid) AS DECIMAL(18,2)) AS Revenue "
                + "FROM BILLING_LOG b LEFT JOIN MEMBERSHIP ms ON ms.Membership_ID = b.Membership_ID "
                + "LEFT JOIN MEMBERSHIP_PLAN p ON p.Plan_ID = ms.Plan_ID "
                + "WHERE b.Billing_Status = 'Paid' AND b.Payment_Date >= ? AND b.Payment_Date < ? "
                + "GROUP BY COALESCE(p.Plan_Name, 'Vé ngày') ORDER BY Revenue DESC, Plan_Name";
        return queryRows(sql, ps -> {
            ps.setTimestamp(1, Timestamp.valueOf(from.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
        }, rs -> new RevenueByPlan(rs.getString("Plan_Name"), rs.getLong("Paid_Count"), rs.getBigDecimal("Revenue")));
    }

    private static InvoiceView mapInvoice(ResultSet rs) throws SQLException {
        BigDecimal total = rs.getBigDecimal("Total_Amount");
        if (total == null) {
            total = BigDecimal.ZERO.setScale(2);
        }
        return new InvoiceView(rs.getInt("Bill_ID"), rs.getInt("Visitor_ID"), rs.getString("Customer_Name"),
                rs.getString("Phone"), Sql.integer(rs, "Membership_ID"), rs.getString("Plan_Name"),
                Sql.dateTime(rs, "Invoice_Date"), Sql.date(rs, "Due_Date"), Sql.dateTime(rs, "Payment_Date"),
                total.setScale(2, java.math.RoundingMode.HALF_UP), rs.getBigDecimal("Amount_Paid"),
                BillingStatus.fromDb(rs.getString("Billing_Status")));
    }

    private static void addKeyword(StringBuilder where, List<Object> params, String keyword, String... columns) {
        if (keyword == null || keyword.isBlank()) {
            return;
        }
        where.append(" AND (");
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) {
                where.append(" OR ");
            }
            where.append(columns[i]).append(" LIKE ?");
            params.add("%" + keyword.trim() + "%");
        }
        where.append(") ");
    }

    private static void bindValues(PreparedStatement ps, List<?> values) throws SQLException {
        for (int i = 0; i < values.size(); i++) {
            Object value = values.get(i);
            int parameter = i + 1;
            if (value instanceof LocalDate date) {
                ps.setDate(parameter, java.sql.Date.valueOf(date)); 
            }else if (value instanceof LocalDateTime dateTime) {
                ps.setTimestamp(parameter, Timestamp.valueOf(dateTime)); 
            }else {
                ps.setObject(parameter, value);
            }
        }
    }

    private static void validatePage(int page, int pageSize) {
        if (page < 1 || pageSize < 1) {
            throw new IllegalArgumentException("Trang và kích thước trang phải từ 1 trở lên.");
        }
    }

    private static void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new IllegalArgumentException("Ngày kết thúc không được trước ngày bắt đầu.");
        }
    }

    private long countRows(String sql, List<?> params) {
        return withConnection(sql, ps -> bindValues(ps, new ArrayList<>(params)), rs -> rs.next() ? rs.getLong(1) : 0L);
    }

    private <T> List<T> queryRows(String sql, StatementBinder binder, RowMapper<T> mapper) {
        try (Connection cn = DBConnection.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(mapper.map(rs));
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Không đọc được dữ liệu của module Gói tập & Thanh toán.", e);
        }
    }

    private <T> Optional<T> queryOneRows(String sql, StatementBinder binder, RowMapper<T> mapper) {
        List<T> rows = queryRows(sql, binder, mapper);
        return rows.stream().findFirst();
    }

    @FunctionalInterface
    private interface SqlResult<T> {

        T read(ResultSet rs) throws SQLException;
    }

    private <T> T withConnection(String sql, StatementBinder binder, SqlResult<T> reader) {
        try (Connection cn = DBConnection.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return reader.read(rs);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Không đọc được dữ liệu của module Gói tập & Thanh toán.", e);
        }
    }
}
