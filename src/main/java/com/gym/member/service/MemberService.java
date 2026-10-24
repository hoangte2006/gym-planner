package com.gym.member.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.gym.core.dao.BillingLogDao;
import com.gym.core.dao.DailyPassTicketDao;
import com.gym.core.dao.MemberDao;
import com.gym.core.dao.MembershipDao;
import com.gym.core.dao.PageResult;
import com.gym.core.model.BillingLog;
import com.gym.core.model.BillingStatus;
import com.gym.core.model.DailyPassTicket;
import com.gym.core.model.Member;
import com.gym.core.model.Membership;

public class MemberService {

    private final MemberDao memberDao = new MemberDao();
    private final DailyPassTicketDao dailyPassTicketDao = new DailyPassTicketDao();
    private final BillingLogDao billingLogDao = new BillingLogDao();
    private final MembershipDao membershipDao = new MembershipDao();

    public PageResult<Member> searchMembers(String keyword, int page, int pageSize) {
        return memberDao.search(keyword, page, pageSize);
    }

    public Optional<Member> getMemberById(int id) {
        return memberDao.findById(id);
    }

    public byte[] getMemberPhoto(int id) {
        return memberDao.findPhoto(id).orElse(null);
    }

    public List<Membership> getMembershipHistory(int visitorId) {
        return membershipDao.findByVisitor(visitorId);
    }

    public List<BillingLog> getBillingHistory(int visitorId) {
        return billingLogDao.findByVisitor(visitorId);
    }

    public void validate(Member m) {
        if (m.getFirstName() == null || m.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên không được để trống.");
        }
        if (m.getLastName() == null || m.getLastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Họ không được để trống.");
        }
        if (m.getPhone() == null || !m.getPhone().trim().matches("^0\\d{9}$")) {
            throw new IllegalArgumentException("Số điện thoại phải gồm 10 chữ số, bắt đầu bằng 0.");
        }
        if (m.getEmail() != null && !m.getEmail().trim().isEmpty()) {
            if (!m.getEmail().trim().matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
                throw new IllegalArgumentException("Email không đúng định dạng.");
            }
        }
    }

    public Member saveMember(Member member) {
        validate(member);
        if (member.getVisitorId() == 0) {
            if (member.getRegistrationDate() == null) {
                member.setRegistrationDate(LocalDateTime.now());
            }
            return memberDao.insert(member);
        } else {
            memberDao.update(member);
            return member;
        }
    }

    public void deleteMember(int visitorId) {
        List<String> blockers = new ArrayList<>();
        List<BillingLog> bills = billingLogDao.findByVisitor(visitorId);
        if (!bills.isEmpty()) {
            blockers.add(bills.size() + " hóa đơn tài chính");
        }
        List<Membership> memberships = membershipDao.findByVisitor(visitorId);
        if (!memberships.isEmpty()) {
            blockers.add(memberships.size() + " hợp đồng gói tập");
        }

        if (!blockers.isEmpty()) {
            throw new IllegalArgumentException("Không thể xóa khách này vì còn lưu giữ chứng từ (BR-MEM-08): "
                    + String.join(", ", blockers) + ".");
        }

        try {
            memberDao.deleteById(visitorId);
        } catch (Exception ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : "";
            if (msg.contains("547") || msg.toLowerCase().contains("foreign key")) {
                throw new IllegalArgumentException("Không thể xóa khách này vì còn bản ghi tham chiếu trong hệ thống.");
            }
            throw ex;
        }
    }

    public void sellDailyPass(String firstName, String lastName, String phone, LocalDate visitDate, BigDecimal rate) {
        if (firstName == null || firstName.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên không được để trống.");
        }
        if (lastName == null || lastName.trim().isEmpty()) {
            throw new IllegalArgumentException("Họ không được để trống.");
        }
        if (phone == null || !phone.trim().matches("^0\\d{9}$")) {
            throw new IllegalArgumentException("Số điện thoại phải gồm 10 chữ số, bắt đầu bằng 0.");
        }

        BigDecimal finalRate = (rate != null) ? rate : new BigDecimal("15.00");
        LocalDate finalDate = (visitDate != null) ? visitDate : LocalDate.now();

        DailyPassTicket ticket = new DailyPassTicket();
        ticket.setFirstName(firstName.trim());
        ticket.setLastName(lastName.trim());
        ticket.setPhone(phone.trim());
        ticket.setVisitDate(finalDate);
        ticket.setDailyRate(finalRate);

        DailyPassTicket savedTicket = dailyPassTicketDao.insert(ticket);

        BillingLog bill = new BillingLog();
        bill.setVisitorId(savedTicket.getVisitorId());
        bill.setAmountPaid(finalRate);
        bill.setBillingStatus(BillingStatus.PAID);
        bill.setPaymentDate(LocalDateTime.now());
        bill.setDueDate(finalDate);
        billingLogDao.insert(bill);
    }
}