package net.kprod.mn.monitoring;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Aspect
@Component
public class ControllerLogAsyncAspect {
    @Autowired
    private MonitoringService monitoringService;

    private Logger LOG = LoggerFactory.getLogger(ControllerLogAsyncAspect.class);

    /**
     * Async annotation interceptor
     * Execute function and expects a {@link CompletableFuture<Long>}
     * After execution, report end of process with execution time
     * @param joinPoint AOP jointPoint
     * @return
     * @throws Throwable
     */
    @Around("@annotation(fr.lmsg.mntt.monitoring.MonitoringAsync)"
            + " && !@annotation(fr.lmsg.mntt.monitoring.MonitoringDisable)")
    public void asyncMethodInterceptor(ProceedingJoinPoint joinPoint) throws Throwable {
        Object proceed = joinPoint.proceed();

        if(proceed instanceof CompletableFuture) {
            CompletableFuture<?> future = (CompletableFuture<?>) proceed;
            future.thenAccept(futureReturnValue -> {

                if (futureReturnValue instanceof AsyncResult) {
                    AsyncResult result = (AsyncResult) futureReturnValue;
                    monitoringService.end(result.getRunTime());
                    if(result.isSuccessful()) {
                    } else {
                        LOG.error("AsyncHandler caught exception", result.getException());
                    }
                }
            });
        } else {
            System.err.println("Async method does not return CompletableFuture");
        }
    }
}