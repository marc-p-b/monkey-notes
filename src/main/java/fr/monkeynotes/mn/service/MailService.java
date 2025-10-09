package fr.monkeynotes.mn.service;

public interface MailService {
    void sendSimpleMessage(String[] to, String subject, String body);
}
