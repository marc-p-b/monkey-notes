package net.kprod.dsb;

import net.kprod.dsb.monitoring.MonitoringService;
import net.kprod.dsb.service.DriveService;
import org.springframework.context.ApplicationContext;

import java.io.IOException;

public class RefreshWatchTask implements Runnable{
	private ApplicationContext ctx;

	public RefreshWatchTask(ApplicationContext ctx){
		this.ctx = ctx;

	}

	@Override
	public void run() {
		DriveService service = ctx.getBean(DriveService.class);

		MonitoringService monitoringService = ctx.getBean(MonitoringService.class);
		monitoringService.start("RefreshWatchTask", "run");

		long startTime = System.currentTimeMillis();
		try {
            service.renewWatch();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
		monitoringService.end(System.currentTimeMillis() - startTime);


    }
}