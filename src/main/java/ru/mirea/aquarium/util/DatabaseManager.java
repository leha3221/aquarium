package ru.mirea.aquarium.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseManager {
    private static final String URL =
            "jdbc:postgresql://localhost:5433/aquarium_service?characterEncoding=UTF-8";
    private static final String USER = "postgres";
    private static final String PASSWORD = "1234";

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
