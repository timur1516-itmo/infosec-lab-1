package ru.itmo.secureapi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import ru.itmo.secureapi.dto.LoginRequest;
import ru.itmo.secureapi.dto.LoginResponse;
import ru.itmo.secureapi.model.UserAccount;
import ru.itmo.secureapi.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final PasswordService passwords;
    private final JwtService jwt;

    public LoginResponse login(LoginRequest request) {
        UserAccount user = users.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!passwords.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        return new LoginResponse(jwt.issue(user.getUsername()), "Bearer", jwt.ttlSeconds());
    }
}
