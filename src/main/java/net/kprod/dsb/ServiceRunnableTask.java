package net.kprod.dsb;

import net.kprod.dsb.service.DriveService;
import org.springframework.context.ApplicationContext;

public class ServiceRunnableTask implements Runnable{
	private ApplicationContext ctx;

	public ServiceRunnableTask(ApplicationContext ctx){
		this.ctx = ctx;

	}

	@Override
	public void run() {
		DriveService service = ctx.getBean(DriveService.class);
		service.flushChanges();
	}
}