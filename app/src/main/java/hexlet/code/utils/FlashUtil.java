package hexlet.code.utils;

import hexlet.code.dto.BasePage;
import io.javalin.http.Context;

public class FlashUtil {

    public static void setFlashToPage(Context ctx, BasePage page) {
        String flash = ctx.sessionAttribute("flash");
        String flashType = ctx.sessionAttribute("flashType");

        if (flash != null) {
            page.setFlash(flash);
            page.setFlashType(flashType);
            clearFlash(ctx);
        }
    }

    public static void setFlash(Context ctx, String message, String type) {
        ctx.sessionAttribute("flash", message);
        ctx.sessionAttribute("flashType", type);
    }

    public static void clearFlash(Context ctx) {
        ctx.sessionAttribute("flash", null);
        ctx.sessionAttribute("flashType", null);
    }
}
