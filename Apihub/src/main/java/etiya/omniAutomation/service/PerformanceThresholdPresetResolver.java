package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.PerformanceThresholdConfig;
import etiya.omniAutomation.business.dto.PerformanceThresholdPreset;
import etiya.omniAutomation.request.PerformanceRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumMap;
import java.util.Map;

@Service
public class PerformanceThresholdPresetResolver {

    private static final Map<PerformanceThresholdPreset, PerformanceThresholdConfig> PRESETS = new EnumMap<>(PerformanceThresholdPreset.class);

    static {
        PRESETS.put(PerformanceThresholdPreset.SMOKE, new PerformanceThresholdConfig(5, 3000, 8000, 12000, 1));
        PRESETS.put(PerformanceThresholdPreset.NORMAL, new PerformanceThresholdConfig(1, 1000, 3000, 5000, 20));
        PRESETS.put(PerformanceThresholdPreset.STRESS, new PerformanceThresholdConfig(2, 1500, 5000, 8000, 50));
        PRESETS.put(PerformanceThresholdPreset.STRICT_SLA, new PerformanceThresholdConfig(0.1, 500, 1000, 2000, 50));
    }

    public PerformanceThresholdConfig resolve(PerformanceRequest request) {
        PerformanceThresholdPreset preset = request.getThresholdPreset() == null
                ? PerformanceThresholdPreset.NORMAL
                : request.getThresholdPreset();

        if (preset == PerformanceThresholdPreset.CUSTOM) {
            return customConfig(request);
        }

        PerformanceThresholdConfig base = PRESETS.getOrDefault(preset, PRESETS.get(PerformanceThresholdPreset.NORMAL));
        PerformanceThresholdConfig resolved = new PerformanceThresholdConfig(
                valueOrDefault(request.getMaxErrorRatePercent(), base.maxErrorRatePercent()),
                valueOrDefault(request.getMaxAverageMs(), base.maxAverageMs()),
                valueOrDefault(request.getMaxP95Ms(), base.maxP95Ms()),
                valueOrDefault(request.getMaxP99Ms(), base.maxP99Ms()),
                valueOrDefault(request.getMinThroughputPerSecond(), base.minThroughputPerSecond())
        );
        validate(resolved);
        return resolved;
    }

    private PerformanceThresholdConfig customConfig(PerformanceRequest request) {
        if (request.getMaxErrorRatePercent() == null
                || request.getMaxAverageMs() == null
                || request.getMaxP95Ms() == null
                || request.getMaxP99Ms() == null
                || request.getMinThroughputPerSecond() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CUSTOM threshold preset requires all threshold values.");
        }
        PerformanceThresholdConfig config = new PerformanceThresholdConfig(
                request.getMaxErrorRatePercent(),
                request.getMaxAverageMs(),
                request.getMaxP95Ms(),
                request.getMaxP99Ms(),
                request.getMinThroughputPerSecond()
        );
        validate(config);
        return config;
    }

    private double valueOrDefault(Double value, double defaultValue) {
        return value == null ? defaultValue : value;
    }

    private void validate(PerformanceThresholdConfig config) {
        if (config.maxErrorRatePercent() < 0
                || config.maxAverageMs() < 0
                || config.maxP95Ms() < 0
                || config.maxP99Ms() < 0
                || config.minThroughputPerSecond() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Threshold values cannot be negative.");
        }
        if (config.maxAverageMs() == 0 || config.maxP95Ms() == 0 || config.maxP99Ms() == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Response time thresholds must be greater than zero.");
        }
    }
}
