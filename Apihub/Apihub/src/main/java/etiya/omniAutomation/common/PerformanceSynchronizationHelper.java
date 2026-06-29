package etiya.omniAutomation.common;

import org.springframework.transaction.support.TransactionSynchronization;

public class PerformanceSynchronizationHelper implements TransactionSynchronization {

    private Runnable runnable;

    private PerformanceSynchronizationHelper() {}

    public static TransactionSynchronization of(Runnable runnable) {
        PerformanceSynchronizationHelper performanceSynchronizationHelper = new PerformanceSynchronizationHelper();
        performanceSynchronizationHelper.runnable = runnable;
        return performanceSynchronizationHelper;
    }

    @Override
    public void afterCompletion(int status) {
        if (status == TransactionSynchronization.STATUS_COMMITTED) {
            runnable.run();
        }
    }
}
