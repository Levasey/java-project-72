package hexlet.code.dto;

import hexlet.code.model.Url;
import hexlet.code.model.UrlCheck;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Accessors(chain = true)
public class UrlsPage extends BasePage {
    private List<Url> urls;
    private Map<Long, UrlCheck> latestChecks = new HashMap<>();

    public List<Url> getUrls() {
        return urls;
    }

    public void setUrls(List<Url> urls) {
        this.urls = urls;
    }

    public Map<Long, UrlCheck> getLatestChecks() {
        return latestChecks;
    }

    public void setLatestChecks(Map<Long, UrlCheck> latestChecks) {
        this.latestChecks = latestChecks;
    }
}
