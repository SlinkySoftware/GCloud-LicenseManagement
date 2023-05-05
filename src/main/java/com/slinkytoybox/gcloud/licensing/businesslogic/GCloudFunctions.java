/*
 *   gcloudlicensemanagement - GCloudFunctions.java
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

import com.slinkytoybox.gcloud.licensing.connection.GCloudAPIConnection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Michael Junek (michael@juneks.com.au)
 */
@Component
@Slf4j
public class GCloudFunctions {
    
    @Autowired
    private GCloudAPIConnection cloudApi;
    
    
    String getAzureAdAccessGroup(Long cloudPlatformId) {
        final String logPrefix = "getCloudAzureAdControlGroup() - ";
        log.trace("{}Entering Method", logPrefix);
        if (cloudApi.getCloudPlatform(cloudPlatformId) == null) {
            log.error("{}Cloud Platform ID {} is not registered", logPrefix);
            throw new IllegalArgumentException("Cloud platform is not registered");
        }
        
        return cloudApi.getCloudPlatform(cloudPlatformId).getAzureAdAccessGroup();
        
    }
    
    
}
