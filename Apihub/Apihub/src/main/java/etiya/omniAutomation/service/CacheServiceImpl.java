package etiya.omniAutomation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CacheServiceImpl {

    private final CacheManager cacheManager;

    public void clearAllCaches() {
        cacheManager.getCacheNames().forEach(this::clearCache);
    }

    public void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            System.out.println(cacheName + " cache'i temizlendi");
        }
    }
}