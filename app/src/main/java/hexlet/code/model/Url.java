package hexlet.code.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@ToString
@Setter
@Getter
public final class Url {
    private long id;
    private String name;
    private LocalDateTime createdAt;

    public Url(String name) {
        this.name = name;
        this.createdAt = LocalDateTime.now();
    }

    public Url() {
    }

    public String getFormattedCreatedAt() {
        if (createdAt == null) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return createdAt.format(formatter);
    }

}
