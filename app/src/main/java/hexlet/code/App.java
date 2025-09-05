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

    private static String getDatabaseUrl() {
        return System.getenv().getOrDefault("JDBC_DATABASE_URL", "jdbc:h2:mem:project;DB_CLOSE_DELAY=-1;");
    }

    private static String readResourceFile(String fileName) throws IOException {
        var inputStream = App.class.getClassLoader().getResourceAsStream(fileName);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    private static TemplateEngine createTemplateEngine() {
        ClassLoader classLoader = App.class.getClassLoader();
        ResourceCodeResolver codeResolver = new ResourceCodeResolver("templates", classLoader);
        TemplateEngine templateEngine = TemplateEngine.create(codeResolver, ContentType.Html);
        return templateEngine;
    }

    public static Javalin getApp() throws IOException, SQLException {

        var hikariConfig = new HikariConfig();
        String databaseUrl = getDatabaseUrl();

        // Настройка для разных типов баз данных
        if (databaseUrl.startsWith("jdbc:h2:")) {
            // H2 база данных
            hikariConfig.setJdbcUrl(databaseUrl);
            hikariConfig.setDriverClassName("org.h2.Driver");
        } else if (databaseUrl.startsWith("jdbc:postgresql:")) {
            // PostgreSQL база данных
            hikariConfig.setJdbcUrl(databaseUrl);
            hikariConfig.setDriverClassName("org.postgresql.Driver");

            // Дополнительные настройки для PostgreSQL
            hikariConfig.setMaximumPoolSize(10);
            hikariConfig.setMinimumIdle(2);
            hikariConfig.setIdleTimeout(30000);
            hikariConfig.setConnectionTimeout(20000);
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
        app.get("/urls/{id}", UrlsController::show);

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
        Javalin app = getApp();
        app.start(getPort());
    }
}
