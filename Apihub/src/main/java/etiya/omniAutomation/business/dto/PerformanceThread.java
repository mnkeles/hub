package etiya.omniAutomation.business.dto;

import java.util.List;

public record PerformanceThread(int threadNumber, List<PerformanceResultItemDto> steps) {
}
