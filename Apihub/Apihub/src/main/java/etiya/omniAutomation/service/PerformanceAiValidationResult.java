package etiya.omniAutomation.service;

import java.util.List;

public record PerformanceAiValidationResult(boolean valid, List<String> errors) {
    public static PerformanceAiValidationResult valid() {
        return new PerformanceAiValidationResult(true, List.of());
    }

    public static PerformanceAiValidationResult invalid(List<String> errors) {
        return new PerformanceAiValidationResult(false, errors == null ? List.of() : errors);
    }
}
