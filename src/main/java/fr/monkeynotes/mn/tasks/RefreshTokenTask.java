package fr.monkeynotes.mn.tasks;

import fr.monkeynotes.mn.data.NoAuthContext;
import fr.monkeynotes.mn.data.NoAuthContextHolder;
import fr.monkeynotes.mn.monitoring.MonitoringService;
import fr.monkeynotes.mn.service.DriveService;
import org.springframework.context.ApplicationContext;

public class RefreshTokenTask implements Runnable{
	private ApplicationContext ctx;
	//private Authentication auth;
	private String username;

	public RefreshTokenTask(ApplicationContext ctx, String username) {
		this.ctx = ctx;
		this.username = username;
	}

	@Override
	public void run() {
		try {
			NoAuthContextHolder.setContext(new NoAuthContext(username));

			DriveService service = ctx.getBean(DriveService.class);

			MonitoringService monitoringService = ctx.getBean(MonitoringService.class);
			monitoringService.start("RefreshTokenTask", "run");

			long startTime = System.currentTimeMillis();
			service.refreshToken();

			monitoringService.end(System.currentTimeMillis() - startTime);
		} finally {
			NoAuthContextHolder.clearContext();
		}
    }
}