package net.kprod.dsb.tasks;

import net.kprod.dsb.monitoring.MonitoringService;
import net.kprod.dsb.service.DriveService;
import org.springframework.context.ApplicationContext;

public class RefreshTokenTask implements Runnable{
	private ApplicationContext ctx;

	public RefreshTokenTask(ApplicationContext ctx){
		this.ctx = ctx;

	}

	@Override
	public void run() {
		DriveService service = ctx.getBean(DriveService.class);

		MonitoringService monitoringService = ctx.getBean(MonitoringService.class);
		monitoringService.start("RefreshTokenTask", "run");

		long startTime = System.currentTimeMillis();
		service.refreshToken();

		monitoringService.end(System.currentTimeMillis() - startTime);


    }
}