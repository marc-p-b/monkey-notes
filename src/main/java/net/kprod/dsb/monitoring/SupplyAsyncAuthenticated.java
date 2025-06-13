package net.kprod.dsb.monitoring;

import net.kprod.dsb.ServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.function.Supplier;

public class SupplyAsyncAuthenticated implements Supplier {
        private MonitoringData monitorData;
        private AsyncRunnable runnable;
        private MonitoringService monitoringService;
        private Authentication authentication;

    /**
     * Constructor
     * @param monitoringService
     * @param runnable
     */
    public SupplyAsyncAuthenticated(MonitoringService monitoringService, MonitoringData monitorData, Authentication authentication, AsyncRunnable runnable) throws ServiceException {
        if(monitoringService == null ||
                monitorData == null ||
                runnable == null) {
            //FIXME
            throw new ServiceException("All parameters must be defined");
        }
        this.monitoringService = monitoringService;
        this.monitorData = monitorData;
        this.runnable = runnable;
        this.authentication = authentication;
    }


    /**
     * Async execution supplier
     * Execute runnable method and returns execution time
     * @return execution time in milliseconds
     */
    @Override
    public Object get() {
        monitoringService.keep(monitorData,"async");
        //TODO also user noauthcontext ?
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        long start = System.currentTimeMillis();

        try {
            runnable.runThrows();
        } catch (Exception e) {
            return AsyncResult.failure(System.currentTimeMillis() - start, e);
        }
        return AsyncResult.success(System.currentTimeMillis() - start);
    }
}