package com.gym.member.service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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

    /** Nâng cấp vé ngày thành hội viên chính thức */
   public Member upgradeDailyPassToMember(int visitorId, String email, LocalDate birthDate) {
        checkPermission();

        DailyPassTicket ticket;
        try {
            ticket = dailyPassTicketDao.findById(visitorId)
                    .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin vé ngày của khách #" + visitorId));
        } catch (com.gym.core.exception.DataAccessException dae) {
            throw new BusinessException("Không tìm thấy thông tin vé ngày của khách #" + visitorId);
        }

        Member newMember = new Member();
        newMember.setFirstName(ticket.getFirstName());
        newMember.setLastName(ticket.getLastName());
        newMember.setPhone(ticket.getPhone());
        newMember.setEmail(email);
        newMember.setBirthDate(birthDate);
        newMember.setRegistrationDate(LocalDateTime.now());

        validate(newMember);

        return Tx.call(() -> {
            dailyPassTicketDao.deleteById(visitorId);
            return memberDao.insert(newMember);
        });
    }

    /** Xuất danh sách hội viên ra định dạng CSV UTF-8 có BOM */
    public byte[] exportMembersToCsv(List<Member> members) {
        checkPermission();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
            baos.write(0xEF);
            baos.write(0xBB);
            baos.write(0xBF);

            writer.println("Mã hội viên,Họ,Tên,Số điện thoại,Email,Ngày sinh,Ngày đăng ký");
            for (Member m : members) {
                writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                        m.getVisitorId(),
                        m.getLastName() != null ? m.getLastName() : "",
                        m.getFirstName() != null ? m.getFirstName() : "",
                        m.getPhone() != null ? m.getPhone() : "",
                        m.getEmail() != null ? m.getEmail() : "",
                        m.getBirthDate() != null ? m.getBirthDate().toString() : "",
                        m.getRegistrationDate() != null ? m.getRegistrationDate().toString() : ""
                );
            }
            writer.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("Không thể xuất tệp CSV: " + e.getMessage());
        }
    }
}