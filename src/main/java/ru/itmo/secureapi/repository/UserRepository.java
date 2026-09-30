package ru.itmo.secureapi.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.secureapi.model.UserAccount;

public interface UserRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsername(String username);

    boolean existsByUsername(String username);
}
