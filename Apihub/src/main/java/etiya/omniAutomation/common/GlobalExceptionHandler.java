package etiya.omniAutomation.common;

import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;


@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        log.warn("Multipart upload rejected because the configured size limit was exceeded.", ex);
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of(
                "message", "File too large. Maximum supported upload size is 100MB.",
                "error", "MAX_UPLOAD_SIZE_EXCEEDED",
                "status", HttpStatus.PAYLOAD_TOO_LARGE.value()
        ));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(AuthenticationException ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = "Username or password hatalı";
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "message", message,
                "error", "AUTHENTICATION_FAILED",
                "status", HttpStatus.UNAUTHORIZED.value()
        ));
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<Map<String, Object>> handleJwtException(JwtException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "message", "Invalid refresh token",
                "error", "INVALID_TOKEN",
                "status", HttpStatus.UNAUTHORIZED.value()
        ));
    }

}
