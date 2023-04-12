/*
 *   gcloudlicensing - APIConnection.java
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
package com.slinkytoybox.gcloud.licensing.genesysapi;

import com.mypurecloud.sdk.v2.ApiClient;
import com.mypurecloud.sdk.v2.ApiException;
import com.mypurecloud.sdk.v2.ApiResponse;
import com.mypurecloud.sdk.v2.Configuration;
import com.mypurecloud.sdk.v2.PureCloudRegionHosts;
import com.mypurecloud.sdk.v2.extensions.AuthResponse;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Locale;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 *
 * @author Michael Junek (michael@juneks.com.au)
 */
@Component
@Slf4j
public class APIConnection {
    
    private String clientSecret = "";
    private String clientId = "";
    private PureCloudRegionHosts region = null;
    private ApiClient apiClient;
    
    private final static String USER_AGENT = "GCloud-License-Management";
    
    
    @Autowired
    private Environment env;
    
    @PostConstruct
    private void init () {
        final String logPrefix = "init() - ";
        log.trace("{}Entering Method", logPrefix);
        
        
        log.debug("{}Getting Genesys cloud configuration options", logPrefix);
        clientId = env.getProperty("genesys.cloud.client-id");
        clientSecret = env.getProperty("genesys.cloud.client-secret");
        String regionString = env.getProperty("genesys.cloud.region");
        
        if (clientId == null || clientSecret == null || regionString == null) {
            throw new IllegalArgumentException ("Genesys cloud configuration not specified");
        }
        
        try {
            region = PureCloudRegionHosts.valueOf(regionString);
        }
        catch (Exception ex) {
            throw new IllegalArgumentException ("Invalid Genesys cloud region",ex);
        }
               
        log.info("{}Genesys Cloud Client ID: {} | Region: {}", logPrefix, clientId, region);
        
        Integer connectTimeout;
        try {
             connectTimeout = Integer.valueOf(env.getProperty("genesys.cloud.timeout", "5000"));
        }
        catch (NumberFormatException ex) {
            log.warn("{}Could not convert connection timeout to integer, setting to 5 seconds", logPrefix);
            connectTimeout = 3000;
        }
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.LONG,DateFormat.LONG, Locale.ENGLISH);

        
        log.debug("{}Building api connection", logPrefix);
        apiClient = ApiClient.Builder
                .standard()
                .withBasePath(region)
                .withUserAgent(USER_AGENT)
                .withShouldRefreshAccessToken(true)
                .withDateFormat(df)
                .withShouldThrowErrors(true)
                .withConnectionTimeout(connectTimeout)
                .build();

        log.info("{}Authenticating to Genesys cloud", logPrefix);
        try {
            ApiResponse<AuthResponse> authResponse = apiClient.authorizeClientCredentials(clientId, clientSecret);
        log.info("{}Client Authentication Response: {}", logPrefix, authResponse.getBody());
        }
        catch (ApiException | IOException ex) {
            throw new IllegalArgumentException("Exception authenticating", ex);
        }

        Configuration.setDefaultApiClient(apiClient);
        
        
        log.trace("{}Leaving Method", logPrefix);
    }
    
    public ApiClient getApiClient() {
        final String logPrefix = "getApiClient() - ";
        log.trace("{}Entering Method", logPrefix);
        return apiClient;
    }
    
}
