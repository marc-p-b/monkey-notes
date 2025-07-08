package net.kprod.mn.monitoring;

import java.util.function.Supplier;

public class SupplyAsync implements Supplier {
        private MonitoringData monitorData;
        private AsyncRunnable runnable;
        private MonitoringService monitoringService;


    /**
     * Constructor
     * @param monitoringService
     * @param runnable
     */
    public SupplyAsync(MonitoringService monitoringService, MonitoringData monitorData, AsyncRunnable runnable)  {
        this.monitoringService = monitoringService;
        this.monitorData = monitorData;
        this.runnable = runnable;
    }

    /**
     * Async execution supplier
     * Execute runnable method and returns execution time
     * @return execution time in milliseconds
     */
    @Override
    public Object get() {
        monitoringService.keep(monitorData,"async");

        long start = System.currentTimeMillis();

        try {
            runnable.runThrows();
        } catch (Exception e) {
            return AsyncResult.failure(System.currentTimeMillis() - start, e);
        }
        return AsyncResult.success(System.currentTimeMillis() - start);
    }
}