package ru.itmo.secureapi.data;

import java.time.Instant;

public record DataResponse(
        Long id,
        String title,
        String content,
        String owner,
        Instant createdAt) {

    static DataResponse from(DataItem item) {
        return new DataResponse(
                item.getId(),
                item.getTitle(),
                item.getContent(),
                item.getOwnerUsername(),
                item.getCreatedAt());
    }
}
