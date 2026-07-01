package etiya.omniAutomation.common;

public class GeneralEnums {

    public enum FilterCriteria {
        PROCESS_FLOW_ID,
        PROJECT_ID,
    }

    public enum PerformanceStatus {
        RUNNING,
        FAILED,
        COMPLETED,
        STOPPING,
        COMPLETED_PASSED,
        COMPLETED_FAILED,
        STOPPED,
        ERROR
    }
}
