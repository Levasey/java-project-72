package hexlet.code.repository;

import hexlet.code.model.Url;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UrlRepository {
    public static void save(Url url) throws SQLException {
        String sql = "INSERT INTO urls (name, created_at) VALUES (?, ?)";

        try (var connection = BaseRepository.dataSource.getConnection();
             var preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            preparedStatement.setString(1, url.getName());
            preparedStatement.setTimestamp(2, Timestamp.valueOf(url.getCreatedAt()));
            preparedStatement.executeUpdate();

            var generatedKeys = preparedStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                url.setId(generatedKeys.getLong(1));
            }
        }
    }

    public static List<Url> findAll() throws SQLException {
        String sql = "SELECT * FROM urls ORDER BY created_at DESC";
        var result = new ArrayList<Url>();

        try (var connection = BaseRepository.dataSource.getConnection();
             var statement = connection.createStatement();
             var resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                result.add(extractUrl(resultSet));
            }
        }
        return result;
    }

    public static Optional<Url> findById(Long id) throws SQLException {
        String sql = "SELECT * FROM urls WHERE id = ?";

        try (var connection = BaseRepository.dataSource.getConnection();
             var preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setLong(1, id);
            var resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return Optional.of(extractUrl(resultSet));
            }
            return Optional.empty();
        }
    }

    public static Optional<Url> findByName(String name) throws SQLException {
        String sql = "SELECT * FROM urls WHERE name = ?";

        try (var connection = BaseRepository.dataSource.getConnection();
             var preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, name);
            var resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return Optional.of(extractUrl(resultSet));
            }
            return Optional.empty();
        }
    }

    private static Url extractUrl(ResultSet resultSet) throws SQLException {
        var url = new Url();
        url.setId(resultSet.getLong("id"));
        url.setName(resultSet.getString("name"));

        Timestamp timestamp = resultSet.getTimestamp("created_at");
        if (timestamp != null) {
            url.setCreatedAt(timestamp.toLocalDateTime());
        }

        return url;
    }
}