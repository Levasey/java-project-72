package hexlet.code.dto;

/**
 * Base class for pages with flash message support.
 * This class is designed for extension.
 */
public class BasePage {
    private String flash;
    private String flashType;

    /**
     * Returns the type of flash message.
     *
     * @return the flash message type
     */
    public String getFlashType() {
        return flashType;
    }

    /**
     * Sets the type of flash message.
     *
     * @param flashType the flash message type to set
     */
    public void setFlashType(String flashType) {
        this.flashType = flashType;
    }

    /**
     * Returns the flash message content.
     *
     * @return the flash message content
     */
    public String getFlash() {
        return flash;
    }

    /**
     * Sets the flash message content.
     *
     * @param flash the flash message content to set
     */
    public void setFlash(String flash) {
        this.flash = flash;
    }

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
