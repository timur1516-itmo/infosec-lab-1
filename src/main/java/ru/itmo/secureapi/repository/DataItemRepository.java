package ru.itmo.secureapi.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.secureapi.model.DataItem;

public interface DataItemRepository extends JpaRepository<DataItem, Long> {

    List<DataItem> findAllByOrderByCreatedAtDesc();
}
