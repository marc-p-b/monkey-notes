package fr.monkeynotes.mn.tasks;

import fr.monkeynotes.mn.data.NoAuthContextHolder;
import fr.monkeynotes.mn.monitoring.MonitoringService;
import fr.monkeynotes.mn.service.UpdateService;
import org.springframework.context.ApplicationContext;

public class FlushMonkeySyncTask implements Runnable{
	private ApplicationContext ctx;
	//private Authentication auth;


	public FlushMonkeySyncTask(ApplicationContext ctx){
		this.ctx = ctx;
		//this.auth = auth;
		//this.username = username;
	}

	@Override
	public void run() {
		try {
			//NoAuthContextHolder.setContext(new NoAuthContext(username));

			UpdateService service = ctx.getBean(UpdateService.class);

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