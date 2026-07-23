package etiya.omniAutomation.service;

import etiya.omniAutomation.repository.PerformanceResultRepository;
import etiya.omniAutomation.repository.PerformanceScheduleRepository;
import etiya.omniAutomation.request.PerformanceRequest;
import etiya.omniAutomation.request.PerformanceScheduleRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class PerformanceScheduleServiceTest {

    private final PerformanceScheduleService service = new PerformanceScheduleService(
            mock(PerformanceScheduleRepository.class),
            mock(PerformanceResultRepository.class),
            mock(PerformanceService.class)
    );

    @Test
    void validCronCalculatesNextRun() {
        Date now = new Date();

        Date next = service.nextRun("0 0 * * * *", "Europe/Istanbul", now);

        assertTrue(next.after(now));
    }

    @Test
    void invalidCronFails() {
        assertThrows(ResponseStatusException.class, () -> service.nextRun("bad cron", "Europe/Istanbul", new Date()));
    }

    @Test
    void timezoneDefaultsAreAccepted() {
        Date now = new Date();

        Date next = service.nextRun("0 0 9 * * *", "Europe/Istanbul", now);

        assertTrue(next.after(now));
    }

    @Test
    void mismatchedSnapshotProjectFails() {
        PerformanceScheduleRequest request = validRequest();
        request.getRequestSnapshot().setProjectId(999L);

        assertThrows(ResponseStatusException.class, () -> service.create(request));
    }

    private PerformanceScheduleRequest validRequest() {
        PerformanceRequest snapshot = new PerformanceRequest();
        snapshot.setProjectId(1L);
        snapshot.setProcessFlowId(2L);
        snapshot.setEnvironment("DEV");
        snapshot.setThreadCount(1);

        PerformanceScheduleRequest request = new PerformanceScheduleRequest();
        request.setProjectId(1L);
        request.setProcessFlowId(2L);
        request.setName("Daily");
        request.setCronExpression("0 0 9 * * *");
        request.setTimezone("Europe/Istanbul");
        request.setEnabled(true);
        request.setRequestSnapshot(snapshot);
        return request;
    }
}
