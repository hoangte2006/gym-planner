package com.gym.member.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.gym.app.Session;
import com.gym.core.ErrorTranslator;
import com.gym.core.Tx;
import com.gym.core.Validator;
import com.gym.core.dao.BillingLogDao;
import com.gym.core.dao.DailyPassTicketDao;
import com.gym.core.dao.MemberDao;
import com.gym.core.dao.MembershipDao;
import com.gym.core.dao.PageResult;
import com.gym.core.exception.BusinessException;
import com.gym.core.model.BillingLog;
import com.gym.core.model.BillingStatus;
import com.gym.core.model.DailyPassTicket;
import com.gym.core.model.Member;
import com.gym.core.model.Membership;
import com.gym.core.model.StaffRole;

public class MemberService {

    public static final BigDecimal DEFAULT_DAILY_RATE = new BigDecimal("15.00");

    private final MemberDao memberDao = new MemberDao();
    private final DailyPassTicketDao dailyPassTicketDao = new DailyPassTicketDao();
    private final BillingLogDao billingLogDao = new BillingLogDao();
    private final MembershipDao membershipDao = new MembershipDao();

    /** Chỉ Lễ tân và Quản lý mới được thao tác trên module Hội viên */
    private void checkPermission() {
        if (Session.isLoggedIn()) {
            StaffRole role = Session.getRole();
            if (role != StaffRole.MANAGER && role != StaffRole.RECEPTIONIST) {
                throw new BusinessException("Bạn không có quyền thực hiện thao tác này.");
            }
        }
    }

    public PageResult<Member> searchMembers(String keyword, int page, int pageSize) {
        checkPermission();
        return memberDao.search(keyword, page, pageSize);
    }

    public Optional<Member> getMemberById(int id) {
        checkPermission();
        return memberDao.findById(id);
    }

    public byte[] getMemberPhoto(int id) {
        checkPermission();
        return memberDao.findPhoto(id).orElse(null);
    }

    public List<Membership> getMembershipHistory(int visitorId) {
        checkPermission();
        return membershipDao.findByVisitor(visitorId);
    }

    public List<BillingLog> getBillingHistory(int visitorId) {
        checkPermission();
        return billingLogDao.findByVisitor(visitorId);
    }

    public void validate(Member m) {
        Validator v = new Validator()
                .name("Tên", m.getFirstName())
                .name("Họ", m.getLastName())
                .phone("Số điện thoại", m.getPhone());

        if (m.getEmail() != null && !m.getEmail().isBlank()) {
            v.email("Email", m.getEmail());
        }

        if (m.getBirthDate() != null) {
            v.birthDate("Ngày sinh", m.getBirthDate(), LocalDate.now());
        }

        v.throwIfInvalid();
    }

    public Member saveMember(Member member) {
        checkPermission();
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
        checkPermission();

        // BR-MEM-08: Ràng buộc chứng từ tài chính và gói tập
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
            throw new BusinessException("Không thể xóa khách này vì còn lưu giữ chứng từ (BR-MEM-08): "
                    + String.join(", ", blockers) + ".");
        }

        try {
            memberDao.deleteById(visitorId);
        } catch (Exception ex) {
            throw new BusinessException(ErrorTranslator.toMessage(ex));
        }
    }

    public void sellDailyPass(String firstName, String lastName, String phone, LocalDate visitDate, BigDecimal rate) {
        checkPermission();

        new Validator()
                .name("Tên", firstName)
                .name("Họ", lastName)
                .phone("Số điện thoại", phone)
                .throwIfInvalid();

        BigDecimal finalRate = (rate != null) ? rate : DEFAULT_DAILY_RATE;
        LocalDate finalDate = (visitDate != null) ? visitDate : LocalDate.now();

        // Bọc cả hai DAO vào Tx.run để đảm bảo tính toàn vẹn giao dịch
        Tx.run(() -> {
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
        });
    }
}