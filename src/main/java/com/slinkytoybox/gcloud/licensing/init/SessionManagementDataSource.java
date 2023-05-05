/*
 *   gcloudlicensemanagement - SessionManagementDataSource.java
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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 *
 * @author Michael Junek (michael@juneks.com.au)
 */
@Component
@Configuration
@Slf4j
public class SessionManagementDataSource {

    @Autowired
    private PlatformEncryption pe;

    @Autowired
    private Environment env;

    @Bean
    public DataSource getDataSource() {
       final String logPrefix = "getDataSource() - ";
        log.trace("{}Entering Method", logPrefix);
         return new HikariDataSource(hikariConfig());
    }

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "session-management.db")
    public HikariConfig hikariConfig() {
        final String logPrefix = "hikariConfig() - ";
        log.trace("{}Entering Method", logPrefix);
        HikariConfig hc = new HikariConfig();
        String encPw = env.getProperty("session-management.db.pass");
        String password = pe.decrypt(encPw);
        if (password == null) {
            log.error("{}Could not decrypt session management password", logPrefix);
            throw new IllegalArgumentException("Could not decrypt session management password");
        }
        hc.setPassword(password);
        return hc;
    }

}
