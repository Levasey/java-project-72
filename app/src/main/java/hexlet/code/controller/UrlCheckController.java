package hexlet.code.controller;

import hexlet.code.model.UrlCheck;
import hexlet.code.repository.UrlCheckRepository;
import hexlet.code.repository.UrlRepository;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Objects;

public class UrlCheckController {
    public static void create(Context ctx) throws SQLException {
        var id = ctx.pathParamAsClass("id", Long.class).get();

        // Находим URL по ID
        var url = UrlRepository.findById(id)
                .orElseThrow(() -> new NotFoundResponse("Url not found"));

        try {
            System.out.println("Checking URL: " + url.getName());

            // Выполняем HTTP-запрос с таймаутами
            var response = Unirest.get(url.getName())
                    .connectTimeout(5000)
                    .socketTimeout(5000)
                    .asString();

            int statusCode = response.getStatus();

            // Парсим HTML только если статус код успешный
            String title = "";
            String h1 = "";
            String description = "";

            if (statusCode >= 200 && statusCode < 400) {
                try {
                    Document doc = Jsoup.parse(response.getBody());
                    doc.title();
                    title = doc.title();
                    h1 = doc.selectFirst("h1") != null ? Objects.requireNonNull(doc.selectFirst("h1")).text() : "";
                    description = doc.selectFirst("meta[name=description]") != null
                            ? Objects.requireNonNull(doc.selectFirst("meta[name=description]")).attr("content")
                            : "";
                } catch (Exception e) {
                    System.out.println("Error parsing HTML: " + e.getMessage());
                    // Продолжаем с пустыми значениями
                }
            }

            System.out.println("Extracted data - Status: " + statusCode + ", Title: " + title + ", H1: " + h1 + ", Description: " + description);

            // Создаем и сохраняем проверку
            createAndSaveUrlCheck(id, statusCode, title, h1, description, ctx, true);

        } catch (UnirestException e) {
            System.out.println("Unirest exception: " + e.getMessage());
            // Сохраняем проверку с информацией об ошибке
            createAndSaveUrlCheck(id, 0, "", "", "", ctx, false);
        } catch (Exception e) {
            System.out.println("General exception: " + e.getMessage());
            // Сохраняем проверку с информацией об ошибке
            createAndSaveUrlCheck(id, 0, "", "", "", ctx, false);
        }

        ctx.redirect("/urls/" + id);
    }

    private static void createAndSaveUrlCheck(Long id, int statusCode, String title, String h1,
                                              String description, Context ctx, boolean success) throws SQLException {
        var urlCheck = new UrlCheck();
        urlCheck.setStatusCode(statusCode);
        urlCheck.setTitle(title != null ? title : "");
        urlCheck.setH1(h1 != null ? h1 : "");
        urlCheck.setDescription(description != null ? description : "");
        urlCheck.setUrlId(id);
        urlCheck.setCreatedAt(LocalDateTime.now());

        UrlCheckRepository.save(urlCheck);

        if (success) {
            ctx.sessionAttribute("flash", "Страница успешно проверена");
            ctx.sessionAttribute("flashType", "success");
        } else {
            ctx.sessionAttribute("flash", "Ошибка при проверке страницы");
            ctx.sessionAttribute("flashType", "danger");
        }
    }
}
