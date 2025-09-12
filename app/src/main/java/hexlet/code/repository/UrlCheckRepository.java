package hexlet.code.repository;

import hexlet.code.model.UrlCheck;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class UrlCheckRepository {
    public static void save(UrlCheck urlCheck) throws SQLException {
        String sql = "INSERT INTO url_checks (status_code, title, h1, description, url_id, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        System.out.println("Saving UrlCheck: statusCode=" + urlCheck.getStatusCode() +
                ", title=" + urlCheck.getTitle() +
                ", h1=" + urlCheck.getH1() +
                ", description=" + urlCheck.getDescription() +
                ", urlId=" + urlCheck.getUrlId());

        try (var conn = BaseRepository.dataSource.getConnection();
             var stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, urlCheck.getStatusCode());
            stmt.setString(2, urlCheck.getTitle());
            stmt.setString(3, urlCheck.getH1());
            stmt.setString(4, urlCheck.getDescription());
            stmt.setLong(5, urlCheck.getUrlId());
            stmt.setTimestamp(6, Timestamp.valueOf(urlCheck.getCreatedAt()));

            int affectedRows = stmt.executeUpdate();
            System.out.println("Affected rows: " + affectedRows);

            var generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                urlCheck.setId(generatedKeys.getLong(1));
                System.out.println("Saved UrlCheck with id: " + urlCheck.getId());
            } else {
                throw new SQLException("DB have not returned an id after saving an entity");
            }
        }
    }

    public static List<UrlCheck> findByUrlId(Long urlId) throws SQLException {
        String sql = "SELECT * FROM url_checks WHERE url_id = ? ORDER BY created_at DESC";

        try (var conn = BaseRepository.dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, urlId);
            var resultSet = stmt.executeQuery();

            var result = new ArrayList<UrlCheck>();
            while (resultSet.next()) {
                long id = resultSet.getLong("id");
                int statusCode = resultSet.getInt("status_code");
                String title = resultSet.getString("title");
                String h1 = resultSet.getString("h1");
                String description = resultSet.getString("description");
                LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();

                UrlCheck urlCheck = new UrlCheck();
                urlCheck.setId(id);
                urlCheck.setStatusCode(statusCode);
                urlCheck.setTitle(title);
                urlCheck.setH1(h1);
                urlCheck.setDescription(description);
                urlCheck.setUrlId(urlId);
                urlCheck.setCreatedAt(createdAt);

                result.add(urlCheck);
            }
            return result;
        }
    }

    public static Optional<UrlCheck> findLatestCheck(Long urlId) throws SQLException {
        String sql = "SELECT * FROM url_checks WHERE url_id = ? ORDER BY created_at DESC LIMIT 1";

        try (var conn = BaseRepository.dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, urlId);
            var resultSet = stmt.executeQuery();

            if (resultSet.next()) {
                long id = resultSet.getLong("id");
                int statusCode = resultSet.getInt("status_code");
                String title = resultSet.getString("title");
                String h1 = resultSet.getString("h1");
                String description = resultSet.getString("description");
                LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();

                UrlCheck urlCheck = new UrlCheck();
                urlCheck.setId(id);
                urlCheck.setStatusCode(statusCode);
                urlCheck.setTitle(title);
                urlCheck.setH1(h1);
                urlCheck.setDescription(description);
                urlCheck.setUrlId(urlId);
                urlCheck.setCreatedAt(createdAt);

                return Optional.of(urlCheck);
            }
            return Optional.empty();
        }
    }

    public static Optional<UrlCheck> findById(Long id) throws SQLException {
        String sql = "SELECT * FROM url_checks WHERE id = ?";

        try (var conn = BaseRepository.dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            var resultSet = stmt.executeQuery();

            if (resultSet.next()) {
                long checkId = resultSet.getLong("id");
                int statusCode = resultSet.getInt("status_code");
                String title = resultSet.getString("title");
                String h1 = resultSet.getString("h1");
                String description = resultSet.getString("description");
                Long urlId = resultSet.getLong("url_id");
                LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();

                UrlCheck urlCheck = new UrlCheck();
                urlCheck.setId(checkId);
                urlCheck.setStatusCode(statusCode);
                urlCheck.setTitle(title);
                urlCheck.setH1(h1);
                urlCheck.setDescription(description);
                urlCheck.setUrlId(urlId);
                urlCheck.setCreatedAt(createdAt);

                return Optional.of(urlCheck);
            }
            return Optional.empty();
        }
    }
}
