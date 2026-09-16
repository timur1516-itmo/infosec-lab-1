package ru.itmo.secureapi.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {

    private static final int BCRYPT_COST = 12;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(BCRYPT_COST);

    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String passwordHash) {
        return encoder.matches(rawPassword, passwordHash);
    }
}
