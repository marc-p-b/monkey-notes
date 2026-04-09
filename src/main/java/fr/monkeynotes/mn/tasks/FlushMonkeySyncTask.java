package fr.monkeynotes.mn.tasks;

import fr.monkeynotes.mn.data.NoAuthContextHolder;
import fr.monkeynotes.mn.monitoring.MonitoringService;
import fr.monkeynotes.mn.service.MonkeySyncService;
import org.springframework.context.ApplicationContext;

public class FlushMonkeySyncTask implements Runnable{
	private ApplicationContext ctx;

	public FlushMonkeySyncTask(ApplicationContext ctx){
		this.ctx = ctx;
	}

	@Override
	public void run() {
		try {
			MonkeySyncService service = ctx.getBean(MonkeySyncService.class);

			MonitoringService monitoringService = ctx.getBean(MonitoringService.class);
			monitoringService.start("FlushMonkeySyncTask", "run");

			long startTime = System.currentTimeMillis();

			service.flushMonkeySync();

			monitoringService.end(System.currentTimeMillis() - startTime);
		} finally {
			NoAuthContextHolder.clearContext();
		}
    }
}