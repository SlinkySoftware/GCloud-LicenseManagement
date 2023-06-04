/*
 *   gcloudlicensemanagement - Scheduler.java
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

import com.slinkytoybox.gcloud.licensing.businesslogic.LicenseManagement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 *
 * @author Michael Junek (michael@juneks.com.au)
 */
@Component
@Slf4j
@DependsOn("CloudDatabaseConnection")

public class Scheduler {

    @Autowired
    private LicenseManagement licMgmt;

    @Autowired
    private Environment env;

    @Scheduled(fixedDelayString = "${expiry.repeat.seconds:120}000", initialDelayString = "${expiry.delay.seconds:30}000")
    public void runExpirySchedule() {
        final String logPrefix = "runExpirySchedule() - ";
        log.trace("{}Entering Method", logPrefix);

        boolean expiryEnabled = env.getProperty("expiry.enabled", Boolean.class, Boolean.TRUE);
        log.info("{}Expiry Enabled? {}", logPrefix, expiryEnabled);
        if (expiryEnabled) {
            log.info("{}Scheduler running expiry routine", logPrefix);
            licMgmt.expireOldLicenses();
        }
        log.trace("{}Leaving Method", logPrefix);
    }
}
