package hexlet.code.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Base class for pages with flash message support.
 * This class is designed for extension.
 */
@Setter
@Getter
public class BasePage {
    private String flash;
    private String flashType;

    /**
     * Checks if the page has a flash message.
     * Subclasses should call this method to check for flash messages
     * and can override to add additional conditions.
     *
     * @return true if flash message exists and is not empty
     */
    public boolean hasFlash() {
        return flash != null && !flash.isEmpty();
    }
}
