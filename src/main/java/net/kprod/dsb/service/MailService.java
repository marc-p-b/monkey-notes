package net.kprod.dsb.service;

public interface MailService {
    void sendSimpleMessage(String[] to, String subject, String body);
}
