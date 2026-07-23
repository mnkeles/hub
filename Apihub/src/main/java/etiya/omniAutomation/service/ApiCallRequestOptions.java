package etiya.omniAutomation.service;

public record ApiCallRequestOptions(Integer timeoutMs) {
    public boolean hasTimeout() {
        return timeoutMs != null && timeoutMs > 0;
    }
}
