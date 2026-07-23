package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.PerformanceResultDto;
import etiya.omniAutomation.business.dto.PerformanceScheduleDto;
import etiya.omniAutomation.business.dto.PerformanceScheduleStatus;
import etiya.omniAutomation.business.dto.PerformanceThresholdPreset;
import etiya.omniAutomation.common.GeneralEnums;
import etiya.omniAutomation.entity.PerformanceScheduleEntity;
import etiya.omniAutomation.repository.PerformanceResultRepository;
import etiya.omniAutomation.repository.PerformanceScheduleRepository;
import etiya.omniAutomation.request.PerformanceRequest;
import etiya.omniAutomation.request.PerformanceScheduleRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PerformanceScheduleService {

    private final PerformanceScheduleRepository scheduleRepository;
    private final PerformanceResultRepository performanceResultRepository;
    private final PerformanceService performanceService;

    @Transactional(readOnly = true)
    public List<PerformanceScheduleDto> list(Long projectId, Long processFlowId) {
        if (projectId == null || processFlowId == null) {
            throw badRequest("projectId and processFlowId are required.");
        }
        return scheduleRepository.findByProjectIdAndProcessFlowIdOrderByCreatedAtDesc(projectId, processFlowId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public PerformanceScheduleDto create(PerformanceScheduleRequest request) {
        validateRequest(request);
        Date now = new Date();
        PerformanceScheduleEntity entity = new PerformanceScheduleEntity();
        applyRequest(entity, request);
        entity.setEnabled(request.getEnabled() == null || request.getEnabled());
        entity.setLastStatus(PerformanceScheduleStatus.NEVER_RUN);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setNextRunAt(Boolean.TRUE.equals(entity.getEnabled()) ? nextRun(entity.getCronExpression(), entity.getTimezone(), now) : null);
        return toDto(scheduleRepository.save(entity));
    }

    @Transactional
    public PerformanceScheduleDto update(Long scheduleId, PerformanceScheduleRequest request) {
        validateRequest(request);
        PerformanceScheduleEntity entity = find(scheduleId);
        applyRequest(entity, request);
        entity.setEnabled(request.getEnabled() == null ? entity.getEnabled() : request.getEnabled());
        entity.setUpdatedAt(new Date());
        entity.setNextRunAt(Boolean.TRUE.equals(entity.getEnabled()) ? nextRun(entity.getCronExpression(), entity.getTimezone(), new Date()) : null);
        return toDto(scheduleRepository.save(entity));
    }

    @Transactional
    public PerformanceScheduleDto setEnabled(Long scheduleId, boolean enabled) {
        PerformanceScheduleEntity entity = find(scheduleId);
        entity.setEnabled(enabled);
        entity.setLastStatus(enabled ? entity.getLastStatus() : PerformanceScheduleStatus.DISABLED);
        entity.setNextRunAt(enabled ? nextRun(entity.getCronExpression(), entity.getTimezone(), new Date()) : null);
        entity.setUpdatedAt(new Date());
        return toDto(scheduleRepository.save(entity));
    }

    @Transactional
    public void deactivate(Long scheduleId) {
        PerformanceScheduleEntity entity = find(scheduleId);
        entity.setEnabled(false);
        entity.setLastStatus(PerformanceScheduleStatus.DISABLED);
        entity.setNextRunAt(null);
        entity.setUpdatedAt(new Date());
        scheduleRepository.save(entity);
    }

    @Transactional
    public PerformanceResultDto runNow(Long scheduleId) {
        PerformanceScheduleEntity entity = find(scheduleId);
        if (!Boolean.TRUE.equals(entity.getEnabled())) {
            throw badRequest("Schedule is disabled.");
        }
        return startSchedule(entity, new Date());
    }

    @Transactional
    public void runDueSchedules(Date now) {
        Date effectiveNow = now == null ? new Date() : now;
        List<PerformanceScheduleEntity> dueSchedules = scheduleRepository.findByEnabledTrueAndNextRunAtLessThanEqual(effectiveNow);
        for (PerformanceScheduleEntity schedule : dueSchedules) {
            try {
                if (hasActivePreviousRun(schedule)) {
                    schedule.setLastStatus(PerformanceScheduleStatus.SKIPPED_RUNNING);
                    schedule.setNextRunAt(nextRun(schedule.getCronExpression(), schedule.getTimezone(), effectiveNow));
                    schedule.setUpdatedAt(effectiveNow);
                    scheduleRepository.save(schedule);
                    continue;
                }
                startSchedule(schedule, effectiveNow);
            } catch (Exception e) {
                schedule.setLastStatus(PerformanceScheduleStatus.FAILED_TO_START);
                schedule.setNextRunAt(nextRun(schedule.getCronExpression(), schedule.getTimezone(), effectiveNow));
                schedule.setUpdatedAt(effectiveNow);
                scheduleRepository.save(schedule);
            }
        }
    }

    public Date nextRun(String cronExpression, String timezone, Date after) {
        try {
            ZoneId zoneId = ZoneId.of(timezone);
            CronExpression cron = CronExpression.parse(cronExpression);
            ZonedDateTime base = ZonedDateTime.ofInstant((after == null ? new Date() : after).toInstant(), zoneId);
            ZonedDateTime next = cron.next(base);
            if (next == null) {
                throw badRequest("Cron expression does not produce a next run.");
            }
            return Date.from(next.toInstant());
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw badRequest("Invalid cron expression or timezone: " + e.getMessage());
        }
    }

    private PerformanceResultDto startSchedule(PerformanceScheduleEntity entity, Date now) {
        PerformanceResultDto result = performanceService.executePerformanceTest(entity.getRequestSnapshot());
        entity.setLastRunAt(now);
        entity.setLastResultId(result.getPerformanceResultId());
        entity.setLastStatus(PerformanceScheduleStatus.STARTED);
        entity.setNextRunAt(nextRun(entity.getCronExpression(), entity.getTimezone(), now));
        entity.setUpdatedAt(now);
        scheduleRepository.save(entity);
        return result;
    }

    private boolean hasActivePreviousRun(PerformanceScheduleEntity schedule) {
        return schedule.getLastResultId() != null
                && performanceResultRepository.existsByPerfRsltIdAndPerfStatusIn(
                schedule.getLastResultId(),
                List.of(GeneralEnums.PerformanceStatus.RUNNING, GeneralEnums.PerformanceStatus.STOPPING)
        );
    }

    private void validateRequest(PerformanceScheduleRequest request) {
        if (request == null) {
            throw badRequest("Schedule request is required.");
        }
        if (request.getProjectId() == null || request.getProcessFlowId() == null) {
            throw badRequest("projectId and processFlowId are required.");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw badRequest("Schedule name is required.");
        }
        if (request.getCronExpression() == null || request.getCronExpression().isBlank()) {
            throw badRequest("Cron expression is required.");
        }
        if (request.getTimezone() == null || request.getTimezone().isBlank()) {
            throw badRequest("Timezone is required.");
        }
        PerformanceRequest snapshot = request.getRequestSnapshot();
        if (snapshot == null) {
            throw badRequest("requestSnapshot is required.");
        }
        if (!request.getProjectId().equals(snapshot.getProjectId()) || !request.getProcessFlowId().equals(snapshot.getProcessFlowId())) {
            throw badRequest("Schedule project and flow must match request snapshot.");
        }
        if (snapshot.getEnvironment() == null || snapshot.getEnvironment().isBlank()) {
            throw badRequest("Request snapshot environment is required.");
        }
        if (snapshot.getThreadCount() == null || snapshot.getThreadCount() <= 0) {
            throw badRequest("Request snapshot threadCount must be positive.");
        }
        if (snapshot.getThresholdPreset() == PerformanceThresholdPreset.CUSTOM
                && (snapshot.getMaxErrorRatePercent() == null
                || snapshot.getMaxAverageMs() == null
                || snapshot.getMaxP95Ms() == null
                || snapshot.getMaxP99Ms() == null
                || snapshot.getMinThroughputPerSecond() == null)) {
            throw badRequest("CUSTOM threshold preset requires all threshold values.");
        }
        nextRun(request.getCronExpression(), request.getTimezone(), new Date());
    }

    private void applyRequest(PerformanceScheduleEntity entity, PerformanceScheduleRequest request) {
        entity.setProjectId(request.getProjectId());
        entity.setProcessFlowId(request.getProcessFlowId());
        entity.setName(request.getName().trim());
        entity.setCronExpression(request.getCronExpression().trim());
        entity.setTimezone(request.getTimezone().trim());
        entity.setRequestSnapshot(request.getRequestSnapshot());
    }

    private PerformanceScheduleEntity find(Long scheduleId) {
        if (scheduleId == null) {
            throw badRequest("scheduleId is required.");
        }
        return scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found: " + scheduleId));
    }

    private PerformanceScheduleDto toDto(PerformanceScheduleEntity entity) {
        return new PerformanceScheduleDto(
                entity.getScheduleId(),
                entity.getProjectId(),
                entity.getProcessFlowId(),
                entity.getName(),
                entity.getCronExpression(),
                entity.getTimezone(),
                entity.getEnabled(),
                entity.getRequestSnapshot(),
                entity.getLastRunAt(),
                entity.getNextRunAt(),
                entity.getLastResultId(),
                entity.getLastStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
