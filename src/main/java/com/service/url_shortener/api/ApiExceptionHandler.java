package com.service.url_shortener.api;

import com.service.url_shortener.exception.AliasConflictException;
import com.service.url_shortener.exception.InvalidAliasException;
import com.service.url_shortener.exception.InvalidUrlException;
import com.service.url_shortener.exception.LinkNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidUrlException.class)
    ProblemDetail handleInvalidUrl(InvalidUrlException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid URL", exception.getMessage(), "INVALID_URL");
    }

    @ExceptionHandler(InvalidAliasException.class)
    ProblemDetail handleInvalidAlias(InvalidAliasException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid custom alias", exception.getMessage(), "INVALID_ALIAS");
    }

    @ExceptionHandler(AliasConflictException.class)
    ProblemDetail handleAliasConflict(AliasConflictException exception) {
        return problem(HttpStatus.CONFLICT, "Custom alias conflict", exception.getMessage(), "ALIAS_CONFLICT");
    }

    @ExceptionHandler(LinkNotFoundException.class)
    ProblemDetail handleNotFound(LinkNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Short link not found", exception.getMessage(), "LINK_NOT_FOUND");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        ProblemDetail detail = problem(
                HttpStatus.BAD_REQUEST,
                "Invalid request",
                "One or more request fields are invalid",
                "INVALID_REQUEST"
        );
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage())
        );
        detail.setProperty("errors", errors);
        return detail;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadableBody() {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Malformed request",
                "Request body is missing or contains malformed JSON",
                "MALFORMED_JSON"
        );
    }

    private ProblemDetail problem(HttpStatus status, String title, String message, String errorCode) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setTitle(title);
        detail.setProperty("errorCode", errorCode);
        return detail;
    }
}
