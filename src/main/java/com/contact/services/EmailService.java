package com.contact.services;

import java.util.Properties;

import org.springframework.stereotype.Service;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
public boolean sendEmail(String subject, String message, String to) {
    	
    	boolean flag = false;
        String from = "shabbirnoman09@gmail.com";
        String host = "smtp.gmail.com";

        // get system properties
        Properties properties = System.getProperties();

        // setting important properties
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", "465");
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.auth", "true");

        // Step 1: get session object
        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                
                return new PasswordAuthentication("shabbirnoman09@gmail.com", "rwqa esbv faof wlqk");
            }
        });

        session.setDebug(true);

        try {
            // Step 2: compose message
            MimeMessage m = new MimeMessage(session);
            m.setFrom(new InternetAddress(from));
            m.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            m.setSubject(subject);
            m.setText(message);

            // Step 3: send message
            Transport.send(m);
            System.out.println("Sent successfully...");
            
            flag = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }
}
