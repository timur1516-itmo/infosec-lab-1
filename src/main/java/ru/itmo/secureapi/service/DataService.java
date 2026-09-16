package ru.itmo.secureapi.service;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.secureapi.dto.CreateDataRequest;
import ru.itmo.secureapi.dto.DataResponse;
import ru.itmo.secureapi.model.DataItem;
import ru.itmo.secureapi.repository.DataItemRepository;
import ru.itmo.secureapi.utils.PlainTextSanitizer;

@Service
public class DataService {

    private final DataItemRepository items;
    private final PlainTextSanitizer sanitizer;

    public DataService(DataItemRepository items, PlainTextSanitizer sanitizer) {
        this.items = items;
        this.sanitizer = sanitizer;
    }

    @Transactional(readOnly = true)
    public List<DataResponse> findAll() {
        return items.findAllByOrderByCreatedAtDesc().stream()
                .map(DataResponse::from)
                .toList();
    }

    @Transactional
    public DataResponse create(CreateDataRequest request, String username) {
        String safeTitle = sanitizer.sanitize(request.title());
        String safeContent = sanitizer.sanitize(request.content());
        if (safeTitle.isBlank() || safeContent.isBlank()) {
            throw new IllegalArgumentException("Data must contain visible plain text");
        }
        DataItem saved = items.save(new DataItem(
                safeTitle,
                safeContent,
                username,
                Instant.now()));
        return DataResponse.from(saved);
    }
}
