package hexlet.code.repository;

import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class BaseRepository {
    public static HikariDataSource dataSource;

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
