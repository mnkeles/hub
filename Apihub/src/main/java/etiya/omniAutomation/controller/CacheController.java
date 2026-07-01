package etiya.omniAutomation.controller;

import etiya.omniAutomation.service.CacheServiceImpl;
import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cache")
@RolesAllowed("ROLE_USER")
@RequiredArgsConstructor
public class CacheController {

    private final CacheServiceImpl cacheService;

    @PostMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearAllCaches() {
        Map<String, Object> response = new HashMap<>();

        try {
            cacheService.clearAllCaches();

            response.put("success", true);
            response.put("message", "Tüm cache'ler başarıyla temizlendi");
            response.put("clearedCaches", new String[]{"processFlowCache", "project"});
            response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Cache temizleme hatası: " + e.getMessage());
            response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            return ResponseEntity.internalServerError().body(response);
        }
    }
    @PostMapping("/clear/{cacheName}")
    public ResponseEntity<Map<String, Object>> clearSpecificCache(@PathVariable String cacheName) {
        Map<String, Object> response = new HashMap<>();
        try {
            cacheService.clearCache(cacheName);

            response.put("success", true);
            response.put("message", cacheName + " cache'i başarıyla temizlendi");
            response.put("clearedCache", cacheName);
            response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Cache temizleme hatası: " + e.getMessage());
            response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            return ResponseEntity.internalServerError().body(response);
        }
    }
}