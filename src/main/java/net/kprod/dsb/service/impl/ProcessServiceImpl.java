package net.kprod.dsb.service.impl;

import net.kprod.dsb.data.dto.AsyncProcess;
import net.kprod.dsb.data.dto.DtoProcess;
import net.kprod.dsb.data.enums.AsyncProcessName;
import net.kprod.dsb.monitoring.AsyncResult;
import net.kprod.dsb.monitoring.MonitoringData;
import net.kprod.dsb.service.ProcessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class ProcessServiceImpl implements ProcessService {
    private Logger LOG = LoggerFactory.getLogger(ProcessService.class);
    private Map<String, AsyncProcess> mapAsyncProcess = new HashMap<>();
    long CONCURRENT_LIMIT = 1;

    public void registerSyncProcess(AsyncProcessName name, MonitoringData monitoringData, String description, CompletableFuture<AsyncResult> future) {
        String id = monitoringData.getId();
        AsyncProcess asyncProcess = new AsyncProcess()
                .setId(id)
                .setFuture(future)
                //todo use enum ?
                .setName(name.name())
                .setCreatedAt(OffsetDateTime.now())
                .setDescription(description);

        mapAsyncProcess.put(id, asyncProcess);
    }

    public boolean concurrentProcessFull() {
        long count = mapAsyncProcess.values().stream()
                .filter(p -> p.getName().equals(AsyncProcessName.flushChanges.name()))
                .filter(p -> !p.getFuture().isDone())
                .count();

        if(count >= CONCURRENT_LIMIT) {
            LOG.debug("Concurrent process full ({} processes)", count);
            return true;
        }

        return false;
    }

    @Override
    public void cancelProcess(String id) {
        if(mapAsyncProcess.get(id) == null) {
            LOG.error("Process does not exists {}", id);
            return;
        }
        LOG.info("Request process cancellation{}", id);
        CompletableFuture future = mapAsyncProcess.get(id).getFuture();
        future.cancel(true);
        mapAsyncProcess.remove(id);
        LOG.info("Process cancelled {}", id);
    }


    @Override
    public List<DtoProcess> listProcess() {
        //Map<String, AsyncProcess> mapAsyncProcess = getMapAsyncProcess();

        List<DtoProcess> list = mapAsyncProcess.entrySet().stream()
            .map(e -> {
                AsyncProcess asyncProcess = e.getValue();
                String processName = asyncProcess.getName();
                CompletableFuture<AsyncResult> future = asyncProcess.getFuture();
                DtoProcess.Status status = DtoProcess.Status.unknown;
                String statusStr = "unknown";
                DtoProcess p = new DtoProcess(e.getKey(), processName);

                Duration d = Duration.between(asyncProcess.getCreatedAt(), OffsetDateTime.now());
                p.setDescription(asyncProcess.getDescription());
                String strDuration = new StringBuilder()
                        .append(d.toHoursPart()).append("h ")
                        .append(d.toMinutesPart()).append("m ")
                        .append(d.toSecondsPart()).append("s ").toString();
                p.setDuration(strDuration);


                if (future.isDone()) {
                    try {
                        AsyncResult asyncResult = future.get();
                        statusStr = switch (asyncResult.getState()) {
                            case failed -> "failed";
                            case completed -> "completed in " + asyncResult.getRunTime() + "ms";
                            default -> "unknown";
                        };

                        status = switch (asyncResult.getState()) {
                            case failed -> DtoProcess.Status.failed;
                            case completed -> DtoProcess.Status.completed;
                            default -> DtoProcess.Status.unknown;
                        };

                    } catch (InterruptedException | ExecutionException e2) {
                        statusStr = "ERROR while getting process status";
                        status = DtoProcess.Status.error;
                    }
                } else {
                    statusStr = "running";
                    status = DtoProcess.Status.running;
                }
                p.setStatusStr(statusStr);
                p.setStatus(status);
                return p;
            })
            .toList();


        //TODO add a checkbox on page to remove none running processes
        Set<String> set2Remove = list.stream()
                .filter(p -> !p.getStatus().equals(DtoProcess.Status.running))
                .map(DtoProcess::getId)
                .collect(Collectors.toSet());

        mapAsyncProcess.keySet().removeAll(set2Remove);

        return list;
    }
}
