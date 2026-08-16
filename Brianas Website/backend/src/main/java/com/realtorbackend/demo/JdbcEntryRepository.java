package com.realtorbackend.demo;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcEntryRepository implements EntryRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final RowMapper<Entry> ROW_MAPPER = (rs, rowNum) -> new Entry(
            rs.getLong("id"),
            rs.getString("address"),
            rs.getString("city"),
            rs.getString("state"),
            rs.getString("zipcode"),
            rs.getBigDecimal("price"),
            rs.getString("picture_url"),
            rs.getString("status")
    );

    public JdbcEntryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        jdbcTemplate.execute("ALTER TABLE entries DROP COLUMN IF EXISTS name");
        jdbcTemplate.execute("ALTER TABLE entries DROP COLUMN IF EXISTS description");
    }

    @Override
    public List<Entry> findAll() {
        String sql = "SELECT id, address, city, state, zipcode, price, picture_url, status, picture_urls FROM entries ORDER BY id DESC";
        return withGalleries(jdbcTemplate.query(sql, ROW_MAPPER));
    }

    @Override
    public Optional<Entry> findById(Long id) {
        String sql = "SELECT id, address, city, state, zipcode, price, picture_url, status, picture_urls FROM entries WHERE id = ?";
        List<Entry> rows = jdbcTemplate.query(sql, ROW_MAPPER, id);
        return rows.isEmpty() ? Optional.empty() : Optional.of(withGalleries(rows).get(0));
    }

    @Override
    public Entry save(Entry entry) {
        if (entry.getId() == null) {
            String sql = "INSERT INTO entries (address, city, state, zipcode, price, picture_url, status, picture_urls) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, new String[] { "id" });
                ps.setString(1, entry.getAddress());
                ps.setString(2, entry.getCity());
                ps.setString(3, entry.getState());
                ps.setString(4, entry.getZipcode());
                ps.setBigDecimal(5, entry.getPrice());
                ps.setString(6, entry.getPictureUrl());
                ps.setString(7, entry.getStatus());
                ps.setString(8, writePictures(entry));
                return ps;
            }, keyHolder);

            Number key = keyHolder.getKey();
            if (key != null) {
                entry.setId(key.longValue());
            }
            return entry;
        }

        String sql = "UPDATE entries SET address = ?, city = ?, state = ?, zipcode = ?, price = ?, picture_url = ?, status = ?, picture_urls = ? WHERE id = ?";
        jdbcTemplate.update(sql, entry.getAddress(), entry.getCity(), entry.getState(), entry.getZipcode(),
            entry.getPrice(), entry.getPictureUrl(), entry.getStatus(), writePictures(entry), entry.getId());
        return entry;
    }

    private List<Entry> withGalleries(List<Entry> entries) {
        for (Entry entry : entries) {
            String json = jdbcTemplate.queryForObject("SELECT picture_urls FROM entries WHERE id = ?", String.class, entry.getId());
            try {
                entry.setPictureUrls(json == null ? new ArrayList<>() : objectMapper.readValue(json, new TypeReference<List<String>>() {}));
            } catch (Exception ignored) {
                entry.setPictureUrls(new ArrayList<>());
            }
            if (entry.getPictureUrl() != null && !entry.getPictureUrls().contains(entry.getPictureUrl())) {
                entry.getPictureUrls().add(0, entry.getPictureUrl());
            }
        }
        return entries;
    }

    private String writePictures(Entry entry) {
        try {
            return objectMapper.writeValueAsString(entry.getPictureUrls());
        } catch (Exception exception) {
            throw new IllegalArgumentException("Listing pictures could not be saved", exception);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        String sql = "DELETE FROM entries WHERE id = ?";
        return jdbcTemplate.update(sql, id) > 0;
    }
}
