package net.kprod.dsb;

import net.kprod.dsb.monitoring.MonitoringService;
import net.kprod.dsb.service.DriveService;
import org.springframework.context.ApplicationContext;

import java.io.IOException;

public class WatchExpirationRunnableTask implements Runnable{
	private ApplicationContext ctx;

	public WatchExpirationRunnableTask(ApplicationContext ctx){
		this.ctx = ctx;

	}

	@Override
	public void run() {
		DriveService service = ctx.getBean(DriveService.class);

		MonitoringService monitoringService = ctx.getBean(MonitoringService.class);
		monitoringService.start("WatchExpirationRunnableTask", "run");

		long startTime = System.currentTimeMillis();
		try {
            service.renewWatch();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
		monitoringService.end(System.currentTimeMillis() - startTime);


    }
}