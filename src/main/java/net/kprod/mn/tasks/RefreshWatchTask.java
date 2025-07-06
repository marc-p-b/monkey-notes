package net.kprod.mn.tasks;

import net.kprod.mn.data.NoAuthContext;
import net.kprod.mn.data.NoAuthContextHolder;
import net.kprod.mn.monitoring.MonitoringService;
import net.kprod.mn.service.DriveChangeManagerService;
import org.springframework.context.ApplicationContext;

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
            service.renewWatch(username);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
		monitoringService.end(System.currentTimeMillis() - startTime);
    }
}