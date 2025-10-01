import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import hexlet.code.App;
import hexlet.code.model.Url;
import hexlet.code.model.UrlCheck;
import hexlet.code.repository.UrlCheckRepository;
import hexlet.code.repository.UrlRepository;
import org.junit.jupiter.api.*;

import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

public class AppTest {

    private Javalin app;
    private static MockWebServer mockWebServer;

    private static Path getFixturePath(String fileName) {
        return Paths.get("src", "test", "resources", "fixtures", fileName)
                .toAbsolutePath().normalize();
    }

    private static String readFixture(String fileName) throws IOException {
        Path filePath = getFixturePath(fileName);
        return Files.readString(filePath).trim();
    }

    @BeforeAll
    public static void beforeAll() throws IOException {
        mockWebServer = new MockWebServer();

        // Настраиваем ответ для любого пути
        MockResponse mockedResponse = new MockResponse()
                .setBody(readFixture("index.html"))
                .setResponseCode(200);

        // Добавляем несколько ответов на случай multiple requests
        mockWebServer.enqueue(mockedResponse);
        mockWebServer.enqueue(mockedResponse);
        mockWebServer.enqueue(mockedResponse);

        mockWebServer.start();
    }

    @AfterAll
    public static void afterAll() throws IOException {
        mockWebServer.shutdown();
    }

    @BeforeEach
    public void setUp() throws IOException, SQLException {
        app = App.getApp();
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
    public void testAddInvalidUrl() {
        JavalinTest.test(app, (server, client) -> {
            var requestBody = "url=invalid-url";
            var response = client.post("/urls", requestBody);

            assertThat(response.code()).isEqualTo(200);

            List<Url> urls = UrlRepository.findAll();
            assertThat(urls).isEmpty();
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
    public void testAddEmptyUrl() {
        JavalinTest.test(app, (server, client) -> {
            var requestBody = "url=";
            var response = client.post("/urls", requestBody);

            assertThat(response.code()).isEqualTo(200);

            List<Url> urls = UrlRepository.findAll();
            assertThat(urls).isEmpty();
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
            assertThat(urls.get(0).getName()).isEqualTo("https://www.example.com");
        });
    }

    @Test
    public void testStore() throws SQLException {
        // Создаем URL
        Url url = new Url("https://www.example.com");
        UrlRepository.save(url);

        // Создаем проверку
        UrlCheck urlCheck = new UrlCheck();
        urlCheck.setStatusCode(200);
        urlCheck.setTitle("Test Title");
        urlCheck.setH1("Test H1");
        urlCheck.setDescription("Test Description");
        urlCheck.setUrlId(url.getId());
        urlCheck.setCreatedAt(LocalDateTime.now());

        // Сохраняем проверку
        UrlCheckRepository.save(urlCheck);

        // Ищем проверку по ID
        Optional<UrlCheck> foundCheck = UrlCheckRepository.findById(urlCheck.getId());
        assertThat(foundCheck).isPresent();

        // Проверяем поля
        UrlCheck check = foundCheck.get();
        assertThat(check.getStatusCode()).isEqualTo(200);
        assertThat(check.getTitle()).isEqualTo("Test Title");
        assertThat(check.getH1()).isEqualTo("Test H1");
        assertThat(check.getDescription()).isEqualTo("Test Description");
        assertThat(check.getUrlId()).isEqualTo(url.getId());
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
    public void testCreateUrlCheckForNonExistentUrl() {
        JavalinTest.test(app, (server, client) -> {
            var response = client.post("/urls/999/checks");
            assertThat(response.code()).isEqualTo(404);
        });
    }

    @Test
    void testUrlCheckTestStore() throws IOException, SQLException {
        String url = mockWebServer.url("/").toString().replaceAll("/$", "");

        JavalinTest.test(app, (server, client) -> {
            var requestBody = "url=" + url;
            assertThat(client.post("/urls", requestBody).code()).isEqualTo(200);

            // Находим созданный URL
            List<Url> urls = UrlRepository.findAll();
            assertThat(urls).isNotEmpty();

            Url actualUrl = urls.stream()
                    .filter(u -> u.getName().equals(url))
                    .findFirst()
                    .orElseThrow();

            assertThat(actualUrl).isNotNull();
            assertThat(actualUrl.getName()).isEqualTo(url);

            // Создаем проверку
            client.post("/urls/" + actualUrl.getId() + "/checks");

            // Проверяем что страница отображается
            assertThat(client.get("/urls/" + actualUrl.getId()).code())
                    .isEqualTo(200);

            // Проверяем что проверка создана с правильными данными
            List<UrlCheck> checks = UrlCheckRepository.findByUrlId(actualUrl.getId());
            assertThat(checks).isNotEmpty();

            UrlCheck actualCheck = checks.get(0);
            assertThat(actualCheck).isNotNull();
            assertThat(actualCheck.getTitle()).isEqualTo("Test page");
            assertThat(actualCheck.getH1()).isEqualTo("Do not expect a miracle, miracles yourself!");
            assertThat(actualCheck.getDescription()).isEqualTo("statements of great people");
        });
    }
}