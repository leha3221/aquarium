package ru.mirea.aquarium.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class DatabaseManager {
    private static final Map<String, String> DOT_ENV = readDotEnv();
    private static final String PORT = config("POSTGRES_PORT", "5433");
    private static final String URL = config("AQUARIUM_DB_URL",
            "jdbc:postgresql://localhost:" + PORT + "/aquarium_service?characterEncoding=UTF-8");
    private static final String USER = firstConfigured("AQUARIUM_DB_USER", "POSTGRES_USER", "postgres");
    private static final String PASSWORD = firstConfigured("AQUARIUM_DB_PASSWORD", "POSTGRES_PASSWORD", "");

    private DatabaseManager() {}

    public static Connection getConnection() throws SQLException {
        if (PASSWORD.isBlank()) {
            throw new SQLException("Пароль PostgreSQL не задан. Заполните POSTGRES_PASSWORD в файле .env.");
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    private static String firstConfigured(String primary, String secondary, String fallback) {
        String value = System.getenv(primary);
        if (value != null && !value.isBlank()) return value;
        value = System.getenv(secondary);
        if (value != null && !value.isBlank()) return value;
        value = DOT_ENV.get(secondary);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String config(String key, String fallback) {
        String value = System.getenv(key);
        if (value != null && !value.isBlank()) return value;
        value = DOT_ENV.get(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static Map<String, String> readDotEnv() {
        Map<String, String> values = new HashMap<>();
        Path file = Path.of(".env");
        if (!Files.isRegularFile(file)) return values;
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                int separator = trimmed.indexOf('=');
                if (separator < 1) continue;
                String key = trimmed.substring(0, separator).trim();
                String value = trimmed.substring(separator + 1).trim();
                if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'")))) {
                    value = value.substring(1, value.length() - 1);
                }
                values.put(key, value);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось прочитать .env", e);
        }
        return values;
    }

    public static void testConnection() throws SQLException {
        try (Connection connection = getConnection()) {
            System.out.println("Подключение к PostgreSQL успешно.");
        }
    }
}
