//package fr.monkeynotes.mn.service.impl;
//
//import com.mailjet.client.ClientOptions;
//import com.mailjet.client.MailjetClient;
//import com.mailjet.client.errors.MailjetException;
//import com.mailjet.client.transactional.SendContact;
//import com.mailjet.client.transactional.SendEmailsRequest;
//import com.mailjet.client.transactional.TransactionalEmail;
//import com.mailjet.client.transactional.response.SendEmailsResponse;
//import fr.monkeynotes.mn.service.MailService;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.context.event.ApplicationReadyEvent;
//import org.springframework.context.event.EventListener;
//import org.springframework.stereotype.Service;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//
//@Service
//public class MailServiceImpl implements MailService {
//    private Logger LOG = LoggerFactory.getLogger(MailService.class);
//
//    @Value("${app.email.enable:false}")
//    private boolean enable;
//
//    @Value("${app.email.mailjet.apikey.public}")
//    private String mailjetApiKeyPublic;
//
//    @Value("${app.email.mailjet.apikey.private}")
//    private String mailjetApiKeyPrivate;
//
//    private MailjetClient mailjetClient;
//
//    @Value("${app.email.sender}")
//    private String emailSender;
//
//    @EventListener(ApplicationReadyEvent.class)
//    public void initMailjetClient() {
//        if(!enable) {
//            return;
//        }
//        ClientOptions options = ClientOptions.builder()
//                .apiKey(mailjetApiKeyPublic)
//                .apiSecretKey(mailjetApiKeyPrivate)
//                .build();
//
//        mailjetClient = new MailjetClient(options);
//
//    }
//
//    public void sendSimpleMessage(String[] to, String subject, String body) {
//        if(enable) {
//            sendMailjetMessage(to, subject, body);
//            LOG.info("Sent email to " + Arrays.toString(to));
//        } else {
//            LOG.info("Email disabled");
//        }
//    }
//
//    private void sendMailjetMessage(String[] to, String subject, String body) {
//        if (mailjetClient == null) {
//            initMailjetClient();
//        }
//
//        List<SendContact> listTo = new ArrayList<>();
//        for(String toItem : to) {
//            listTo.add(new SendContact(toItem));
//        }
//
//        TransactionalEmail message = TransactionalEmail
//                .builder()
//                .from(new SendContact(emailSender, "Monkey notify"))
//                .to(listTo)
//                .htmlPart(body)
//                .subject(subject)
//                .build();
//
//        SendEmailsRequest request = SendEmailsRequest
//                .builder()
//                .message(message)
//                .build();
//        try {
//            SendEmailsResponse response = request.sendWith(mailjetClient);
//
//            System.out.println(response);
//        } catch (MailjetException e) {
//            LOG.error("Failed to send email", e);
//        }
//    }
//
//}
