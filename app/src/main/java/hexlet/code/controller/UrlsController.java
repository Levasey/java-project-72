package hexlet.code.controller;

import hexlet.code.dto.UrlPage;
import hexlet.code.dto.UrlsPage;
import hexlet.code.model.Url;
import hexlet.code.model.UrlCheck;
import hexlet.code.repository.UrlCheckRepository;
import hexlet.code.repository.UrlRepository;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;

import java.net.URI;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;

import static io.javalin.rendering.template.TemplateUtil.model;

public class UrlsController {
    public static void index(Context ctx) throws SQLException {
        List<Url> urls = UrlRepository.findAll();
        UrlsPage page = new UrlsPage();
        page.setUrls(urls);

        // Загружаем последние проверки для каждого URL
        for (var url : urls) {
            var latestCheck = UrlCheckRepository.findLatestCheck(url.getId());
            latestCheck.ifPresent(check -> page.getLatestChecks().put(url.getId(), check));
        }

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

        ctx.render("urls/index.jte", model("page", page));
    }

    public static void create(Context ctx) throws SQLException {
        String inputUrl = ctx.formParam("url");
        if (inputUrl == null || inputUrl.trim().isEmpty()) {
            ctx.sessionAttribute("flash", "URL не может быть пустым");
            ctx.sessionAttribute("flashType", "danger");
            ctx.redirect("/");
            return;
        }

        try {
            // Парсим URL и нормализуем
            URI uri = new URI(inputUrl.trim());
            URL url = uri.toURL();
            String normalizedUrl = url.getProtocol() + "://" + url.getHost();

            // Добавляем порт, если он указан и не стандартный
            if (url.getPort() != -1 &&
                    !((url.getProtocol().equals("http") && url.getPort() == 80) ||
                            (url.getProtocol().equals("https") && url.getPort() == 443))) {
                normalizedUrl += ":" + url.getPort();
            }

            // Проверяем существование URL
            if (UrlRepository.existsByName(normalizedUrl)) {
                ctx.sessionAttribute("flash", "Страница уже существует");
                ctx.sessionAttribute("flashType", "info");
                ctx.redirect("/urls");
                return;
            }

            // Сохраняем новый URL
            Url newUrl = new Url(normalizedUrl);
            UrlRepository.save(newUrl);

            ctx.sessionAttribute("flash", "Страница успешно добавлена");
            ctx.sessionAttribute("flashType", "success");
            ctx.redirect("/urls");

        } catch (Exception e) {
            ctx.sessionAttribute("flash", "Некорректный URL");
            ctx.sessionAttribute("flashType", "danger");
            ctx.redirect("/");
        }
    }

    public static void show(Context ctx) throws SQLException {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        var url = UrlRepository.findById(id)
                .orElseThrow(() -> new NotFoundResponse("Entity with id = " + id + " not found"));
        List<UrlCheck> checks = UrlCheckRepository.findByUrlId(id);

        var page = new UrlPage();
        page.setUrl(url);
        page.setChecks(checks);

        // Добавляем flash-сообщения
        String flash = ctx.sessionAttribute("flash");
        String flashType = ctx.sessionAttribute("flashType");

        if (flash != null) {
            page.setFlash(flash);
            page.setFlashType(flashType);
            ctx.sessionAttribute("flash", null);
            ctx.sessionAttribute("flashType", null);
        }

        ctx.render("urls/show.jte", model("page", page));
    }
}
