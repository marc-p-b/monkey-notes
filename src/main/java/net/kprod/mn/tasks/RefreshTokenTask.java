package net.kprod.mn.tasks;

import net.kprod.mn.data.NoAuthContext;
import net.kprod.mn.data.NoAuthContextHolder;
import net.kprod.mn.monitoring.MonitoringService;
import net.kprod.mn.service.DriveService;
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
		DriveService service = ctx.getBean(DriveService.class);

//		SecurityContext context = SecurityContextHolder.getContext();
//		context.setAuthentication(auth);
		NoAuthContextHolder.setContext(new NoAuthContext(username));

		MonitoringService monitoringService = ctx.getBean(MonitoringService.class);
		monitoringService.start("RefreshTokenTask", "run");

		long startTime = System.currentTimeMillis();
		service.refreshToken();

		monitoringService.end(System.currentTimeMillis() - startTime);


    }
}