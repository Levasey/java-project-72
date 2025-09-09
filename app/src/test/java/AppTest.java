import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import hexlet.code.App;
import hexlet.code.model.Url;
import hexlet.code.repository.BaseRepository;
import hexlet.code.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;

public class AppTest {

    private Javalin app;

    @BeforeEach
    public final void setUp() throws IOException, SQLException {
        // Устанавливаем тестовый режим
        System.setProperty("test", "true");
        app = App.getApp();
        clearDatabase();
    }

    private void clearDatabase() throws SQLException {
        try (var connection = BaseRepository.getConnection();
             var statement = connection.createStatement()) {
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
}