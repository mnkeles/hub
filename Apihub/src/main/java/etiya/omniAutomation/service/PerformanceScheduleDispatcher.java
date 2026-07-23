package etiya.omniAutomation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class PerformanceScheduleDispatcher {

    private final PerformanceScheduleService performanceScheduleService;

    @Scheduled(fixedDelay = 60000)
    public void dispatch() {
        performanceScheduleService.runDueSchedules(new Date());
    }
}
