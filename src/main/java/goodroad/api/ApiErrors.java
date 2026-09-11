package goodroad.api;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;

@Slf4j
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

        public ApiException(HttpStatus status, String code, String message) {
            super(message);
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
        public ResponseEntity<ApiError> handleApiException(ApiException exception) {
            if (exception.status().is5xxServerError()) {
                log.error("API exception: code={}, msg={}", exception.code(), exception.getMessage(), exception);
            }
            return ResponseEntity.status(exception.status())
                    .body(ApiError.of(exception.code(), exception.getMessage()));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
            return badRequest("REQUEST_VALIDATION_FAILED", "Request fields are invalid");
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException exception) {
            return badRequest("REQUEST_BODY_INVALID", "Request body is missing or contains invalid JSON");
        }

        @ExceptionHandler({
                MethodArgumentTypeMismatchException.class,
                MissingServletRequestParameterException.class,
                ConstraintViolationException.class,
                HandlerMethodValidationException.class
        })
        public ResponseEntity<ApiError> handleInvalidParameters(Exception exception) {
            return badRequest("REQUEST_VALIDATION_FAILED", "Request parameters are invalid");
        }

        @ExceptionHandler(MissingServletRequestPartException.class)
        public ResponseEntity<ApiError> handleMissingPart(MissingServletRequestPartException exception) {
            return badRequest("REQUEST_PART_MISSING", "Required request part is missing");
        }

        @ExceptionHandler(MaxUploadSizeExceededException.class)
        public ResponseEntity<ApiError> handleUploadTooLarge(MaxUploadSizeExceededException exception) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(ApiError.of("FILE_TOO_LARGE", "Uploaded file is too large"));
        }

        @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
        public ResponseEntity<ApiError> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException exception) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .body(ApiError.of("CONTENT_TYPE_UNSUPPORTED", "Content type is not supported"));
        }

        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<ApiError> handleConflict(DataIntegrityViolationException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiError.of("DATA_CONFLICT", "Operation conflicts with current data"));
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiError.of("ACCESS_DENIED", "Access denied"));
        }

        @ExceptionHandler(NoResourceFoundException.class)
        public ResponseEntity<ApiError> handleNotFound(NoResourceFoundException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiError.of("ENDPOINT_NOT_FOUND", "Endpoint not found"));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiError> handleServerError(Exception exception) {
            log.error("Unexpected exception", exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiError.of("SERVER_INTERNAL_ERROR", "Server internal error"));
        }

        private ResponseEntity<ApiError> badRequest(String code, String message) {
            return ResponseEntity.badRequest().body(ApiError.of(code, message));
        }
    }
}
