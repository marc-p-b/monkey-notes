package net.kprod.dsb.monitoring;

import net.kprod.dsb.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** {@inheritDoc} */
@Service
public class MonitoringServiceImpl implements MonitoringService {
    public static final String MONITORING_PROCESS_NAME = "_processName";
    public static final String MONITORING_PROCESS_ID = "_processId";
    public static final String MONITORING_PROCESS_ID_SHORT = "_processIdShort";
    public static final String MONITORING_PROCESS_SUFFIX = "_processSuffix";

    private Logger LOG = LoggerFactory.getLogger(MonitoringServiceImpl.class);

    protected final static int LOG_DATA_ID_LENGTH = 36;
    protected final static int LOG_DATA_ID_SHORT_LENGTH = 12;
    protected final static int LOG_DATA_ID_SHORT_DIFF = LOG_DATA_ID_LENGTH - LOG_DATA_ID_SHORT_LENGTH;

    public static final String SERVICENAME_SEPARATOR = ".";

    @Value("${tooling.error.stacktrace.include:false}")
    private boolean includeStackTrace;

    @Value("${tooling.error.stacktrace.log:true}")
    private boolean logStackTrace;

    public static final String STACKTRACE_UNAVAILABLE_MESSAGE = "unavailable";

    /** {@inheritDoc} */
    @Override
    public String start(String controllerName, String methodName) {
        MDC.clear();
        //processId is a random UUID
        String processId = UUID.randomUUID().toString();

        StringBuilder sbServiceName = new StringBuilder();
        sbServiceName
                .append(controllerName)
                .append(SERVICENAME_SEPARATOR)
                .append(methodName);

        try {
            MonitoringData monitoringData = new MonitoringData
                    .Builder(sbServiceName.toString(), processId)
                    .build();
            this.initMonitoring(monitoringData);
        } catch (ServiceException e) {
            LOG.error("Failed to start monitoring, [{}]", e.getMessage());
        }
        return processId;
    }

    /** {@inheritDoc} */
    @Override
    public String keep(MonitoringData monitoringData, String suffix) {
        MonitoringData monitoringDataAsync = MonitoringData.Builder.duplicate(monitoringData, suffix);
        this.initMonitoring(monitoringDataAsync);
        return monitoringDataAsync.getId();
    }

    private void initMonitoring(MonitoringData monitoringData) {
        MDC.put(MONITORING_PROCESS_NAME, monitoringData.getService());
        MDC.put(MONITORING_PROCESS_ID, monitoringData.getId());
        //Short version of processId (split after the last UUID dash)
        MDC.put(MONITORING_PROCESS_ID_SHORT, this.getShortLogDataId(monitoringData.getId()));
        if(monitoringData.getSuffix().isPresent()) {
            MDC.put(MONITORING_PROCESS_SUFFIX, monitoringData.getSuffix().get());
        }

        LOG.debug("Start monitoring processName [{}] id [{}]",
                monitoringData.getService(),
                monitoringData.getId());
    }

    // mark process as finished
    @Override
    public void end(long elapsedTime) {
        LOG.debug("End monitoring processName [{}] id [{}] took [{}] ms",
                this.getProcessName(),
                this.getProcessId(),
                elapsedTime);

        //clear MDC context
        MDC.clear();
    }

    public MonitoringData getCurrentMonitoringData() {
        try {
            return new MonitoringData.Builder(this.getProcessName(), this.getProcessId())
                    .setSuffix(getProcessSuffix())
                    .build();
        } catch (ServiceException e) {
            LOG.error("Failed to get current monitoring, [{}]", e.getMessage());
        }
        return null;
    }

    //get processId from MDC
    private String getProcessId() {
        return MDC.get(MONITORING_PROCESS_ID);
    }

    //get processName from MDC
    private String getProcessName() {
        return MDC.get(MONITORING_PROCESS_NAME);
    }

    //get processSuffix from MDC
    private String getProcessSuffix() {
        return MDC.get(MONITORING_PROCESS_SUFFIX);
    }

    //shortening processId UUID
    private String getShortLogDataId(String logDataId) {
        if(logDataId == null || logDataId.length() < LOG_DATA_ID_SHORT_DIFF) {
            return logDataId;
        }
        return logDataId.substring(LOG_DATA_ID_SHORT_DIFF);
    }

//    /** {@inheritDoc} */
//    @Override
//    public ResponseEntity<ResponseException> createErrorResponse(Exception parentException, HttpServiceException serviceException) {
//        //Create response
//        ResponseException responseException = this.processException(parentException, Optional.of(serviceException));
//        //Return response entity with proper status
//        return ResponseEntity.status(serviceException.getStatus()).body(responseException);
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    public ResponseException processException(Exception exception) {
//        return this.processException(exception, Optional.empty());
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    public ResponseException processException(Exception parentException, Optional<HttpServiceException> optFinalException) {
//        ResponseException responseException = new ResponseException();
//
//        if(optFinalException.isPresent()) {
//            responseException.setMessage(Msg.format("Exception message [{}] translated to [{}] status [{}]",
//                    parentException.getMessage(),
//                    optFinalException.get().getMessage(),
//                    optFinalException.get().getReason()));
//        } else {
//            responseException.setMessage(Msg.format("Exception message [{}]",
//                    parentException.getMessage()));
//        }
//
//        if(includeStackTrace) {
//            responseException.setStacktrace(ExceptionUtils.getStacktraceAsString(parentException));
//        } else {
//            responseException.setStacktrace(STACKTRACE_UNAVAILABLE_MESSAGE);
//        }
//
//        LOG.error(responseException.getMessage());
//
//        if(logStackTrace) {
//            LOG.error(responseException.getStacktrace());
//        }
//
//        return responseException;
//    }


}