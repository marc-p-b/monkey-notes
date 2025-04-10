package net.kprod.dsb.monitoring;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Aspect
@Component
public class ControllerLogAsyncAspect {
    @Autowired
    private MonitoringService monitoringService;

    //private Logger LOG = ToolingLoggerFactory.getLogger(ControllerLogAsyncAspect.class);

    /**
     * Async annotation interceptor
     * Execute function and expects a {@link CompletableFuture<Long>}
     * After execution, report end of process with execution time
     * @param joinPoint AOP jointPoint
     * @return
     * @throws Throwable
     */
    @Around("@annotation(org.springframework.scheduling.annotation.Async)"
            + " && !@annotation(net.kprod.dsb.monitoring.MonitoringDisable)")
    public void asyncMethodInterceptor(ProceedingJoinPoint joinPoint) throws Throwable {
        Object proceed = joinPoint.proceed();
        if(proceed instanceof CompletableFuture) {
                CompletableFuture<?> future = (CompletableFuture<?>) proceed;
                future.thenAccept(futureReturnValue -> {

                    if (futureReturnValue instanceof AsyncResult) {
                        AsyncResult result = (AsyncResult) futureReturnValue;
                        if(result.isSuccessful()) {
                            monitoringService.end(result.getRunTime());
                        } else {
                            //monitoringService.logException("Async handler", result.getException());
                            //LOG.error("AsyncHandler caught exception", result.getException());
                            System.err.println("AsyncHandler caught exception : " + result.getException());
                        }
                    }
                });
        } else {
            //LOG.error("Async method does not return CompletableFuture");
            System.err.println("Async method does not return CompletableFuture");
        }
    }
}