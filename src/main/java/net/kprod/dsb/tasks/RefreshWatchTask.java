package net.kprod.dsb.tasks;

import net.kprod.dsb.data.NoAuthContext;
import net.kprod.dsb.data.NoAuthContextHolder;
import net.kprod.dsb.monitoring.MonitoringService;
import net.kprod.dsb.service.DriveChangeManagerService;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

public class RefreshWatchTask implements Runnable{
	private ApplicationContext ctx;
	//private Authentication auth;
	private String username;


	public RefreshWatchTask(ApplicationContext ctx, String username){
		this.ctx = ctx;
		//this.auth = auth;
		this.username = username;
	}

	@Override
	public void run() {
		DriveChangeManagerService service = ctx.getBean(DriveChangeManagerService.class);

//		SecurityContext context = SecurityContextHolder.getContext();
//		context.setAuthentication(auth);

		NoAuthContextHolder.setContext(new NoAuthContext(username));


		MonitoringService monitoringService = ctx.getBean(MonitoringService.class);
		monitoringService.start("RefreshWatchTask", "run");

		long startTime = System.currentTimeMillis();
		try {
			//TODO set username here
            service.renewWatch();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
		monitoringService.end(System.currentTimeMillis() - startTime);
    }
}