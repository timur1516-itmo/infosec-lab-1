package ru.itmo.secureapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "data_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DataItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(name = "owner_username", nullable = false, length = 64)
    private String ownerUsername;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public DataItem(String title, String content, String ownerUsername, Instant createdAt) {
        this.title = title;
        this.content = content;
        this.ownerUsername = ownerUsername;
        this.createdAt = createdAt;
    }
}
