package ru.mirea.aquarium.repository;

import ru.mirea.aquarium.model.Client;
import ru.mirea.aquarium.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClientRepository {
    public Client create(Client client) {
        String sql = "INSERT INTO clients(full_name, phone, email, address) VALUES (?, ?, ?, ?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, client.getFullName());
            ps.setString(2, client.getPhone());
            ps.setString(3, client.getEmail());
            ps.setString(4, client.getAddress());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) client.setId(rs.getLong(1));
            }
            return client;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка создания клиента: " + e.getMessage(), e);
        }
    }

    public List<Client> findAll() {
        String sql = "SELECT * FROM clients ORDER BY id";
        List<Client> result = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(map(rs));
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка чтения клиентов: " + e.getMessage(), e);
        }
    }

    public Optional<Client> findById(long id) {
        String sql = "SELECT * FROM clients WHERE id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска клиента: " + e.getMessage(), e);
        }
    }

    public void update(Client client) {
        String sql = "UPDATE clients SET full_name=?, phone=?, email=?, address=? WHERE id=?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, client.getFullName());
            ps.setString(2, client.getPhone());
            ps.setString(3, client.getEmail());
            ps.setString(4, client.getAddress());
            ps.setLong(5, client.getId());
            if (ps.executeUpdate() == 0)
                throw new RuntimeException("Клиент не найден.");
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка изменения клиента: " + e.getMessage(), e);
        }
    }

    public void delete(long id) {
        String sql = "DELETE FROM clients WHERE id=?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            if (ps.executeUpdate() == 0)
                throw new RuntimeException("Клиент не найден.");
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка удаления клиента: " + e.getMessage(), e);
        }
    }

    private Client map(ResultSet rs) throws SQLException {
        return new Client(rs.getLong("id"), rs.getString("full_name"),
                rs.getString("phone"), rs.getString("email"), rs.getString("address"));
    }
}
