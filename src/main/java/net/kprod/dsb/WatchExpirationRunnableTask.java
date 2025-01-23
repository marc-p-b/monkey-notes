package net.kprod.dsb;

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
		//service.flushChanges();
        try {
            service.renewWatch();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}