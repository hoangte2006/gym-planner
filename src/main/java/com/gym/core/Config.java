package com.gym.core;

import com.gym.core.exception.DataAccessException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Đọc cấu hình từ config.local.properties (thư mục chạy ứng dụng, UTF-8).
 * Có thể đổi đường dẫn bằng -Dgym.config=đường_dẫn. File này không được commit.
 */
public final class Config {

    public static final String DEFAULT_FILE = "config.local.properties";
    public static final String FILE_PROPERTY = "gym.config";

    private static Config instance;

    private final Properties props;
    private final Path source;

    private Config(Properties props, Path source) {
        this.props = props;
        this.source = source;
    }

    /** Nạp cấu hình một lần, các lần sau dùng lại. */
    public static synchronized Config get() {
        if (instance == null) {
            instance = load(Path.of(System.getProperty(FILE_PROPERTY, DEFAULT_FILE)));
        }
        return instance;
    }

    static Config load(Path file) {
        if (!Files.isRegularFile(file)) {
            throw new DataAccessException("Không tìm thấy file cấu hình " + file.toAbsolutePath()
                    + ". Hãy sao chép config.example.properties thành " + DEFAULT_FILE
                    + " rồi điền thông tin kết nối SQL Server.");
        }
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(file);
             InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            p.load(reader);
        } catch (IOException e) {
            throw new DataAccessException("Không đọc được file cấu hình " + file.toAbsolutePath(), e);
        }
        return new Config(p, file);
    }

    public String get(String key, String defaultValue) {
        String v = props.getProperty(key);
        return v == null || v.isBlank() ? defaultValue : v.trim();
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            throw new DataAccessException("Giá trị cấu hình '" + key + "' phải là số nguyên.", e);
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(get(key, String.valueOf(defaultValue)));
    }

    public Path getSource() {
        return source;
    }
}
