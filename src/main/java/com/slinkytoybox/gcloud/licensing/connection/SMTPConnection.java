/*
 *   gcloudlicensemanagement - SMTPConnection.java
 *
 *   Copyright (c) 2022-2023, Slinky Software
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as
 *   published by the Free Software Foundation, either version 3 of the
 *   License, or (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   A copy of the GNU Affero General Public License is located in the 
 *   AGPL-3.0.md supplied with the source code.
 *
 */
package com.slinkytoybox.gcloud.licensing.connection;

import com.slinkytoybox.gcloud.licensing.businesslogic.MailContentBuilder;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Component;


/**
 *
 * @author Michael Junek (michael@juneks.com.au)
 */
@Component
@Slf4j

public class SMTPConnection {
    
    
    @Autowired
    private Environment env;

    @Autowired
    private JavaMailSenderImpl mailSender;

    @Autowired
    private MailContentBuilder mailContentBuilder;

   
    public void sendTemplateMail(@NonNull List<String> recipientList, @NonNull String sender, @NonNull String subject, String templateName, Object templateContents) {
        final String logPrefix = "sendTemplateMail(L<S>,S,S,S,O) - ";
        log.trace("{}Entering method", logPrefix);
        String body = mailContentBuilder.buildMessage(templateName, templateContents);
        sendMail(recipientList, sender, subject, body);
        log.trace("{}Leaving method", logPrefix);
    }

    public void sendTemplateMail(@NonNull String recipient, @NonNull String sender, @NonNull String subject, String templateName, Object templateContents) {
        final String logPrefix = "sendTemplateMail(S,S,S,S,O) - ";
        log.trace("{}Entering method", logPrefix);
        String body = mailContentBuilder.buildMessage(templateName, templateContents);
        sendMail(recipient, sender, subject, body);
        log.trace("{}Leaving method", logPrefix);
    }

    private void sendMail(@NonNull String recipient, @NonNull String sender, @NonNull String subject, @NonNull String body) {
        final String logPrefix = "sendMail(S,S,S,S) - ";
        log.trace("{}Entering method", logPrefix);
        List<String> recipientList = new ArrayList<>();
        recipientList.add(recipient);
        sendMail(recipientList, sender, subject, body);
        log.trace("{}Leaving method", logPrefix);
    }

    private void sendMail(@NonNull List<String> recipientList, @NonNull String sender, @NonNull String subject, @NonNull String body) {
        final String logPrefix = "sendMail(L<S>,S,S,S) - ";
        log.trace("{}Entering method", logPrefix);
        log.debug("{}Preparing message to be sent", logPrefix);
        MimeMessagePreparator messagePreparator = mimeMessage -> {
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage);
            for (String recipient : recipientList) {
                messageHelper.addTo(recipient);
            }
            messageHelper.setFrom(sender);
            messageHelper.setSubject(subject);
            messageHelper.setText(body, true);
        };

        try {
            log.info("{}Sending email: {}", logPrefix, messagePreparator);
            Boolean sendMailEnabled = env.getProperty("alert.sendmail.enabled", Boolean.class);
            if (!Boolean.TRUE.equals(sendMailEnabled)) {
                log.warn("{}Global sending of emails is disabled. Check alert.sendmail.enabled configuration option.", logPrefix);
                return;
            }
            mailSender.send(messagePreparator);
            log.info("{}Email sent", logPrefix);
        }
        catch (MailException ex) {
            log.error("{}An exception occurred sending the email", logPrefix, ex);
        }
        log.trace("{}Leaving method", logPrefix);
    }

    
}
