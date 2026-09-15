package ru.itmo.secureapi.auth;

import ru.itmo.secureapi.security.JwtService;
import ru.itmo.secureapi.security.PasswordService;
import ru.itmo.secureapi.user.UserAccount;
import ru.itmo.secureapi.user.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

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
