package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.PerformanceResultDto;
import etiya.omniAutomation.business.dto.PerformanceScheduleStatus;
import etiya.omniAutomation.entity.PerformanceScheduleEntity;
import etiya.omniAutomation.repository.PerformanceResultRepository;
import etiya.omniAutomation.repository.PerformanceScheduleRepository;
import etiya.omniAutomation.request.PerformanceRequest;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PerformanceScheduleDispatcherTest {

    private final PerformanceScheduleRepository scheduleRepository = mock(PerformanceScheduleRepository.class);
    private final PerformanceResultRepository resultRepository = mock(PerformanceResultRepository.class);
    private final PerformanceService performanceService = mock(PerformanceService.class);
    private final PerformanceScheduleService service = new PerformanceScheduleService(scheduleRepository, resultRepository, performanceService);

    @Test
    void dueScheduleStartsRun() {
        PerformanceScheduleEntity schedule = schedule();
        PerformanceResultDto result = new PerformanceResultDto();
        result.setPerformanceResultId(77L);
        when(scheduleRepository.findByEnabledTrueAndNextRunAtLessThanEqual(any(Date.class))).thenReturn(List.of(schedule));
        when(performanceService.executePerformanceTest(any(PerformanceRequest.class))).thenReturn(result);
        when(scheduleRepository.save(any(PerformanceScheduleEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.runDueSchedules(new Date());

        verify(performanceService).executePerformanceTest(any(PerformanceRequest.class));
        assertEquals(PerformanceScheduleStatus.STARTED, schedule.getLastStatus());
        assertEquals(77L, schedule.getLastResultId());
    }

    @Test
    void runningPreviousResultSkipsRun() {
        PerformanceScheduleEntity schedule = schedule();
        schedule.setLastResultId(55L);
        when(scheduleRepository.findByEnabledTrueAndNextRunAtLessThanEqual(any(Date.class))).thenReturn(List.of(schedule));
        when(resultRepository.existsByPerfRsltIdAndPerfStatusIn(eq(55L), anyCollection())).thenReturn(true);
        when(scheduleRepository.save(any(PerformanceScheduleEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.runDueSchedules(new Date());

        verify(performanceService, never()).executePerformanceTest(any(PerformanceRequest.class));
        assertEquals(PerformanceScheduleStatus.SKIPPED_RUNNING, schedule.getLastStatus());
    }

    @Test
    void failedStartRecordsFailedToStart() {
        PerformanceScheduleEntity schedule = schedule();
        when(scheduleRepository.findByEnabledTrueAndNextRunAtLessThanEqual(any(Date.class))).thenReturn(List.of(schedule));
        when(performanceService.executePerformanceTest(any(PerformanceRequest.class))).thenThrow(new IllegalStateException("boom"));
        when(scheduleRepository.save(any(PerformanceScheduleEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.runDueSchedules(new Date());

        assertEquals(PerformanceScheduleStatus.FAILED_TO_START, schedule.getLastStatus());
    }

    private PerformanceScheduleEntity schedule() {
        PerformanceRequest snapshot = new PerformanceRequest();
        snapshot.setProjectId(1L);
        snapshot.setProcessFlowId(2L);
        snapshot.setEnvironment("DEV");
        snapshot.setThreadCount(1);

        PerformanceScheduleEntity schedule = new PerformanceScheduleEntity();
        schedule.setScheduleId(10L);
        schedule.setProjectId(1L);
        schedule.setProcessFlowId(2L);
        schedule.setName("Hourly");
        schedule.setCronExpression("0 0 * * * *");
        schedule.setTimezone("Europe/Istanbul");
        schedule.setEnabled(true);
        schedule.setRequestSnapshot(snapshot);
        schedule.setNextRunAt(new Date(0));
        return schedule;
    }
}
