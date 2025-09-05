package hexlet.code.dto;

public class BasePage {
    private String flash;
    private String flashType;

    public String getFlashType() {
        return flashType;
    }

    public void setFlashType(String flashType) {
        this.flashType = flashType;
    }

    public String getFlash() {
        return flash;
    }

    public void setFlash(String flash) {
        this.flash = flash;
    }

    public boolean hasFlash() {
        return flash != null && !flash.isEmpty();
    }
}
