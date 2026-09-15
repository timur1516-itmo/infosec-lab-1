package ru.itmo.secureapi.config;

import ru.itmo.secureapi.security.PasswordService;
import ru.itmo.secureapi.user.UserAccount;
import ru.itmo.secureapi.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class BootstrapAdmin implements ApplicationRunner {

    private final UserRepository users;
    private final PasswordService passwords;
    private final String username;
    private final String rawPassword;

    public BootstrapAdmin(
            UserRepository users,
            PasswordService passwords,
            @Value("${app.bootstrap.username:}") String username,
            @Value("${app.bootstrap.password:}") String rawPassword) {
        this.users = users;
        this.passwords = passwords;
        this.username = username;
        this.rawPassword = rawPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (username.isBlank() || rawPassword.isBlank() || users.existsByUsername(username)) {
            return;
        }
        users.save(new UserAccount(username, passwords.hash(rawPassword)));
    }
}
