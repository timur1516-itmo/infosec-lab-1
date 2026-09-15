package ru.itmo.secureapi.auth;

public record LoginResponse(String token, String tokenType, long expiresInSeconds) {
}
