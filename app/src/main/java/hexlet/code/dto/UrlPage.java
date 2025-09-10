package hexlet.code.dto;

import hexlet.code.model.Url;
import hexlet.code.model.UrlCheck;
import lombok.experimental.Accessors;

import java.util.List;

@Accessors(chain = true)
public class UrlPage extends BasePage {
    private Url url;
    private List<UrlCheck> checks;

    public Url getUrl() {
        return url;
    }

    public void setUrl(Url url) {
        this.url = url;
    }

    public List<UrlCheck> getChecks() {
        return checks;
    }

    public void setChecks(List<UrlCheck> checks) {
        this.checks = checks;
    }
}
