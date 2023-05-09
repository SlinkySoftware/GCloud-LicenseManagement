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

import com.mypurecloud.sdk.v2.ApiException;
import com.mypurecloud.sdk.v2.api.TokensApi;
import com.mypurecloud.sdk.v2.api.UsersApi;
import com.mypurecloud.sdk.v2.model.User;
import com.mypurecloud.sdk.v2.model.UserSearchCriteria;
import com.mypurecloud.sdk.v2.model.UserSearchRequest;
import com.mypurecloud.sdk.v2.model.UsersSearchResponse;
import com.slinkytoybox.gcloud.licensing.connection.GCloudAPIConnection;
import com.slinkytoybox.gcloud.licensing.genesys.CloudPlatform;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

    boolean forceLogOutUser(String upn, Long cloudPlatformId) {
        final String logPrefix = "forceLogOutUser() - ";
        log.trace("{}Entering Method", logPrefix);
        log.info("{}Force logging out {} from platform {}", logPrefix, upn, cloudPlatformId);
        CloudPlatform cp = cloudApi.getCloudPlatform(cloudPlatformId);

        log.debug("{}Creating API Clients", logPrefix);
        UsersApi userApi = new UsersApi(cp.getApiClient());
        TokensApi tokenApi = new TokensApi(cp.getApiClient());
        log.debug("{}Creating search request", logPrefix);
        UserSearchRequest userSearch = new UserSearchRequest();
        List<UserSearchCriteria> criteriaList = new ArrayList<>();
        UserSearchCriteria usernameCriteria = new UserSearchCriteria();
        List<String> fields = new ArrayList<>();
        fields.add("username");
        usernameCriteria.setFields(fields);
        usernameCriteria.setValue(upn);
        usernameCriteria.setType(UserSearchCriteria.TypeEnum.EXACT);
        criteriaList.add(usernameCriteria);
        userSearch.setQuery(criteriaList);
        log.debug("{}Query formed: {}", logPrefix, userSearch);

        log.debug("{}About to send off API Search Requst for user");
        UsersSearchResponse userResult;
        try {
            userResult = userApi.postUsersSearch(userSearch);
        }
        catch (ApiException | IOException ex) {
            log.error("{}Exception encountered searching for Genesys Cloud user", logPrefix, ex);
            return false;
        }
        if (userResult.getTotal() != 1) {
            log.error("{}Search found () users. Expected only one.", logPrefix, userResult.getTotal());
            return false;
        }
        log.trace("{}Getting user result list and first/only entry", logPrefix);
        List<User> resultList = userResult.getResults();
        User foundUser = resultList.get(0);
        log.debug("{}UPN Search {} found Genesys user ID {}", logPrefix, upn, foundUser.getId());

        log.info("{}Deleting all authentication tokens for user {} ({})", logPrefix, upn, foundUser.getId());
        try {
            tokenApi.deleteToken(foundUser.getId());
        }
        catch (ApiException | IOException ex) {
            log.error("{}Exception encountered removing all tokens", logPrefix, ex);
            return false;
        }

        return true;

    }

}
