package etiya.omniAutomation.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class PerformanceRunRegistry {

    private final ConcurrentMap<Long, PerformanceRunControl> controls = new ConcurrentHashMap<>();

    public PerformanceRunControl register(Long performanceResultId, List<CompletableFuture<?>> futures) {
        PerformanceRunControl control = new PerformanceRunControl(performanceResultId, futures);
        controls.put(performanceResultId, control);
        return control;
    }

    public Optional<PerformanceRunControl> find(Long performanceResultId) {
        return Optional.ofNullable(controls.get(performanceResultId));
    }

    public void unregister(Long performanceResultId) {
        controls.remove(performanceResultId);
    }

    public boolean requestStop(Long performanceResultId) {
        return find(performanceResultId)
                .map(control -> {
                    control.requestStop();
                    return true;
                })
                .orElse(false);
    }

    public boolean requestForceStop(Long performanceResultId) {
        return find(performanceResultId)
                .map(control -> {
                    control.requestForceStop();
                    return true;
                })
                .orElse(false);
    }

    public static final class PerformanceRunControl {
        private final Long performanceResultId;
        private final AtomicBoolean cancellationRequested = new AtomicBoolean(false);
        private final AtomicBoolean forceCancellationRequested = new AtomicBoolean(false);
        private final List<CompletableFuture<?>> futures;

        private PerformanceRunControl(Long performanceResultId, List<CompletableFuture<?>> futures) {
            this.performanceResultId = performanceResultId;
            this.futures = futures == null ? new ArrayList<>() : futures;
        }

        public Long getPerformanceResultId() {
            return performanceResultId;
        }

        public boolean isCancellationRequested() {
            return cancellationRequested.get();
        }

        public boolean isForceCancellationRequested() {
            return forceCancellationRequested.get();
        }

        public void requestStop() {
            cancellationRequested.set(true);
        }

        public void requestForceStop() {
            cancellationRequested.set(true);
            forceCancellationRequested.set(true);
            futures.stream()
                    .filter(future -> !future.isDone())
                    .forEach(future -> future.cancel(true));
        }

        public int activeFutureCount() {
            return (int) futures.stream()
                    .filter(future -> !future.isDone())
                    .count();
        }
    }
}
