package net.kprod.dsb.tasks;

import net.kprod.dsb.monitoring.MonitoringService;
import net.kprod.dsb.service.DriveChangeManagerService;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

public class RefreshWatchTask implements Runnable{
	private ApplicationContext ctx;

	public RefreshWatchTask(ApplicationContext ctx){
		this.ctx = ctx;
	}

	@Override
	public void run() {
		DriveChangeManagerService service = ctx.getBean(DriveChangeManagerService.class);

		SecurityContext context = SecurityContextHolder.getContext();

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