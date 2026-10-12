package com.gym.core.service;

import com.gym.core.Config;
import com.gym.core.dao.StaffDao;
import com.gym.core.exception.BusinessException;
import com.gym.core.util.PasswordHasher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {

    @Test
    void hashIsDeterministicAndSaltDependent() {
        String salt = PasswordHasher.newSalt();
        assertEquals(PasswordHasher.hash(salt, "abc123"), PasswordHasher.hash(salt, "abc123"));
        assertNotEquals(PasswordHasher.hash(salt, "abc123"), PasswordHasher.hash(PasswordHasher.newSalt(), "abc123"));
        assertEquals(64, PasswordHasher.hash(salt, "abc123").length());   // SHA-256 = 64 ký tự hex
    }

    @Test
    void matchesAcceptsOnlyCorrectPassword() {
        String salt = PasswordHasher.newSalt();
        String hash = PasswordHasher.hash(salt, "secret1");
        assertTrue(PasswordHasher.matches(salt, hash, "secret1"));
        assertFalse(PasswordHasher.matches(salt, hash, "secret2"));
        assertFalse(PasswordHasher.matches(salt, hash, null));
    }

    @Test
    void changePasswordWritesHashAndReplacesDefault(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("config.local.properties");
        Files.write(file, List.of("db.host=localhost", "auth.salt=", "auth.passwordHash=", "db.port=1433"),
                StandardCharsets.UTF_8);
        AuthService auth = new AuthService(new StaffDao(), Config.load(file));

        // Sai mật khẩu hiện tại / mật khẩu mới quá ngắn
        assertThrows(BusinessException.class, () -> auth.changePassword("sai-roi", "matkhaumoi1"));
        assertThrows(BusinessException.class, () -> auth.changePassword(AuthService.DEFAULT_PASSWORD, "123"));

        auth.changePassword(AuthService.DEFAULT_PASSWORD, "matkhaumoi1");

        String text = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(text.contains("db.host=localhost"), "các dòng khác phải được giữ nguyên");
        assertTrue(text.contains("db.port=1433"));
        assertFalse(text.contains("matkhaumoi1"), "không được lưu mật khẩu dạng chữ thường");
        assertFalse(text.contains("auth.passwordHash=\n"));

        // Mật khẩu mặc định không còn dùng được, mật khẩu mới thì được
        assertThrows(BusinessException.class, () -> auth.changePassword(AuthService.DEFAULT_PASSWORD, "khac12345"));
        auth.changePassword("matkhaumoi1", "matkhautiep2");
    }

    @Test
    void loginRejectsBlankInputWithoutTouchingDatabase(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("c.properties");
        Files.write(file, List.of("auth.salt=", "auth.passwordHash="), StandardCharsets.UTF_8);
        AuthService auth = new AuthService(new StaffDao(), Config.load(file));
        assertThrows(BusinessException.class, () -> auth.login("", "gym123"));
        assertThrows(BusinessException.class, () -> auth.login("0901000005", ""));
    }
}
