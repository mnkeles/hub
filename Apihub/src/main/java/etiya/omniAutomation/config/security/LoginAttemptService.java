package etiya.omniAutomation.config.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private static final Logger logger = LoggerFactory.getLogger(LoginAttemptService.class);

    @Value("${security.login.max-attempts:5}")
    private int maxAttempts;

    @Value("${security.login.lockout-duration-minutes:15}")
    private long lockoutDurationMinutes;

    @Value("${security.login.attempt-window-minutes:5}")
    private long attemptWindowMinutes;

    private record AttemptRecord(int count, Instant windowStart, Instant lockedUntil) {}

    private final Map<String, AttemptRecord> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String username) {
        AttemptRecord record = attempts.get(username);
        if (record == null) return false;

        if (record.lockedUntil() != null && Instant.now().isBefore(record.lockedUntil())) {
            return true;
        }

        if (record.lockedUntil() != null && Instant.now().isAfter(record.lockedUntil())) {
            attempts.remove(username);
        }

        return false;
    }

    public void loginSucceeded(String username) {
        attempts.remove(username);
    }

    public void loginFailed(String username) {
        Instant now = Instant.now();
        AttemptRecord existing = attempts.get(username);

        if (existing == null || now.isAfter(existing.windowStart().plusSeconds(attemptWindowMinutes * 60))) {
            attempts.put(username, new AttemptRecord(1, now, null));
            return;
        }

        int newCount = existing.count() + 1;
        if (newCount >= maxAttempts) {
            Instant lockedUntil = now.plusSeconds(lockoutDurationMinutes * 60);
            attempts.put(username, new AttemptRecord(newCount, existing.windowStart(), lockedUntil));
            logger.warn("Account locked due to too many failed attempts: {} (locked until {})", username, lockedUntil);
        } else {
            attempts.put(username, new AttemptRecord(newCount, existing.windowStart(), null));
        }
    }
}
