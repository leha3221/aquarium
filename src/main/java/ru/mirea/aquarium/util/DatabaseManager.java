package ru.mirea.aquarium.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseManager {
    private static final String URL = System.getenv().getOrDefault("AQUARIUM_DB_URL",
            "jdbc:postgresql://localhost:5433/aquarium_service?characterEncoding=UTF-8");
    private static final String USER = System.getenv().getOrDefault("AQUARIUM_DB_USER", "postgres");
    private static final String PASSWORD = System.getenv().getOrDefault("AQUARIUM_DB_PASSWORD", "");

    private DatabaseManager() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void testConnection() throws SQLException {
        try (Connection connection = getConnection()) {
            System.out.println("Подключение к PostgreSQL успешно.");
        }
    }
}
