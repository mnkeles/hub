package etiya.omniAutomation.business.dto;

public record ServiceAccountRequest(
        String serviceCode,
        String name,
        String description,
        String owner
) { }
