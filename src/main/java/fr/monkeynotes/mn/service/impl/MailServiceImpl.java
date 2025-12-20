package fr.monkeynotes.mn.service.impl;

import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.transactional.SendContact;
import com.mailjet.client.transactional.SendEmailsRequest;
import com.mailjet.client.transactional.TransactionalEmail;
import com.mailjet.client.transactional.response.SendEmailsResponse;
import fr.monkeynotes.mn.data.dto.AsyncProcess;
import fr.monkeynotes.mn.data.dto.AsyncProcessFileEvent;
import fr.monkeynotes.mn.data.dto.DtoUser;
import fr.monkeynotes.mn.service.MailService;
import fr.monkeynotes.mn.service.ProcessService;
import fr.monkeynotes.mn.service.UserService;
import fr.monkeynotes.mn.tasks.MailerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MailServiceImpl implements MailService {
    private Logger LOG = LoggerFactory.getLogger(MailService.class);

    @Value("${app.email.enable:false}")
    private boolean enable;

    @Value("${app.email.mailjet.apikey.public}")
    private String mailjetApiKeyPublic;

    @Value("${app.email.mailjet.apikey.private}")
    private String mailjetApiKeyPrivate;

    private MailjetClient mailjetClient;

    @Value("${app.email.sender}")
    private String emailSender;

    @Value("${app.email.reports.delay}")
    private int emailReportDelay;

    @Autowired
    private TaskScheduler taskScheduler;

    @Autowired
    private ApplicationContext applicationContext;

    @EventListener(ApplicationReadyEvent.class)
    public void initMailjetClient() {
        if(!enable) {
            return;
        }
        ClientOptions options = ClientOptions.builder()
                .apiKey(mailjetApiKeyPublic)
                .apiSecretKey(mailjetApiKeyPrivate)
                .build();

        mailjetClient = new MailjetClient(options);

        taskScheduler.scheduleWithFixedDelay(new MailerTask(applicationContext), Duration.of(emailReportDelay, ChronoUnit.MINUTES));

    }

    @Autowired
    private ProcessService processService;

    @Autowired
    private UserService userService;

    @Override
    public void sendAsyncProcessFinishedMessage() {
        Map<String, List<AsyncProcess>> mapProcesses = processService.getAllProcessesMapByUser();

        if(mapProcesses.isEmpty()){
            return;
        }

        //processService.processDebug();

        mapProcesses.forEach((username, listProcesses) -> {

            try {
                DtoUser dtoUser = userService.getUser(username);

                String body = listProcesses.stream()
                        .flatMap(p -> p.getFileEvents().stream())
                        .map(fe -> {
                            return new StringBuilder()
                                    .append(fe.getFolderName())
                                    .append("/")
                                    .append(fe.getFileName())
                                    .append(" modified ")
                                    .append(fe.getModifiedPages())
                                    .append(" pages on a total of ")
                                    .append(fe.getTotalPages())
                                    .toString();

                        })
                        .collect(Collectors.joining(", "));

                long modified = listProcesses.stream()
                        .flatMap(p -> p.getFileEvents().stream())
                        .count();

                String subject = new StringBuilder()
                        .append(modified)
                        .append(" modified documents")
                        .toString();

                String emails[] = {dtoUser.getEmail()};

                this.sendSimpleMessage(emails, subject, body);

            } catch (UsernameNotFoundException e) {
                //todo err
            }
        });
    }

    public void sendSimpleMessage(String[] to, String subject, String body) {
        if(enable) {
            sendMailjetMessage(to, subject, body);
            LOG.info("Sent email to " + Arrays.toString(to));
        } else {
            LOG.info("Email disabled");
        }
    }

    private void sendMailjetMessage(String[] to, String subject, String body) {
        if (mailjetClient == null) {
            initMailjetClient();
        }

        List<SendContact> listTo = new ArrayList<>();
        for(String toItem : to) {
            listTo.add(new SendContact(toItem));
        }

        TransactionalEmail message = TransactionalEmail
                .builder()
                .from(new SendContact(emailSender, "Monkey notify"))
                .to(listTo)
                .htmlPart(body)
                .subject(subject)
                .build();

        SendEmailsRequest request = SendEmailsRequest
                .builder()
                .message(message)
                .build();
        try {
            SendEmailsResponse response = request.sendWith(mailjetClient);

            System.out.println(response);
        } catch (MailjetException e) {
            LOG.error("Failed to send email", e);
        }
    }

}
