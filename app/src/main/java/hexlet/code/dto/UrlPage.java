package hexlet.code.dto;

import hexlet.code.model.Url;
import hexlet.code.model.UrlCheck;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Accessors(chain = true)
@Getter
@Setter
public final class UrlPage extends BasePage {
    private Url url;
    private List<UrlCheck> checks;
}
