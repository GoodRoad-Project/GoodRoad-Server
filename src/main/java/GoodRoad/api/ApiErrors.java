package GoodRoad.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

public final class ApiErrors {

    private ApiErrors() {
    }

    public record ApiError(String code, String msg, Instant ts) {
        public static ApiError of(String code, String msg) {
            return new ApiError(code, msg, Instant.now());
        }
    }

    public static class ApiException extends RuntimeException {
        private final HttpStatus status;
        private final String code;

        public ApiException(HttpStatus status, String code, String msg) {
            super(msg);
            this.status = status;
            this.code = code;
        }

        public HttpStatus status() {
            return status;
        }

        public String code() {
            return code;
        }
    }

    @RestControllerAdvice
    public static class GlobalHandler {

        @ExceptionHandler(ApiException.class)
        public ResponseEntity<ApiError> onApi(ApiException e) {
            return ResponseEntity.status(e.status()).body(ApiError.of(e.code(), e.getMessage()));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiError> onAny() {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiError.of("SERVER_INTERNAL_ERROR", "Server internal error"));
        }
    }
}