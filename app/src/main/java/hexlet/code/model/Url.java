package hexlet.code.model;

import lombok.ToString;

import java.time.LocalDateTime;

@ToString
public class Url {
    private long id;
    private String name;
    private LocalDateTime createdAt;

    public Url(String name) {
        this.name = name;
        this.createdAt = LocalDateTime.now();
    }

    public Url() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
