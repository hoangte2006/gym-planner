package com.gym.core.dao;

import com.gym.core.model.BillingLog;
import com.gym.core.model.BillingStatus;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** DAO bảng BILLING_LOG (hóa đơn). */
public class BillingLogDao extends AbstractJdbcDao<BillingLog, Integer> {

    @Override
    protected String tableName() {
        return "BILLING_LOG";
    }

    @Override
    protected String idColumn() {
        return "Bill_ID";
    }

    @Override
    protected BillingLog mapRow(ResultSet rs) throws SQLException {
        BillingLog b = new BillingLog();
        b.setBillId(rs.getInt("Bill_ID"));
        b.setVisitorId(rs.getInt("Visitor_ID"));
        b.setMembershipId(Sql.integer(rs, "Membership_ID"));
        b.setInvoiceDate(Sql.dateTime(rs, "Invoice_Date"));
        b.setDueDate(Sql.date(rs, "Due_Date"));
        b.setPaymentDate(Sql.dateTime(rs, "Payment_Date"));
        b.setAmountPaid(rs.getBigDecimal("Amount_Paid"));
        b.setBillingStatus(BillingStatus.fromDb(rs.getString("Billing_Status")));
        return b;
    }

    @Override
    public BillingLog insert(BillingLog b) {
        int id = insertAndGetKey(
                "INSERT INTO BILLING_LOG (Visitor_ID, Membership_ID, Invoice_Date, Due_Date, Payment_Date, "
                        + "Amount_Paid, Billing_Status) "
                        + "VALUES (?, ?, COALESCE(?, CURRENT_TIMESTAMP), ?, ?, COALESCE(?, 0.00), COALESCE(?, 'Pending'))",
                ps -> {
                    ps.setInt(1, b.getVisitorId());
                    Sql.setInteger(ps, 2, b.getMembershipId());
                    Sql.setDateTime(ps, 3, b.getInvoiceDate());
                    Sql.setDate(ps, 4, b.getDueDate());
                    Sql.setDateTime(ps, 5, b.getPaymentDate());
                    Sql.setDecimal(ps, 6, b.getAmountPaid());
                    Sql.setEnum(ps, 7, b.getBillingStatus());
                });
        b.setBillId(id);
        // Nhận lại giá trị mặc định do DB điền (ngày lập, số tiền, trạng thái)
        findById(id).ifPresent(r -> {
            b.setInvoiceDate(r.getInvoiceDate());
            b.setAmountPaid(r.getAmountPaid());
            b.setBillingStatus(r.getBillingStatus());
        });
        return b;
    }

    @Override
    public void update(BillingLog b) {
        execute("UPDATE BILLING_LOG SET Visitor_ID = ?, Membership_ID = ?, Invoice_Date = ?, Due_Date = ?, "
                        + "Payment_Date = ?, Amount_Paid = ?, Billing_Status = ? WHERE Bill_ID = ?",
                ps -> {
                    ps.setInt(1, b.getVisitorId());
                    Sql.setInteger(ps, 2, b.getMembershipId());
                    Sql.setDateTime(ps, 3, b.getInvoiceDate());
                    Sql.setDate(ps, 4, b.getDueDate());
                    Sql.setDateTime(ps, 5, b.getPaymentDate());
                    Sql.setDecimal(ps, 6, b.getAmountPaid());
                    Sql.setEnum(ps, 7, b.getBillingStatus());
                    ps.setInt(8, b.getBillId());
                });
    }

    /** Hóa đơn của một khách, mới nhất trước. */
    public List<BillingLog> findByVisitor(int visitorId) {
        return queryList(selectSql() + " WHERE Visitor_ID = ? ORDER BY Invoice_Date DESC, Bill_ID DESC",
                ps -> ps.setInt(1, visitorId));
    }
}
