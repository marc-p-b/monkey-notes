package net.kprod.dsb.tasks;

import net.kprod.dsb.data.NoAuthContext;
import net.kprod.dsb.data.NoAuthContextHolder;
import net.kprod.dsb.monitoring.MonitoringService;
import net.kprod.dsb.service.DriveService;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

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