package com.gym.member.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.gym.app.Session;
import com.gym.core.exception.BusinessException;
import com.gym.core.model.Member;

class MemberServiceTest {

    private MemberService memberService;

    @BeforeEach
    void setUp() {
        // Đảm bảo phiên đăng nhập luôn sạch trước mỗi bài test để không bị vướng checkPermission
        Session.logout();
        memberService = new MemberService();
    }

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
    void sellDailyPass_blankFirstName_throwsBusinessException() {
        assertThrows(BusinessException.class, () ->
            memberService.sellDailyPass("", "Nguyen", "0912345678", null, null)
        );
    }

    @Test
    void sellDailyPass_blankLastName_throwsBusinessException() {
        assertThrows(BusinessException.class, () ->
            memberService.sellDailyPass("An", "", "0912345678", null, null)
        );
    }

    @Test
    void sellDailyPass_invalidPhone_throwsBusinessException() {
        assertThrows(BusinessException.class, () ->
            memberService.sellDailyPass("An", "Nguyen", "12345", null, null)
        );
    }
}