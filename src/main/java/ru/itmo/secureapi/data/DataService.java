package ru.itmo.secureapi.data;

import java.time.Instant;
import java.util.List;
import ru.itmo.secureapi.security.PlainTextSanitizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
