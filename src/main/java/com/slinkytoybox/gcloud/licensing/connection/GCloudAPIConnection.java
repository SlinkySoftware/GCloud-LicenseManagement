/*
 *   gcloudlicensing - GCloudAPIConnection.java
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

import com.mypurecloud.sdk.v2.*;
import com.mypurecloud.sdk.v2.extensions.AuthResponse;
import com.slinkytoybox.gcloud.licensing.genesys.CloudPlatform;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Locale;
import jakarta.annotation.PostConstruct;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 *
 * @author Michael Junek (michael@juneks.com.au)
 */
//@Component
@Slf4j
public class GCloudAPIConnection {

    private final Map<Long, CloudPlatform> cloudPlatforms = new HashMap<>();
    private static final String USER_AGENT = "GCloud-License-Management";
    private final CloudDatabaseConnection dbConn;

    @Value("${genesys.cloud.api-timeout:5000}")
    private Integer apiTimeout;

    private final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, Locale.ENGLISH);

    public GCloudAPIConnection(CloudDatabaseConnection dbConn) {
        final String logPrefix = "ctor() - ";
        log.trace("{}Entering Method", logPrefix);

        this.dbConn = dbConn;
    }

    @PostConstruct
    private void init() {
        final String logPrefix = "init() - ";
        log.trace("{}Entering Method", logPrefix);
        log.info("{}Initialisating Genesys Cloud connections", logPrefix);

        log.debug("{}Getting list of cloud plaforms from Database", logPrefix);

        String platformSql = "SELECT Id, Name, OrganisationName, OrganisationId, ApiRegion, ApiClientId, ApiClientSecret, AzureAdAccessGroup FROM COM_CLOUD_PLATFORM WHERE Enabled = 1";
        try (Connection dbConnection = dbConn.getDatabaseConnection()) {
            try (PreparedStatement ps = dbConnection.prepareStatement(platformSql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.last()) {
                        log.warn("{}No cloud platforms exist. Not starting any connections", logPrefix);
                    }
                    else {
                        rs.beforeFirst();
                        log.info("{}Finding Genesys cloud instances", logPrefix);
                        try {
                            while (rs.next()) {
                                CloudPlatform cp = new CloudPlatform()
                                        .setId(rs.getLong("Id"))
                                        .setName(rs.getString("Name"))
                                        .setOrganisationName(rs.getNString("OrganisationName"))
                                        .setOrganisationId(rs.getNString("OrganisationId"))
                                        .setApiRegion(rs.getNString("ApiRegion"))
                                        .setApiClientId(rs.getNString("ApiClientId"))
                                        .setApiClientSecret(rs.getNString("APIClientSecret"))
                                        .setAzureAdAccessGroup(rs.getNString("AzureAdAccessGroup"));
                                log.info("{}Got Cloud Platform {} - attempting to connect", logPrefix, cp);

                                cp = initCloudPlatform(cp);
                                cloudPlatforms.put(cp.getId(), cp);
                            }
                        }
                        catch (SQLException | IllegalArgumentException ex) {
                            log.error("{}Exception encountered processing cloud plaform. Moving to next", logPrefix, ex);
                        }
                    }
                    log.trace("{}Finisehd with ResultSet", logPrefix, rs);
                }
                log.trace("{}Finished with PreparedStatement", logPrefix, ps);
            }
            log.trace("{}Finished with DBConnection", logPrefix, dbConnection);
        }
        catch (SQLException ex) {
            log.error("{}SQL Exception encountered", logPrefix, ex);
        }

    }

    private CloudPlatform initCloudPlatform(CloudPlatform cp) throws SQLException {
        final String logPrefix = "initCloudPlatform() - ";
        log.trace("{}Entering Method", logPrefix);

        PureCloudRegionHosts region = null;
        ApiClient apiClient;

        if (cp.getApiClientId() == null || cp.getApiClientSecret() == null || cp.getApiRegion() == null) {
            throw new IllegalArgumentException("Genesys cloud configuration not specified in database");
        }
        try {
            region = PureCloudRegionHosts.valueOf(cp.getApiRegion());
        }
        catch (Exception ex) {
            log.error("{}Invalid region specifed: {}", logPrefix, cp.getApiRegion());
            throw new IllegalArgumentException("Invalid Genesys cloud region", ex);
        }
        log.info("{}Genesys Cloud '{}' -> Client ID: {} @ Region: {}", logPrefix, cp.getOrganisationName(), cp.getApiClientId(), cp.getApiRegion());
        log.debug("{}Building api connection", logPrefix);
        apiClient = ApiClient.Builder
                .standard()
                .withBasePath(region)
                .withUserAgent(USER_AGENT)
                .withShouldRefreshAccessToken(true)
                .withDateFormat(dateFormat)
                .withShouldThrowErrors(true)
                .withConnectionTimeout(apiTimeout)
                .build();

        log.debug("{}Authenticating to Genesys cloud", logPrefix);
        try {
            ApiResponse<AuthResponse> authResponse = apiClient.authorizeClientCredentials(cp.getApiClientId(), cp.getApiClientSecret());
            log.info("{}Client Authentication Response: {}", logPrefix, authResponse.getBody());
        }
        catch (ApiException | IOException ex) {
            throw new IllegalArgumentException("Exception authenticating", ex);
        }
        log.debug("{}Successfully authenticated", logPrefix);
        cp.setApiClient(apiClient);
        return cp;
        
    }

    public ApiClient getApiClient(Long platformId) {
        final String logPrefix = "getApiClient() - ";
        log.trace("{}Entering Method", logPrefix);
        CloudPlatform cp = cloudPlatforms.get(platformId);
        if (cp == null) {
            log.error("{}Cloud platform {} does not exist", logPrefix, platformId);
            return null;
        }
        return cp.getApiClient();
    }

    public CloudPlatform getCloudPlatform(Long platformId) {
        final String logPrefix = "getCloudPlatform() - ";
        log.trace("{}Entering Method", logPrefix);
        return cloudPlatforms.get(platformId);
    }

    public Map<Long, CloudPlatform> getAllPlatforms() {
        final String logPrefix = "getAllPlatforms() - ";
        log.trace("{}Entering Method", logPrefix);
        return cloudPlatforms;

    }
}
