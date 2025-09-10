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

public class UrlCheckController {
    public static void create(Context ctx) throws SQLException {
        var id = ctx.pathParamAsClass("id", Long.class).get();

        // Находим URL по ID
        var url = UrlRepository.findById(id)
                .orElseThrow(() -> new NotFoundResponse("Url not found"));

        try {
            // Выполняем HTTP-запрос
            var response = Unirest.get(url.getName()).asString();

            // Парсим HTML
            Document doc = Jsoup.parse(response.getBody());

            // Извлекаем данные
            String title = doc.title();
            String h1 = doc.selectFirst("h1") != null ? doc.selectFirst("h1").text() : "";
            String description = doc.selectFirst("meta[name=description]") != null
                    ? doc.selectFirst("meta[name=description]").attr("content")
                    : "";

            // Создаем проверку
            var urlCheck = new UrlCheck();
            urlCheck.setStatusCode(response.getStatus());
            urlCheck.setTitle(title);
            urlCheck.setH1(h1);
            urlCheck.setDescription(description);
            urlCheck.setUrlId(id);
            urlCheck.setCreatedAt(LocalDateTime.now());

            // Сохраняем проверку
            UrlCheckRepository.save(urlCheck);

            // Устанавливаем flash-сообщение
            ctx.sessionAttribute("flash", "Страница успешно проверена");
            ctx.sessionAttribute("flashType", "success");

        } catch (UnirestException e) {
            // Устанавливаем сообщение об ошибке
            ctx.sessionAttribute("flash", "Невозможно проверить страницу: " + e.getMessage());
            ctx.sessionAttribute("flashType", "danger");
        }

        ctx.redirect("/urls/" + id);
    }
}
