package hexlet.code.controller;

import hexlet.code.dto.BasePage;
import hexlet.code.utils.FlashUtil;
import io.javalin.http.Context;

import static io.javalin.rendering.template.TemplateUtil.model;

public class RootController {

    public static void index(Context ctx) {
        var page = new BasePage();
        FlashUtil.setFlashToPage(ctx, page);
        ctx.render("index.jte", model("page", page));
    }
}
