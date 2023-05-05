/*
 *   gcloudlicensemanagement - AlertFunctions.java
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
package com.slinkytoybox.gcloud.licensing.businesslogic;

import com.slinkytoybox.gcloud.licensing.connection.SMTPConnection;
import com.slinkytoybox.gcloud.licensing.dto.internal.AlertMessage;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 *
 * @author Michael Junek (michael@juneks.com.au)
 */
@Slf4j
@Component

public class AlertFunctions {
    
    @Autowired
    private Environment env;
    
    @Autowired
    private SMTPConnection smtpConnection;
    
    @Value("${info.build.name}")
            private String buildName;
    @Value("${info.build.version}")
            private String buildVersion;
    
    
    
    void alertPlatformAdmins(AlertMessage alertMessage) {
        final String logPrefix = "alertPlatformAdmins() - ";
        log.trace("{}Entering method", logPrefix);
        Boolean alertTeam = env.getProperty("alert.platform.enabled", Boolean.class);
        if (!Boolean.TRUE.equals(alertTeam)){
            log.warn("{}Platform team emails are disabled. Check alert.platform.enabled configuration option.", logPrefix);
            return;
        }
        
        String platformTeam = env.getProperty("alert.platform.recipients", "");
        String subject = env.getProperty("alert.platform.subject", buildName + " " + buildVersion + " - Platform Alert");
        String sender = env.getProperty("alert.platform.sender", "");
        if (platformTeam.isEmpty() || sender.isEmpty()) {
            log.warn("{}No platform team recipient or sender address found. Not sending email", logPrefix);
            return;
        }
       
        String[] recipientArray = platformTeam.split("\\|");
        List<String> recipients = Arrays.asList(recipientArray);
        String template = "platformalert";
        smtpConnection.sendTemplateMail(recipients, sender, subject, template, alertMessage);
        log.trace("{}Leaving method", logPrefix);
    }
    
    
    public void alertUser(AlertMessage alertMessage, String emailAddress) {
        final String logPrefix = "alertUser() - ";
        log.trace("{}Entering method", logPrefix);
        if (emailAddress == null || emailAddress.isBlank()) {
            log.error("{}Email address was not supplied", logPrefix);
            return;
        }
         Boolean alertUser = env.getProperty("alert.user.enabled", Boolean.class);
        if (!Boolean.TRUE.equals(alertUser)){
            log.warn("{}User emails are disabled. Check alert.user.enabled configuration option.", logPrefix);
            return;
        }
        
        String subject = env.getProperty("alert.user.subject", buildName + " " + buildVersion + " - User Alert");
        String sender = env.getProperty("alert.user.sender", "");
        if (sender.isEmpty()) {
            log.error("{}No user sender address found. Not sending email", logPrefix);
            return;
        }
       
        String template = "useralert";
        smtpConnection.sendTemplateMail(emailAddress, sender, subject, template, alertMessage);
        
        log.trace("{}Leaving method", logPrefix);
    }
 
}
