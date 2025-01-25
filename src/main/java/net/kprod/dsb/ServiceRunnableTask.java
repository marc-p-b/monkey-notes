package net.kprod.dsb;

import net.kprod.dsb.monitoring.MonitoringForce;
import net.kprod.dsb.monitoring.MonitoringService;
import net.kprod.dsb.service.DriveService;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

public class ServiceRunnableTask implements Runnable{
	private ApplicationContext ctx;

	public ServiceRunnableTask(ApplicationContext ctx){
		this.ctx = ctx;

	}

	@Override
	public void run() {
		MonitoringService monitoringService = ctx.getBean(MonitoringService.class);
		monitoringService.start("ServiceRunnableTask", "run");
		DriveService service = ctx.getBean(DriveService.class);
		long startTime = System.currentTimeMillis();
		service.flushChanges();
		monitoringService.end(System.currentTimeMillis() - startTime);
	}
}