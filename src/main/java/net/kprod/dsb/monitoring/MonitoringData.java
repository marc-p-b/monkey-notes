package net.kprod.dsb.monitoring;

import net.kprod.dsb.ServiceException;

import java.util.Optional;

/**
 * Immutable bean holding monitoring data
 * Used to initiate monitoring behavior and values
 * Use {@link Builder} as a builder
 */
public class MonitoringData {
    //process id
    private final String id;
    //service name (Controller.methodName)
    private final String service;
    //service suffix if needed
    private final String suffix;
    //force trace mode
    private final boolean traceMode;
    //use by standard java hash function
    private int hashCode;

    /**
     * {@link MonitoringData} builder class
     */
    public static class Builder {
        private String id;
        private String service;
        private String suffix;
        private boolean traceMode = false;

        /**
         * {@link MonitoringData} builder constructor
         * @param service mandatory service name
         */
        public Builder(String service, String processId) {
            this.service = service;
            this.id = processId;
        }

        /**
         * Service suffix name value
         * @param suffix service suffix name
         * @return builder object
         */
        public Builder setSuffix(String suffix) {
            this.suffix = suffix;
            return this;
        }

        /**
         * Force tracing mode
         * @param traceMode if true, no monitoring is kept in database
         * @return builder object
         */
        public Builder setTraceMode(boolean traceMode) {
            this.traceMode = traceMode;
            return this;
        }

        /**
         * Build method
         * @return Builded {@link MonitoringData} object
         */
        public MonitoringData build() throws ServiceException {
//            if(StringUtils.isEmpty(service) || StringUtils.isEmpty(id)) {
//                throw new ServiceException("Unable to build MonitorData");
//            }
            //TODO this is not great, find something better to ensure that those values are not null
            return new MonitoringData(
                    this.id,
                    this.service,
                    this.suffix,
                    this.traceMode);
        }

        /**
         * Duplicate data and adds a suffix
         * @return Builded {@link MonitoringData} object
         */
        public static MonitoringData duplicate(MonitoringData monitoringData, String suffix) {
            return new MonitoringData(
                    monitoringData.getId(),
                    monitoringData.getService(),
                    suffix,
                    monitoringData.isTraceMode());
        }
    }

    //private constructor (use builder is necessary)
    private MonitoringData(String id, String service, String suffix, boolean traceMode) {
        this.id = id;
        this.service = service;
        this.suffix = suffix;
        this.traceMode = traceMode;
    }

    public String getId() {
        return id;
    }

    public String getService() {
        return service;
    }

    public Optional<String> getSuffix() {
        return Optional.ofNullable(suffix);
    }

    public boolean isTraceMode() {
        return traceMode;
    }

    /**
     * Std java hash function
     * @return hash code
     */
    @Override
    public int hashCode() {
        int result = hashCode;
        if (result == 0) {
            String id = this.getId();
            String suffix = this.getSuffix().orElse("");

            result = id.hashCode();
            result = 31 * result + service.hashCode();
            result = 31 * result + suffix.hashCode();
            result = 31 * result + Boolean.valueOf(traceMode).hashCode();
            hashCode = result;
        }
        return result;
    }

    /**
     * Std equals method override
     * @param obj object to compare
     * @return equals or not
     */
    @Override
    public boolean equals(Object obj) {
        if(obj == this) {
            return true;
        }
        if(obj instanceof MonitoringData) {
            MonitoringData other = (MonitoringData) obj;
            String id = this.getId();
            //TODO as in duplicate mode suffix is mandatory, is it relevant to consider as part of equality ?
            String suffix = this.getSuffix().orElse("");

            String otherId = other.getId();
            String otherSuffix = other.getSuffix().orElse("");

            return this.traceMode == other.traceMode &&
                    id.equals(otherId) &&
                    this.service.equals(other.service) &&
                    suffix.equals(otherSuffix);
        }
        return false;
    }
}