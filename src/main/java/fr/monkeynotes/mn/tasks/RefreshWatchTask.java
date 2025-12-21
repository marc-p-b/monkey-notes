package fr.monkeynotes.mn.tasks;

import fr.monkeynotes.mn.data.NoAuthContext;
import fr.monkeynotes.mn.data.NoAuthContextHolder;
import fr.monkeynotes.mn.monitoring.MonitoringService;
import fr.monkeynotes.mn.service.DriveChangeManagerService;
import org.springframework.context.ApplicationContext;

import java.io.IOException;

public class RefreshWatchTask implements Runnable{
	private ApplicationContext ctx;
	//private Authentication auth;
	private String username;


	public RefreshWatchTask(ApplicationContext ctx, String username){
		this.ctx = ctx;
		//this.auth = auth;
		this.username = username;
	}

	@Override
	public void run() {
		try {
			NoAuthContextHolder.setContext(new NoAuthContext(username));

			DriveChangeManagerService service = ctx.getBean(DriveChangeManagerService.class);

			MonitoringService monitoringService = ctx.getBean(MonitoringService.class);
			monitoringService.start("RefreshWatchTask", "run");

			long startTime = System.currentTimeMillis();
			try {
				service.renewWatch(username);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			monitoringService.end(System.currentTimeMillis() - startTime);
		} finally {
			NoAuthContextHolder.clearContext();
		}
    }
}