/*
 *   gcloudlicensemanagement - JavaMailExtension.java
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
package com.slinkytoybox.gcloud.licensing.init;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

/**
 *
 * @author Michael Junek (michael@juneks.com.au)
 *
 */

@Slf4j
@Service("JavaMailExtension")
@DependsOn("CloudDatabaseConnection")
public class JavaMailExtension {
    
    @Autowired
    private Environment env;
    
    @Bean
    public JavaMailSenderImpl mailSender() {
        JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();

        javaMailSender.setProtocol("smtp");
        
        javaMailSender.setHost(env.getProperty("smtp.server"));
        javaMailSender.setPort(env.getProperty("smtp.port", Integer.class, 25));
        javaMailSender.setUsername(env.getProperty("smtp.username"));
        javaMailSender.setPassword(env.getProperty("smtp.password"));
        

        return javaMailSender;
    }
    
}
