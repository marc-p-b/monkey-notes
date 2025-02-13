package net.kprod.dsb;

import net.kprod.dsb.monitoring.MonitoringService;
import net.kprod.dsb.service.DriveChangeManagerService;
import net.kprod.dsb.service.DriveService;
import net.kprod.dsb.service.DriveUtilsService;
import org.springframework.context.ApplicationContext;

public class FlushTask implements Runnable{
	private ApplicationContext ctx;

	public FlushTask(ApplicationContext ctx){
		this.ctx = ctx;

	}

	@Override
	public void run() {
		MonitoringService monitoringService = ctx.getBean(MonitoringService.class);
		monitoringService.start("FlushTask", "run");
		DriveChangeManagerService service = ctx.getBean(DriveChangeManagerService.class);
		long startTime = System.currentTimeMillis();
		service.flushChanges();
		monitoringService.end(System.currentTimeMillis() - startTime);
	}
}