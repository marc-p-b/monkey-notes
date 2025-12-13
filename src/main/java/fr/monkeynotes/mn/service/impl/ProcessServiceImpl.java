package fr.monkeynotes.mn.service.impl;

import fr.monkeynotes.mn.data.dto.AsyncProcess;
import fr.monkeynotes.mn.data.dto.AsyncProcessEvent;
import fr.monkeynotes.mn.data.dto.DtoProcess;
import fr.monkeynotes.mn.data.enums.AsyncProcessName;
import fr.monkeynotes.mn.monitoring.AsyncResult;
import fr.monkeynotes.mn.monitoring.MonitoringData;
import fr.monkeynotes.mn.service.ProcessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class ProcessServiceImpl implements ProcessService {
    private Logger LOG = LoggerFactory.getLogger(ProcessService.class);
    private Map<String, AsyncProcess> mapAsyncProcess = new HashMap<>();
    long CONCURRENT_LIMIT = 1;

    @Override
    public void updateProcess(String processId, String event) {
        if(mapAsyncProcess.containsKey(processId) == false) {
            return;
        }
        AsyncProcess p = mapAsyncProcess.get(processId);
        p.addEvent(event);
    }

    @Override
    public void updateDetails(String processId, String details) {
        if(mapAsyncProcess.containsKey(processId) == false) {
            return;
        }
        AsyncProcess p = mapAsyncProcess.get(processId);
        p.addUpdateDetail(details);
    }

    @Override
    public List<AsyncProcessEvent> getEvents(String processId) {
        if(mapAsyncProcess.containsKey(processId) == false) {
            return new ArrayList<>();
        }
        AsyncProcess p = mapAsyncProcess.get(processId);
        return p.getEvents();
    }

    @Override
    public List<String> getDetails(String processId) {
        if(mapAsyncProcess.containsKey(processId) == false) {
            return new ArrayList<>();
        }
        AsyncProcess p = mapAsyncProcess.get(processId);
        return p.getUpdateDetails();
    }

    public void registerSyncProcess(AsyncProcessName name, MonitoringData monitoringData, String description, CompletableFuture<AsyncResult> future) {

//        Optional<AsyncProcess> optExistingProcess = mapAsyncProcess.values().stream()
//                .filter(p->p.getUniqueId().equals(uniqueId))
//                .findFirst();
//
//        if(optExistingProcess.isPresent()) {
//            AsyncProcess asyncProcess = optExistingProcess.get();
//
//            if(!asyncProcess.getFuture().isDone()) {
//                LOG.warn("Async process {} already exists", asyncProcess.getUniqueId());
//                return;
//            }
//
//            //TODO auto vaccum process here ?
//            //            if(asyncProcess.getFuture().isDone()) {
//            //
//            //            }
//
//        }


        String id = monitoringData.getId();
        AsyncProcess asyncProcess = new AsyncProcess()
                .setId(id)
                .setFuture(future)
                //.setUniqueId(uniqueId)
                //todo use enum ?
                .setName(name.name())
                .setCreatedAt(OffsetDateTime.now())
                .setDescription(description);

        LOG.info(asyncProcess.toString());

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
