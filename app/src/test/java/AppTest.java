import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import hexlet.code.App;
import hexlet.code.model.Url;
import hexlet.code.model.UrlCheck;
import hexlet.code.repository.BaseRepository;
import hexlet.code.repository.UrlCheckRepository;
import hexlet.code.repository.UrlRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

public class AppTest {

    private Javalin app;
    private MockWebServer mockWebServer;

    @BeforeEach
    public final void setUp() throws IOException, SQLException {
        // Устанавливаем тестовый режим
        System.setProperty("test", "true");
        app = App.getApp();
        clearDatabase();

        // Инициализируем MockWebServer
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // Ждем пока сервер полностью запустится
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @AfterEach
    public final void tearDown() {
        if (mockWebServer != null) {
            try {
                mockWebServer.shutdown();
            } catch (Exception e) {
                // Игнорируем ошибки при закрытии, чтобы не мешать другим тестам
                System.out.println("Error shutting down MockWebServer: " + e.getMessage());
            }
        }
    }

    private void clearDatabase() throws SQLException {
        try (var connection = BaseRepository.getConnection();
             var statement = connection.createStatement()) {
            statement.execute("DELETE FROM url_checks");
            statement.execute("DELETE FROM urls");
        }
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
    public void testAddValidUrl() throws SQLException {
        JavalinTest.test(app, (server, client) -> {
            var requestBody = "url=https://www.example.com";
            var response = client.post("/urls", requestBody);

            assertThat(response.code()).isEqualTo(200);

            // Проверяем базу данных
            List<Url> urls = UrlRepository.findAll();
            assertThat(urls).hasSize(1);
            assertThat(urls.get(0).getName()).isEqualTo("https://www.example.com");

            // Проверяем отображение на странице
            var urlsResponse = client.get("/urls");
            String body = urlsResponse.body().string();
            assertThat(body).contains("https://www.example.com");
        });
    }

    @Test
    public void testAddInvalidUrl() throws SQLException {
        JavalinTest.test(app, (server, client) -> {
            var requestBody = "url=invalid-url";
            var response = client.post("/urls", requestBody);

            assertThat(response.code()).isEqualTo(200);

            List<Url> urls = UrlRepository.findAll();
            assertThat(urls).isEmpty();
        });
    }

    @Test
    public void testAddDuplicateUrl() throws SQLException {
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
    public void testAddEmptyUrl() throws SQLException {
        JavalinTest.test(app, (server, client) -> {
            var requestBody = "url=";
            var response = client.post("/urls", requestBody);

            assertThat(response.code()).isEqualTo(200);

            List<Url> urls = UrlRepository.findAll();
            assertThat(urls).isEmpty();
        });
    }

    @Test
    public void testUrlNormalization() throws SQLException {
        JavalinTest.test(app, (server, client) -> {
            // Тест нормализации URL
            var testUrl = "https://www.example.com:443/path?query=param";
            var response = client.post("/urls", "url=" + testUrl);
            assertThat(response.code()).isEqualTo(200);

            List<Url> urls = UrlRepository.findAll();
            assertThat(urls).hasSize(1);
            assertThat(urls.get(0).getName()).isEqualTo("https://www.example.com");
        });
    }

    @Test
    public void testCreateUrlCheckSuccess() throws SQLException, IOException {
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

            // Даем время для обработки
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Проверяем базу данных
            List<UrlCheck> checks = UrlCheckRepository.findByUrlId(url.getId());
            System.out.println("Found " + checks.size() + " checks for urlId: " + url.getId());

            assertThat(checks).hasSize(1);

            UrlCheck check = checks.get(0);
            assertThat(check).isNotNull();
            assertThat(check.getId()).isNotNull();
            assertThat(check.getStatusCode()).isEqualTo(200);
            assertThat(check.getTitle()).isEqualTo("Test Page");
            assertThat(check.getH1()).isEqualTo("Test Header");
            assertThat(check.getDescription()).isEqualTo("Test description");
        });
    }

    @Test
    public void testCreateUrlCheckWithMissingElements() throws SQLException, IOException {
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

            UrlCheck check = checks.get(0);
            assertThat(check).isNotNull();
            assertThat(check.getStatusCode()).isEqualTo(200);
            assertThat(check.getTitle()).isNullOrEmpty();
            assertThat(check.getH1()).isNullOrEmpty();
            assertThat(check.getDescription()).isNullOrEmpty();
        });
    }

    @Test
    public void testCreateUrlCheckServerError() throws SQLException, IOException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        String mockUrl = mockWebServer.url("/").toString();
        Url url = new Url(mockUrl);
        UrlRepository.save(url);

        JavalinTest.test(app, (server, client) -> {
            var response = client.post("/urls/" + url.getId() + "/checks");

            assertThat(response.code()).isEqualTo(200);

            List<UrlCheck> checks = UrlCheckRepository.findByUrlId(url.getId());
            assertThat(checks).hasSize(1);

            UrlCheck check = checks.get(0);
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
    public void testUrlCheckDisplayOnShowPage() throws SQLException, IOException {
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
    public void testMultipleUrlChecks() throws SQLException, IOException {
        // Первая проверка
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("<title>First Check</title>"));

        // Вторая проверка
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(404)
                .setBody("<title>Second Check</title>"));

        String mockUrl = mockWebServer.url("/").toString();
        Url url = new Url(mockUrl);
        UrlRepository.save(url);

        JavalinTest.test(app, (server, client) -> {
            // Первая проверка
            client.post("/urls/" + url.getId() + "/checks");

            // Вторая проверка
            client.post("/urls/" + url.getId() + "/checks");

            List<UrlCheck> checks = UrlCheckRepository.findByUrlId(url.getId());
            assertThat(checks).hasSize(2);

            // Проверяем порядок (последняя проверка должна быть первой)
            assertThat(checks.get(0)).isNotNull();
            assertThat(checks.get(1)).isNotNull();
            assertThat(checks.get(0).getStatusCode()).isEqualTo(404);
            assertThat(checks.get(1).getStatusCode()).isEqualTo(200);
        });
    }

    @Test
    public void testLatestCheckDisplayOnUrlsPage() throws SQLException, IOException {
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
