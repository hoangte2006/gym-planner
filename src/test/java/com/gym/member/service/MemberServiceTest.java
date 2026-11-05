package com.gym.member.service;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.gym.app.Session;
import com.gym.core.Validator;
import com.gym.core.exception.BusinessException;
import com.gym.core.model.Member;

class MemberServiceTest {

    private MemberService memberService;

    @BeforeEach
    public void setUp() {
        Session.logout();
        memberService = new MemberService();
    }

    // --- 1. LUẬT DỮ LIỆU HỘI VIÊN ---

    @Test
    void validate_validMember_doesNotThrow() {
        Member m = new Member();
        m.setFirstName("An");
        m.setLastName("Nguyen");
        m.setPhone("0912345678");
        m.setEmail("an.nguyen@example.com");

        assertDoesNotThrow(() -> memberService.validate(m));
    }

    @Test
    void validate_futureBirthDate_throwsBusinessException() {
        Member m = new Member();
        m.setFirstName("An");
        m.setLastName("Nguyen");
        m.setPhone("0912345678");
        m.setBirthDate(LocalDate.now().plusDays(1));

        var ex = assertThrows(BusinessException.class, () -> memberService.validate(m));
        assertNotNull(ex);
    }
@Test
    void validate_invalidEmail_throwsBusinessException() {
        // Kiểm tra trực tiếp luật email qua Validator của Core
        var ex = assertThrows(BusinessException.class, () ->
            new Validator()
                    .name("Tên", "An")
                    .name("Họ", "Nguyen")
                    .phone("Số điện thoại", "0912345678")
                    .email("Email", "email-sai-dinh-dang")
                    .throwIfInvalid()
        );
        assertNotNull(ex);
    }

    @Test
    void validate_invalidPhone_throwsBusinessException() {
        // Kiểm tra trực tiếp luật số điện thoại qua Validator của Core
        var ex = assertThrows(BusinessException.class, () ->
            new Validator()
                    .name("Tên", "An")
                    .name("Họ", "Nguyen")
                    .phone("Số điện thoại", "12345")
                    .throwIfInvalid()
        );
        assertNotNull(ex);
    }

    // --- 2. BÁN VÉ NGÀY (DAILY PASS) ---

    @Test
    void sellDailyPass_blankFirstName_throwsBusinessException() {
        var ex = assertThrows(BusinessException.class, () ->
            memberService.sellDailyPass("", "Nguyen", "0912345678", null, null)
        );
        assertNotNull(ex);
    }

    @Test
    void sellDailyPass_blankLastName_throwsBusinessException() {
        var ex = assertThrows(BusinessException.class, () ->
            memberService.sellDailyPass("An", "", "0912345678", null, null)
        );
        assertNotNull(ex);
    }

    @Test
    void sellDailyPass_invalidPhone_throwsBusinessException() {
        var ex = assertThrows(BusinessException.class, () ->
            memberService.sellDailyPass("An", "Nguyen", "12345", null, null)
        );
        assertNotNull(ex);
    }

    @Test
    void sellDailyPass_validData_doesNotThrowValidation() {
        assertDoesNotThrow(() ->
            new Validator()
                    .name("Tên", "Binh")
                    .name("Họ", "Tran")
                    .phone("Số điện thoại", "0987654321")
                    .throwIfInvalid()
        );
    }

    // --- 3. NÂNG CẤP VÉ NGÀY & XUẤT CSV ---

    @Test
    void upgradeDailyPassToMember_nonExistentId_throwsBusinessException() {
        var ex = assertThrows(BusinessException.class, () ->
            memberService.upgradeDailyPassToMember(-999, "test@gmail.com", null)
        );
        assertNotNull(ex);
    }

    @Test
    void exportMembersToCsv_validList_generatesValidCsvBytes() {
        Member m = new Member();
        m.setVisitorId(101);
        m.setFirstName("Binh");
        m.setLastName("Tran");
        m.setPhone("0987654321");
        m.setEmail("binh@gym.com");

        byte[] csvData = memberService.exportMembersToCsv(List.of(m));
        assertNotNull(csvData);
        assertTrue(csvData.length > 0);

        String text = new String(csvData);
        assertTrue(text.contains("Mã hội viên"));
        assertTrue(text.contains("Tran"));
        assertTrue(text.contains("0987654321"));
    }

    @Test
    void exportMembersToCsv_emptyList_generatesHeaderOnly() {
        byte[] csvData = memberService.exportMembersToCsv(List.of());
        assertNotNull(csvData);
        String text = new String(csvData);
        assertTrue(text.contains("Mã hội viên,Họ,Tên,Số điện thoại,Email,Ngày sinh,Ngày đăng ký"));
    }
}