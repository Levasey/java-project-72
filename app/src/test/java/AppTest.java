import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import hexlet.code.App;
import hexlet.code.model.Url;
import hexlet.code.model.UrlCheck;
import hexlet.code.repository.UrlCheckRepository;
import hexlet.code.repository.UrlRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import utils.TestUtils;

public class AppTest {

    private Javalin app;
    private static MockWebServer mockWebServer;
    private Map<String, Object> existingUrl;
    private Map<String, Object> existingUrlCheck;
    private HikariDataSource dataSource;

    private static String getDatabaseUrl() {
        return System.getenv().getOrDefault("JDBC_DATABASE_URL", "jdbc:h2:mem:project");
    }

    @BeforeAll
    public static void beforeAll() throws IOException {

        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @BeforeEach
    public final void setUp() throws IOException, SQLException {
        app = App.getApp();

        var hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(getDatabaseUrl());

        dataSource = new HikariDataSource(hikariConfig);

        var schema = AppTest.class.getClassLoader().getResource("schema.sql");
        var file = new File(schema.getFile());

        var sql = Files.lines(file.toPath())
                .collect(Collectors.joining("\n"));

        try (var connection = dataSource.getConnection();
             var statement = connection.createStatement()) {
            statement.execute(sql);
        }

        String url = "https://www.example.com";

        TestUtils.addUrl(dataSource, url);
        existingUrl = TestUtils.getUrlByName(dataSource, url);

        TestUtils.addUrlCheck(dataSource, (long) existingUrl.get("id"));
        existingUrlCheck = TestUtils.getUrlCheck(dataSource, (long) existingUrl.get("id"));
    }

    @AfterAll
    public static void afterAll() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    public void testMainPage() {
        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/");
            assertThat(response.code()).isEqualTo(200);
            String body = response.body().string();
            assertThat(body).contains("Анализатор страниц");
            assertThat(body).contains("Бесплатно проверяйте сайты на SEO пригодность");
        });
    }

    @Test
    public void testUrlsPage() {
        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/urls");
            assertThat(response.code()).isEqualTo(200);
            String body = response.body().string();
            assertThat(body).contains("Сайты");
            assertThat(body).contains("ID");
            assertThat(body).contains("Имя");
        });
    }

    @Test
    public void testShowPage() throws SQLException {
        Url url = new Url("https://www.example.com");
        UrlRepository.save(url);
        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/urls/" + url.getId());
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.body().string())
                    .contains("Сайт:")
                    .contains("https://www.example.com")
                    .contains("ID")
                    .contains(String.valueOf(url.getId()))
                    .contains("Имя")
                    .contains(url.getName())
                    .contains("Дата создания")
                    .contains(url.getFormattedCreatedAt());
        });
    }

    @Test
    public void testShowPageNotFound() {
        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/urls/999");
            assertThat(response.code()).isEqualTo(404);
        });
    }

    @Test
    public void testAddValidUrl() {
        JavalinTest.test(app, (server, client) -> {
            var requestBody = "url=https://www.example.com";
            var response = client.post("/urls", requestBody);

            assertThat(response.code()).isEqualTo(200);

            // Проверяем базу данных
            List<Url> urls = UrlRepository.findAll();
            assertThat(urls).hasSize(1);
            assertThat(urls.getFirst().getName()).isEqualTo("https://www.example.com");

            // Проверяем отображение на странице
            var urlsResponse = client.get("/urls");
            String body = urlsResponse.body().string();
            assertThat(body).contains("https://www.example.com");
        });
    }

    @Test
    public void testAddDuplicateUrl() {
        JavalinTest.test(app, (server, client) -> {
            // Первый запрос
            var requestBody1 = "url=https://www.example.com";
            client.post("/urls", requestBody1);

            // Второй запрос с тем же URL
            var requestBody2 = "url=https://www.example.com";
            var response = client.post("/urls", requestBody2);

            assertThat(response.code()).isEqualTo(200);

            List<Url> urls = UrlRepository.findAll();
            assertThat(urls).hasSize(1);
        });
    }

    @Test
    public void testUrlNormalization() {
        JavalinTest.test(app, (server, client) -> {
            // Тест нормализации URL
            var testUrl = "https://www.example.com:443/path?query=param";
            var response = client.post("/urls", "url=" + testUrl);
            assertThat(response.code()).isEqualTo(200);

            List<Url> urls = UrlRepository.findAll();
            assertThat(urls).hasSize(1);
            assertThat(urls.getFirst().getName()).isEqualTo("https://www.example.com");
        });
    }

    @Test
    public void testCreateUrlCheckSuccess() throws SQLException {
        // Настраиваем mock сервер
        String mockHtml = """
        <!DOCTYPE html>
        <html>
        <head>
            <title>Test Page</title>
            <meta name="description" content="Test description">
        </head>
        <body>
            <h1>Test Header</h1>
            <p>Test content</p>
        </body>
        </html>
        """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(mockHtml)
                .setHeader("Content-Type", "text/html"));

        String mockUrl = mockWebServer.url("/").toString();
        Url url = new Url(mockUrl);
        UrlRepository.save(url);

        System.out.println("Created URL with id: " + url.getId());

        JavalinTest.test(app, (server, client) -> {
            var response = client.post("/urls/" + url.getId() + "/checks");
            assertThat(response.code()).isEqualTo(200);

            // Проверяем базу данных
            List<UrlCheck> checks = UrlCheckRepository.findByUrlId(url.getId());
            System.out.println("Found " + checks.size() + " checks for urlId: " + url.getId());

            assertThat(checks).hasSize(1);

            UrlCheck check = checks.getFirst();
            assertThat(check).isNotNull();
            assertThat(check.getStatusCode()).isEqualTo(200);
            assertThat(check.getTitle()).isEqualTo("Test Page");
            assertThat(check.getH1()).isEqualTo("Test Header");
            assertThat(check.getDescription()).isEqualTo("Test description");
        });
    }

    @Test
    public void testCreateUrlCheckWithMissingElements() throws SQLException {
        // HTML без некоторых элементов
        String mockHtml = """
            <!DOCTYPE html>
            <html>
            <body>
                <p>Test content</p>
            </body>
            </html>
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(mockHtml));

        String mockUrl = mockWebServer.url("/").toString();
        Url url = new Url(mockUrl);
        UrlRepository.save(url);

        JavalinTest.test(app, (server, client) -> {
            var response = client.post("/urls/" + url.getId() + "/checks");

            assertThat(response.code()).isEqualTo(200);

            List<UrlCheck> checks = UrlCheckRepository.findByUrlId(url.getId());
            assertThat(checks).hasSize(1);

            UrlCheck check = checks.getFirst();
            assertThat(check).isNotNull();
            assertThat(check.getStatusCode()).isEqualTo(200);
            assertThat(check.getTitle()).isNullOrEmpty();
            assertThat(check.getH1()).isNullOrEmpty();
            assertThat(check.getDescription()).isNullOrEmpty();
        });
    }

    @Test
    public void testCreateUrlCheckServerError() throws SQLException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        String mockUrl = mockWebServer.url("/").toString();
        Url url = new Url(mockUrl);
        UrlRepository.save(url);

        JavalinTest.test(app, (server, client) -> {
            var response = client.post("/urls/" + url.getId() + "/checks");

            assertThat(response.code()).isEqualTo(200);

            List<UrlCheck> checks = UrlCheckRepository.findByUrlId(url.getId());
            assertThat(checks).hasSize(1);

            UrlCheck check = checks.getFirst();
            assertThat(check).isNotNull();
            assertThat(check.getStatusCode()).isEqualTo(500);
        });
    }

    @Test
    public void testCreateUrlCheckNetworkError() throws SQLException {
        // Используем несуществующий URL для имитации сетевой ошибки
        Url url = new Url("https://nonexistent-domain-12345.test");
        UrlRepository.save(url);

        JavalinTest.test(app, (server, client) -> {
            var response = client.post("/urls/" + url.getId() + "/checks");

            assertThat(response.code()).isEqualTo(200);

            // Проверяем, что проверка не была создана при сетевой ошибке
            List<UrlCheck> checks = UrlCheckRepository.findByUrlId(url.getId());
            assertThat(checks).isEmpty();
        });
    }

    @Test
    public void testCreateUrlCheckForNonExistentUrl() {
        JavalinTest.test(app, (server, client) -> {
            var response = client.post("/urls/999/checks");
            assertThat(response.code()).isEqualTo(404);
        });
    }

    @Test
    public void testUrlCheckDisplayOnShowPage() throws SQLException {
        // Настраиваем mock сервер
        String mockHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Test Page</title>
                <meta name="description" content="Test description">
            </head>
            <body>
                <h1>Test Header</h1>
            </body>
            </html>
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(mockHtml));

        String mockUrl = mockWebServer.url("/").toString();
        Url url = new Url(mockUrl);
        UrlRepository.save(url);

        JavalinTest.test(app, (server, client) -> {
            // Создаем проверку
            client.post("/urls/" + url.getId() + "/checks");

            var response = client.get("/urls/" + url.getId());
            assertThat(response.code()).isEqualTo(200);

            String body = response.body().string();
            assertThat(body)
                    .contains("Проверки")
                    .contains("200") // status code
                    .contains("Test Page") // title
                    .contains("Test Header") // h1
                    .contains("Test description"); // description
        });
    }

    @Test
    public void testLatestCheckDisplayOnUrlsPage() throws SQLException {
        // Настраиваем mock сервер
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(201)
                .setBody("<title>Test Page</title>"));

        String mockUrl = mockWebServer.url("/").toString();
        Url url = new Url(mockUrl);
        UrlRepository.save(url);

        JavalinTest.test(app, (server, client) -> {
            // Создаем проверку
            client.post("/urls/" + url.getId() + "/checks");

            var response = client.get("/urls");
            assertThat(response.code()).isEqualTo(200);

            String body = response.body().string();
            assertThat(body)
                    .contains("201") // status code последней проверки
                    .contains(mockUrl.replaceFirst("/$", "")); // URL без trailing slash
        });
    }
}
