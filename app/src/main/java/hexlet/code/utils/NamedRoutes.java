package hexlet.code.utils;

public class NamedRoutes {
    // Базовый путь для URL операций
    public static String urlsPath() {
        return "/urls";
    }

    // Путь для конкретного URL по ID
    public static String urlPath(String id) {
        return "/urls/" + id;
    }

    // Путь для проверки URL по ID
    public static String urlPathCheck(String id) {
        return "/urls/" + id + "/checks";
    }
}
