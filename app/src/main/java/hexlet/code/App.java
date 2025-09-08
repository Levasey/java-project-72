package hexlet.code;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.resolve.ResourceCodeResolver;
import hexlet.code.controller.UrlsController;
import hexlet.code.dto.BasePage;
import hexlet.code.repository.BaseRepository;
import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinJte;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.stream.Collectors;

import static io.javalin.rendering.template.TemplateUtil.model;

@Slf4j
public class App {
    private static int getPort() {
        String port = System.getenv().getOrDefault("PORT", "7070");
        return Integer.valueOf(port);
    }

    private static String readResourceFile(String fileName) throws IOException {
        var inputStream = App.class.getClassLoader().getResourceAsStream(fileName);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    private static HikariDataSource createDataSource() {
        HikariConfig config = new HikariConfig();

        String jdbcUrl = System.getenv("JDBC_DATABASE_URL");
        String databaseUrl = System.getenv("DATABASE_URL");

        if (jdbcUrl != null && !jdbcUrl.isEmpty()) {
            config.setJdbcUrl(jdbcUrl);
        } else if (databaseUrl != null && !databaseUrl.isEmpty()) {
            // Конвертируем DATABASE_URL в JDBC format
            config.setJdbcUrl(convertDatabaseUrlToJdbc(databaseUrl));
        } else {
            // Локальная разработка с H2
            config.setJdbcUrl("jdbc:h2:mem:project;DB_CLOSE_DELAY=-1");
        }

        // Дополнительные настройки для PostgreSQL
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(30000);

        return new HikariDataSource(config);
    }

    private static String convertDatabaseUrlToJdbc(String databaseUrl) {
        // Конвертация DATABASE_URL в JDBC format
        // Пример: postgresql://user:pass@host:port/dbname -> jdbc:postgresql://host:port/dbname?user=user&password=pass
        try {
            URI dbUri = new URI(databaseUrl);
            String username = dbUri.getUserInfo().split(":")[0];
            String password = dbUri.getUserInfo().split(":")[1];
            String host = dbUri.getHost();
            int port = dbUri.getPort();
            String path = dbUri.getPath();

            return String.format("jdbc:postgresql://%s:%d%s?user=%s&password=%s&ssl=true",
                    host, port, path, username, password);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert DATABASE_URL to JDBC format", e);
        }
    }

    private static TemplateEngine createTemplateEngine() {
        ClassLoader classLoader = App.class.getClassLoader();
        ResourceCodeResolver codeResolver = new ResourceCodeResolver("templates", classLoader);
        TemplateEngine templateEngine = TemplateEngine.create(codeResolver, ContentType.Html);
        return templateEngine;
    }

    public static Javalin getApp() throws IOException, SQLException {
        // Получаем порт из переменной окружения или используем по умолчанию
        int port = getPort();

        // Настраиваем подключение к БД
        HikariConfig hikariConfig = new HikariConfig();

        // Получаем URL БД из переменных окружения
        String jdbcUrl = System.getenv("JDBC_DATABASE_URL");
        if (jdbcUrl == null || jdbcUrl.isEmpty()) {
            // Для локальной разработки
            jdbcUrl = "jdbc:h2:mem:project;DB_CLOSE_DELAY=-1";
            hikariConfig.setJdbcUrl(jdbcUrl);
        } else {
            // Для продакшена (Render.com)
            hikariConfig.setJdbcUrl(jdbcUrl);
        }

        var dataSource = new HikariDataSource(hikariConfig);
        BaseRepository.dataSource = dataSource;

        // Инициализация базы данных
        initializeDatabase(dataSource);

        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableDevLogging();
            config.fileRenderer(new JavalinJte(createTemplateEngine()));
        });

        app.get("/", ctx -> {
            BasePage page = new BasePage();

            // Добавляем flash-сообщения из сессии
            String flash = ctx.sessionAttribute("flash");
            String flashType = ctx.sessionAttribute("flashType");

            if (flash != null) {
                page.setFlash(flash);
                page.setFlashType(flashType);
                // Очищаем flash-сообщения после использования
                ctx.sessionAttribute("flash", null);
                ctx.sessionAttribute("flashType", null);
            }

            ctx.render("index.jte", model("page", page));
        });
        app.post("/urls", UrlsController::create);
        app.get("/urls", UrlsController::index);
//        app.get("/urls/{id}", UrlsController::show);

        return app;
    }

    private static void initializeDatabase(HikariDataSource dataSource) throws SQLException, IOException {
        var sql = readResourceFile("schema.sql");

        try (var connection = dataSource.getConnection();
             var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    public static void main(String[] args) throws IOException, SQLException {
        // Явно загружаем драйвер PostgreSQL
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("PostgreSQL Driver not found", e);
        }

        HikariDataSource dataSource = createDataSource();
        BaseRepository.dataSource = dataSource;

        Javalin app = getApp();
        app.start(getPort());
    }
}
