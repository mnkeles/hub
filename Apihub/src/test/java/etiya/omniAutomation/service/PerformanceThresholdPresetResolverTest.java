package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.PerformanceThresholdConfig;
import etiya.omniAutomation.business.dto.PerformanceThresholdPreset;
import etiya.omniAutomation.request.PerformanceRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PerformanceThresholdPresetResolverTest {

    private final PerformanceThresholdPresetResolver resolver = new PerformanceThresholdPresetResolver();

    @Test
    void normalPresetReturnsDefaultNormalConfig() {
        PerformanceRequest request = new PerformanceRequest();
        request.setThresholdPreset(PerformanceThresholdPreset.NORMAL);

        PerformanceThresholdConfig config = resolver.resolve(request);

        assertEquals(1, config.maxErrorRatePercent());
        assertEquals(1000, config.maxAverageMs());
        assertEquals(3000, config.maxP95Ms());
        assertEquals(5000, config.maxP99Ms());
        assertEquals(20, config.minThroughputPerSecond());
    }

    @Test
    void strictPresetWithAverageOverrideKeepsOtherStrictValues() {
        PerformanceRequest request = new PerformanceRequest();
        request.setThresholdPreset(PerformanceThresholdPreset.STRICT_SLA);
        request.setMaxAverageMs(750.0);

        PerformanceThresholdConfig config = resolver.resolve(request);

        assertEquals(0.1, config.maxErrorRatePercent());
        assertEquals(750, config.maxAverageMs());
        assertEquals(1000, config.maxP95Ms());
        assertEquals(2000, config.maxP99Ms());
        assertEquals(50, config.minThroughputPerSecond());
    }

    @Test
    void customPresetRequiresAllValues() {
        PerformanceRequest request = new PerformanceRequest();
        request.setThresholdPreset(PerformanceThresholdPreset.CUSTOM);
        request.setMaxAverageMs(500.0);

        assertThrows(ResponseStatusException.class, () -> resolver.resolve(request));
    }

    @Test
    void negativeThresholdValueIsRejected() {
        PerformanceRequest request = customRequest();
        request.setMaxErrorRatePercent(-1.0);

        assertThrows(ResponseStatusException.class, () -> resolver.resolve(request));
    }

    @Test
    void zeroResponseTimeThresholdIsRejected() {
        PerformanceRequest request = customRequest();
        request.setMaxP95Ms(0.0);

        assertThrows(ResponseStatusException.class, () -> resolver.resolve(request));
    }

    @Test
    void zeroMinThroughputIsAccepted() {
        PerformanceRequest request = customRequest();
        request.setMinThroughputPerSecond(0.0);

        PerformanceThresholdConfig config = resolver.resolve(request);

        assertEquals(0, config.minThroughputPerSecond());
    }

    private PerformanceRequest customRequest() {
        PerformanceRequest request = new PerformanceRequest();
        request.setThresholdPreset(PerformanceThresholdPreset.CUSTOM);
        request.setMaxErrorRatePercent(1.0);
        request.setMaxAverageMs(1000.0);
        request.setMaxP95Ms(3000.0);
        request.setMaxP99Ms(5000.0);
        request.setMinThroughputPerSecond(20.0);
        return request;
    }
}
