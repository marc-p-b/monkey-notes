package fr.monkeynotes.mn.service;

public interface MailService {
    void sendAsyncProcessFinishedMessage();

    void sendSimpleMessage(String[] to, String subject, String body);
}
