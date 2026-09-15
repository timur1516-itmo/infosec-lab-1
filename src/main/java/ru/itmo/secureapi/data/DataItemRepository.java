package ru.itmo.secureapi.data;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataItemRepository extends JpaRepository<DataItem, Long> {

    List<DataItem> findAllByOrderByCreatedAtDesc();
}
