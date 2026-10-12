package com.gym.core.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** Băm mật khẩu bằng SHA-256 kèm muối (salt). Mật khẩu không bao giờ được lưu dạng chữ thường. */
public final class PasswordHasher {

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    /** Sinh một muối ngẫu nhiên (16 byte, mã hóa Base64). */
    public static String newSalt() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    /** SHA-256 của (muối + mật khẩu), dạng chuỗi hex. */
    public static String hash(String salt, String password) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] digest = sha.digest((salt + password).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JVM không hỗ trợ SHA-256", e);
        }
    }

    /** Mật khẩu nhập vào có khớp với bản băm đã lưu không. */
    public static boolean matches(String salt, String expectedHash, String password) {
        if (salt == null || expectedHash == null || password == null) {
            return false;
        }
        // So sánh theo thời gian không đổi để không lộ thông tin qua độ trễ
        return MessageDigest.isEqual(
                hash(salt, password).getBytes(StandardCharsets.UTF_8),
                expectedHash.getBytes(StandardCharsets.UTF_8));
    }
}
