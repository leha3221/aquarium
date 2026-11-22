package ru.mirea.aquarium.backend.team.Skvorcsov.controller;

import jakarta.persistence.*;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.*;
import org.springframework.web.bind.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import ru.mirea.aquarium.backend.team.Skvorcsov.service.FishFailure;
import java.time.Instant;
import java.util.*;

@RestControllerAdvice(basePackages = "ru.mirea.aquarium.backend.team.Skvorcsov.controller")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FishErrorHandler {
    public record ErrorBody(Instant timestamp, int status, String error, String message) {}

    @ExceptionHandler(FishFailure.class)
    ResponseEntity<ErrorBody> business(FishFailure e) { return error(e.status(), e.getMessage()); }

    @ExceptionHandler(EntityNotFoundException.class)
    ResponseEntity<ErrorBody> missing(EntityNotFoundException e) { return error(404, e.getMessage()); }

    @ExceptionHandler({OptimisticLockException.class, OptimisticLockingFailureException.class})
    ResponseEntity<ErrorBody> stale(Exception e) { return error(409, "Запись уже изменена другим пользователем"); }

    @ExceptionHandler({DataIntegrityViolationException.class, PessimisticLockingFailureException.class, PessimisticLockException.class})
    ResponseEntity<ErrorBody> conflict(Exception e) { return error(409, "Конфликт ограничений или одновременного изменения данных"); }

    @ExceptionHandler({IllegalArgumentException.class, BindException.class, ConstraintViolationException.class,
        HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class, org.springframework.web.multipart.support.MissingServletRequestPartException.class,
        HandlerMethodValidationException.class})
    ResponseEntity<ErrorBody> invalid(Exception e) {
        if (e instanceof BindException b) return error(400, b.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage()).sorted().findFirst().orElse("Проверьте поля запроса"));
        if (e instanceof IllegalArgumentException && !(e instanceof MethodArgumentTypeMismatchException))
            return error(400, e.getMessage());
        return error(400, "Неверные поля, формат JSON или параметры запроса");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ErrorBody> tooLarge(Exception e) { return error(400, "Превышен допустимый размер файла"); }

    @ExceptionHandler({HttpRequestMethodNotSupportedException.class, HttpMediaTypeNotSupportedException.class,
        HttpMediaTypeNotAcceptableException.class})
    ResponseEntity<ErrorBody> protocol(Exception e) {
        return error(((org.springframework.web.ErrorResponse) e).getStatusCode().value(), "Неподдерживаемый метод или формат запроса");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorBody> unexpected(Exception e) {
        org.slf4j.LoggerFactory.getLogger(FishErrorHandler.class).error("Fish API failed", e);
        return error(500, "Внутренняя ошибка сервера");
    }

    private ResponseEntity<ErrorBody> error(int code, String message) {
        HttpStatus status = HttpStatus.valueOf(code);
        return ResponseEntity.status(status).body(new ErrorBody(Instant.now(), code, status.getReasonPhrase(), message));
    }
}
