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
        if (inputStream == null) {
            throw new IOException("Resource file not found: " + fileName);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    private static HikariDataSource createDataSource() throws SQLException {
        HikariConfig config = new HikariConfig();

        String jdbcUrl = System.getenv("JDBC_DATABASE_URL");

        System.out.println("Database URL: " + (jdbcUrl != null ? jdbcUrl : "not set"));

        if (jdbcUrl != null && !jdbcUrl.isEmpty()) {
            // Продакшен на Render с PostgreSQL
            config.setJdbcUrl(jdbcUrl);
        } else {
            // Локальная разработка с H2
            config.setJdbcUrl("jdbc:h2:mem:project;DB_CLOSE_DELAY=-1");
        }

        return new HikariDataSource(config);
    }

    private static TemplateEngine createTemplateEngine() {
        ClassLoader classLoader = App.class.getClassLoader();
        ResourceCodeResolver codeResolver = new ResourceCodeResolver("templates", classLoader);
        TemplateEngine templateEngine = TemplateEngine.create(codeResolver, ContentType.Html);
        return templateEngine;
    }

    public static Javalin getApp() throws IOException, SQLException {
        // Создаем источник данных
        HikariDataSource dataSource = createDataSource();
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
        app.get("/urls/{id}", UrlsController::show);

        return app;
    }

    private static void initializeDatabase(HikariDataSource dataSource) throws SQLException {
        try {
            var sql = readResourceFile("schema.sql");
            System.out.println("Initializing database...");

            try (var connection = dataSource.getConnection();
                 var statement = connection.createStatement()) {
                statement.execute(sql);
                System.out.println("Database initialized successfully");
            }
        } catch (IOException e) {
            System.out.println("Warning: schema.sql not found, using empty database");
        }
    }

    public static void main(String[] args) {
        try {
            System.out.println("Starting application...");

            // Пробуем загрузить драйверы
            try {
                Class.forName("org.postgresql.Driver");
                System.out.println("PostgreSQL driver loaded");
            } catch (ClassNotFoundException e) {
                System.out.println("PostgreSQL driver not found");
            }

            try {
                Class.forName("org.h2.Driver");
                System.out.println("H2 driver loaded");
            } catch (ClassNotFoundException e) {
                System.out.println("H2 driver not found");
            }

            Javalin app = getApp();
            int port = getPort();
            System.out.println("Starting server on port " + port);
            app.start(port);

        } catch (Exception e) {
            System.out.println("Application failed to start: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
