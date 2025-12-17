package fr.monkeynotes.mn.tasks;

import fr.monkeynotes.mn.data.dto.AsyncProcessFileEvent;
import fr.monkeynotes.mn.monitoring.MonitoringService;
import fr.monkeynotes.mn.service.MailService;
import fr.monkeynotes.mn.service.ProcessService;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.stream.Collectors;

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
		ProcessService processService = ctx.getBean(ProcessService.class);

		long startTime = System.currentTimeMillis();
		//TODO wrap in a service
		List<AsyncProcessFileEvent> list = processService.getAllFileEvents();

		if(list.isEmpty()){
			return;
		}

		String subject = new StringBuilder()
				.append(list.size())
				.append(" updated files available")
				.toString();

		String body = list.stream()
			.filter(AsyncProcessFileEvent::notNotified)
			.map(fe -> {

				return new StringBuilder()
						.append(fe.getFolderName())
						.append("/")
						.append(fe.getFileName())
						.append(" modified ")
						.append(fe.getModifiedPages())
						.append(" on a total of ")
						.append(fe.getTotalPages())
						.toString();

			})
			.collect(Collectors.joining("\n"));
		list.forEach(e -> {e.nofified();});
		//TODO email from user
		String[] target = new String[]{"TODO"};
		mailService.sendSimpleMessage(target, subject, body);
		monitoringService.end(System.currentTimeMillis() - startTime);
	}
}