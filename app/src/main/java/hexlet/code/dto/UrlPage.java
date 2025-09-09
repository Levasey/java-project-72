package hexlet.code.dto;

import hexlet.code.model.Url;

public class UrlPage extends BasePage {
    private Url url;

    public UrlPage(Url url) {
        this.url = url;
    }

    public Url getUrl() {
        return url;
    }
}
