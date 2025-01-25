package net.kprod.dsb.monitoring;

/**
 * Monitoring service
 * Control process monitoring
 */
public interface MonitoringService {
    /**
     * Start process monitoring
     *
     * @param controllerName controller Spring name
     * @param methodName     controller method name
     * @return processId
     */
    String start(String controllerName, String methodName);

    /**
     * Continue monitoring, in case of @Async for example
     *
     * @param suffix process suffix
     * @return processId
     */
    String keep(MonitoringData monitoringData, String suffix);

    /**
     * Stop process monitoring
     *
     * @param elsapedTime process execution time in ms
     */
    void end(long elsapedTime);

    /**
     * Return current monitorData object
     * @return monitorData
     */
    MonitoringData getCurrentMonitoringData();

    /**
     * Create error response
     *
     * @param parentException  origin exception before rewriting
     * @param serviceException exception
     * @return created response entity
     */
//    ResponseEntity<ResponseException> createErrorResponse(Exception parentException, HttpServiceException serviceException);
//
//    /**
//     * Transform exception to a {@link ResponseException}
//     * @param exception origin exception before rewriting
//     * @return response exception object
//     */
//    ResponseException processException(Exception exception);
//
//    /**
//     * Transform exception to a {@link ResponseException}
//     * @param parentException origin exception before rewriting
//     * @param optFinalException optional final exception
//     * @return response exception object
//     */
//    ResponseException processException(Exception parentException, Optional<HttpServiceException> optFinalException);
}