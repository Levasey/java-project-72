package hexlet.code.dto;

import hexlet.code.model.Url;
import hexlet.code.model.UrlCheck;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Accessors(chain = true)
public final class UrlsPage extends BasePage {
    private List<Url> urls;
    private Map<Long, UrlCheck> latestChecks = new HashMap<>();
}
