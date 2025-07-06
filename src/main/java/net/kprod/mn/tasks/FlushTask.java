package net.kprod.mn.tasks;

import net.kprod.mn.monitoring.MonitoringService;
import net.kprod.mn.service.DriveChangeManagerService;
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