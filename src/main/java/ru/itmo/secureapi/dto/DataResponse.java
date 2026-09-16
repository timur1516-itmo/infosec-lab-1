package ru.itmo.secureapi.dto;

import java.time.Instant;
import ru.itmo.secureapi.model.DataItem;

public record DataResponse(
        Long id,
        String title,
        String content,
        String owner,
        Instant createdAt) {

    public static DataResponse from(DataItem item) {
        return new DataResponse(
                item.getId(),
                item.getTitle(),
                item.getContent(),
                item.getOwnerUsername(),
                item.getCreatedAt());
    }
}
