package com.realtorbackend.demo;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcUserRepository implements UserRepository {
    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<User> ROW_MAPPER = (rs, rowNum) -> {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setPhoneNumber(rs.getString("phone_number"));
        user.setEmail(rs.getString("email"));
        user.setDescription(rs.getString("description"));
        user.setEmailVerified(rs.getBoolean("email_verified"));
        user.setRole(rs.getString("role"));
        user.setPasswordHash(rs.getString("password_hash"));
        return user;
    };

    public JdbcUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT id, first_name, last_name, phone_number, email, description, email_verified, role, password_hash FROM users ORDER BY id";
        return jdbcTemplate.query(sql, ROW_MAPPER);
    }

    @Override
    public Optional<User> findById(Long id) {
        String sql = "SELECT id, first_name, last_name, phone_number, email, description, email_verified, role, password_hash FROM users WHERE id = ?";
        List<User> rows = jdbcTemplate.query(sql, ROW_MAPPER, id);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT id, first_name, last_name, phone_number, email, description, email_verified, role, password_hash FROM users WHERE email = ?";
        List<User> rows = jdbcTemplate.query(sql, ROW_MAPPER, email);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public User save(User user) {
        if (user.getRole() == null) {
            user.setRole(User.Role.USER);
        }

        if (user.getId() == null) {
            String sql = "INSERT INTO users (first_name, last_name, phone_number, email, description, email_verified, role, password_hash) VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
            Long generatedId = jdbcTemplate.queryForObject(
                    sql,
                    Long.class,
                    user.getFirstName(),
                    user.getLastName(),
                    user.getPhoneNumber(),
                    user.getEmail(),
                    user.getDescription(),
                    user.isEmailVerified(),
                    user.getRole().name(),
                    user.getPasswordHash()
            );
            if (generatedId != null) {
                user.setId(generatedId);
            }
            return user;
        }

        String sql = "UPDATE users SET first_name = ?, last_name = ?, phone_number = ?, email = ?, description = ?, email_verified = ?, role = ?, password_hash = ? WHERE id = ?";
        jdbcTemplate.update(sql, user.getFirstName(), user.getLastName(), user.getPhoneNumber(), user.getEmail(), user.getDescription(), user.isEmailVerified(), user.getRole().name(), user.getPasswordHash(), user.getId());
        return user;
    }

    @Override
    public boolean deleteById(Long id) {
        String sql = "DELETE FROM users WHERE id = ?";
        return jdbcTemplate.update(sql, id) > 0;
    }
}
