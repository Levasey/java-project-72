package hexlet.code.controller;

import hexlet.code.model.UrlCheck;
import hexlet.code.repository.UrlCheckRepository;
import hexlet.code.repository.UrlRepository;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import kong.unirest.Unirest;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Objects;

public class UrlCheckController {
    public static void create(Context ctx) throws SQLException {
        var id = ctx.pathParamAsClass("id", Long.class).get();

        var url = UrlRepository.findById(id)
                .orElseThrow(() -> new NotFoundResponse("Url not found"));

        try {
            System.out.println("Checking URL: " + url.getName());

            var response = Unirest.get(url.getName())
                    .connectTimeout(5000)
                    .socketTimeout(5000)
                    .asString();

            int statusCode = response.getStatus();

            String title = "";
            String h1 = "";
            String description = "";

            if (statusCode >= 200 && statusCode < 400) {
                try {
                    Document doc = Jsoup.parse(response.getBody());
                    doc.title();
                    title = doc.title();
                    h1 = doc.selectFirst("h1") != null ? Objects.requireNonNull(doc.selectFirst("h1"))
                            .text() : "";
                    description = doc.selectFirst("meta[name=description]") != null
                            ? doc.selectFirst("meta[name=description]").attr("content")
                            : "";
                } catch (Exception e) {
                    System.out.println("Error parsing HTML: " + e.getMessage());
                }
            }

            createAndSaveUrlCheck(id, statusCode, title, h1, description, ctx, true);

        } catch (Exception e) {
            System.out.println("Exception during URL check: " + e.getMessage());
            // ВСЕГДА сохраняем проверку, даже при ошибках
            createAndSaveUrlCheck(id, 0, "", "", "", ctx, false);
        }

        ctx.redirect("/urls/" + id);
    }

    private static void createAndSaveUrlCheck(Long id, int statusCode, String title, String h1,
                                              String description, Context ctx, boolean success) throws SQLException {
        var urlCheck = new UrlCheck();
        urlCheck.setStatusCode(statusCode);
        urlCheck.setTitle(title);
        urlCheck.setH1(h1);
        urlCheck.setDescription(description);
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
