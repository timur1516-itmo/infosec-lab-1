package ru.itmo.secureapi.middleware;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<ProblemDetail> badCredentials(BadCredentialsException exception) {
        return error(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ProblemDetail> authenticationRequired() {
        return error(HttpStatus.UNAUTHORIZED, "Bearer token is required");
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, IllegalArgumentException.class})
    ResponseEntity<ProblemDetail> badRequest() {
        return error(HttpStatus.BAD_REQUEST, "Request validation failed");
    }

    private ResponseEntity<ProblemDetail> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, message));
    }
}
