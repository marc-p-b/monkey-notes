package net.kprod.mn.service;

public interface MailService {
    void sendSimpleMessage(String[] to, String subject, String body);
}
