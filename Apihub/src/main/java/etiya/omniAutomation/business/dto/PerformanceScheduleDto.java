package etiya.omniAutomation.business.dto;

import etiya.omniAutomation.request.PerformanceRequest;

import java.util.Date;

public record PerformanceScheduleDto(
        Long scheduleId,
        Long projectId,
        Long processFlowId,
        String name,
        String cronExpression,
        String timezone,
        Boolean enabled,
        PerformanceRequest requestSnapshot,
        Date lastRunAt,
        Date nextRunAt,
        Long lastResultId,
        PerformanceScheduleStatus lastStatus,
        Date createdAt,
        Date updatedAt
) {
}
