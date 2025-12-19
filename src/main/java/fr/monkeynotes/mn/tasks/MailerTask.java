package fr.monkeynotes.mn.tasks;

import fr.monkeynotes.mn.monitoring.MonitoringService;
import fr.monkeynotes.mn.service.MailService;
import org.springframework.context.ApplicationContext;

public class MailerTask implements Runnable{
	private ApplicationContext ctx;

	public MailerTask(ApplicationContext ctx){
		this.ctx = ctx;
	}

	@Override
	public void run() {
		MonitoringService monitoringService = ctx.getBean(MonitoringService.class);
		monitoringService.start("MailerTask", "run");
		MailService mailService = ctx.getBean(MailService.class);
		long startTime = System.currentTimeMillis();
		mailService.sendAsyncProcessFinishedMessage();
		monitoringService.end(System.currentTimeMillis() - startTime);
	}
}