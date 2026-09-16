package ru.itmo.secureapi.service;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import ru.itmo.secureapi.dto.LoginRequest;
import ru.itmo.secureapi.dto.LoginResponse;
import ru.itmo.secureapi.model.UserAccount;
import ru.itmo.secureapi.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordService passwords;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordService passwords, JwtService jwt) {
        this.users = users;
        this.passwords = passwords;
        this.jwt = jwt;
    }

    public LoginResponse login(LoginRequest request) {
        UserAccount user = users.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwords.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        return new LoginResponse(jwt.issue(user.getUsername()), "Bearer", jwt.ttlSeconds());
    }
}
