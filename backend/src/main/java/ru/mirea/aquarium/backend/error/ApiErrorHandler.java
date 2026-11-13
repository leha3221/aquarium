package ru.mirea.aquarium.backend.error;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class ApiErrorHandler {
    @ExceptionHandler(EntityNotFoundException.class) ResponseEntity<?> missing(RuntimeException e) { return response(HttpStatus.NOT_FOUND, e.getMessage()); }
    @ExceptionHandler(OptimisticLockException.class) ResponseEntity<?> conflict(RuntimeException e) { return response(HttpStatus.CONFLICT, e.getMessage()); }
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class}) ResponseEntity<?> invalid(Exception e) {
        String message = e instanceof MethodArgumentNotValidException m ? m.getBindingResult().getFieldErrors().stream().map(x -> x.getField()+": "+x.getDefaultMessage()).findFirst().orElse("Проверьте поля запроса") : e.getMessage();
        return response(HttpStatus.BAD_REQUEST, message);
    }
    private ResponseEntity<?> response(HttpStatus status, String message) { return ResponseEntity.status(status).body(Map.of("timestamp", Instant.now(), "status", status.value(), "error", status.getReasonPhrase(), "message", message == null ? "Ошибка запроса" : message)); }
}
