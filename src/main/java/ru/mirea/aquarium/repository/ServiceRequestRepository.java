package ru.mirea.aquarium.repository;

import ru.mirea.aquarium.model.*;
import ru.mirea.aquarium.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ServiceRequestRepository {
    private static final String BASE =
            "SELECT r.*, c.full_name AS client_name FROM service_requests r " +
            "JOIN clients c ON c.id = r.client_id ";

    public ServiceRequest create(ServiceRequest r) {
        String sql = "INSERT INTO service_requests " +
                "(client_id,aquarium_type,service_type,description,status,priority,created_at,scheduled_at,price) " +
                "VALUES (?,?,?,?,?,?,?,?,?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            fill(ps, r);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) r.setId(rs.getLong(1));
            }
            return r;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка создания заявки: " + e.getMessage(), e);
        }
    }

    public List<ServiceRequest> findAll() {
        return query(BASE + "ORDER BY r.id");
    }

    public Optional<ServiceRequest> findById(long id) {
        String sql = BASE + "WHERE r.id=?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска заявки: " + e.getMessage(), e);
        }
    }

    public void update(ServiceRequest r) {
        String sql = "UPDATE service_requests SET client_id=?, aquarium_type=?, service_type=?, " +
                "description=?, status=?, priority=?, scheduled_at=?, price=? WHERE id=?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, r.getClientId());
            ps.setString(2, r.getAquariumType().name());
            ps.setString(3, r.getServiceType().name());
            ps.setString(4, r.getDescription());
            ps.setString(5, r.getStatus().name());
            ps.setInt(6, r.getPriority());
            ps.setTimestamp(7, Timestamp.valueOf(r.getScheduledAt()));
            ps.setBigDecimal(8, r.getPrice());
            ps.setLong(9, r.getId());
            if (ps.executeUpdate() == 0)
                throw new RuntimeException("Заявка не найдена.");
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка изменения заявки: " + e.getMessage(), e);
        }
    }

    public void delete(long id) {
        String sql = "DELETE FROM service_requests WHERE id=?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            if (ps.executeUpdate() == 0)
                throw new RuntimeException("Заявка не найдена.");
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка удаления заявки: " + e.getMessage(), e);
        }
    }

    private List<ServiceRequest> query(String sql, Object... params) {
        List<ServiceRequest> result = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(map(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка SQL-запроса: " + e.getMessage(), e);
        }
    }

    private void fill(PreparedStatement ps, ServiceRequest r) throws SQLException {
        ps.setLong(1, r.getClientId());
        ps.setString(2, r.getAquariumType().name());
        ps.setString(3, r.getServiceType().name());
        ps.setString(4, r.getDescription());
        ps.setString(5, r.getStatus().name());
        ps.setInt(6, r.getPriority());
        ps.setTimestamp(7, Timestamp.valueOf(r.getCreatedAt()));
        ps.setTimestamp(8, Timestamp.valueOf(r.getScheduledAt()));
        ps.setBigDecimal(9, r.getPrice());
    }

    private ServiceRequest map(ResultSet rs) throws SQLException {
        return new ServiceRequest(
                rs.getLong("id"),
                rs.getLong("client_id"),
                AquariumType.valueOf(rs.getString("aquarium_type")),
                ServiceType.valueOf(rs.getString("service_type")),
                rs.getString("description"),
                RequestStatus.valueOf(rs.getString("status")),
                rs.getInt("priority"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("scheduled_at").toLocalDateTime(),
                rs.getBigDecimal("price"),
                rs.getString("client_name")
        );
    }
}
