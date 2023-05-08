/*
 *   gcloudlicensemanagement - AzureADFunctions.java
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

import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.options.Option;
import com.microsoft.graph.options.QueryOption;
import com.microsoft.graph.models.*;
import com.microsoft.graph.requests.*;
import com.slinkytoybox.gcloud.licensing.connection.AzureADConnection;
import jakarta.annotation.PostConstruct;
import java.util.LinkedList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

/**
 *
 * @author Michael Junek (michael@juneks.com.au)
 */
@Slf4j
@Component
@DependsOn("AzureADConnection")
public class AzureADFunctions {

    @Autowired
    private AzureADConnection adConn;

    private GraphServiceClient graphClient;

    @PostConstruct
    private void setup() {
        final String logPrefix = "setup() - ";
        log.trace("{}Entering Method", logPrefix);
        graphClient = adConn.getClient();
    }

    boolean addUserToGroup(String upn, String groupName) {
        final String logPrefix = "addUserToGroup() - ";
        log.trace("{}Entering Method", logPrefix);
        log.info("{}Adding {} to Azure Group {}", logPrefix, upn, groupName);
        return modifyGroup(upn, groupName, true);
    }

    private boolean modifyGroup(String upn, String groupName, boolean addUser) {
        final String logPrefix = "modifyGroup() - ";
        log.trace("{}Entering Method", logPrefix);
        log.info("{}AD Group change for {} - User {} ... Adding? {}", logPrefix, groupName, upn, addUser);

        LinkedList<Option> requestOptions = new LinkedList<>();
        requestOptions.add(new QueryOption("$filter", "displayName eq '" + groupName + "'"));

        log.debug("{}Sending off Azure group request", logPrefix);
        GroupCollectionPage groupsPage = graphClient.groups()
                .buildRequest(requestOptions)
                .get();

        if (groupsPage == null) {
            log.error("{}Could not find group {} in Azure", logPrefix, groupName);
            return false;
        }

        List<Group> groupList = groupsPage.getCurrentPage();
        log.debug("{}Got {} groups", logPrefix, groupList.size());

        if (groupList.size() != 1) {
            log.warn("{}A single group was not returned");
            return false;
        }

        User adUser = graphClient.users(upn).buildRequest().get();
        if (adUser == null || adUser.id == null) {
            log.error("{}User {} was not found in AzureAD", logPrefix, upn);
            return false;
        }

        Group adGroup = groupList.get(0);
        log.trace("{}Found GroupID: {} for GRoup Name {}", logPrefix, adGroup.id, adGroup.displayName);

        log.debug("{}Getting list of current members in group", logPrefix);
        UserCollectionPage ucp = graphClient.groups(adGroup.id).membersAsUser().buildRequest().get();
        boolean doOperation = false;

        if (ucp == null && !addUser) {
            log.warn("{}No users found in group. Nothing to do", logPrefix);
            return true;
        }
        else if (ucp == null && addUser) {
            log.debug("{}No users in group. Adding single user");
            doOperation = true;
        }
        else {
            log.trace("{}Iterating group membership", logPrefix);
            while (ucp != null && !ucp.getCurrentPage().isEmpty()) {
                List<User> users = ucp.getCurrentPage();
                log.trace("{}Found {} members total", logPrefix, users.size());

                for (User usr : users) {
                    log.trace("{}Found user: {} = {}", logPrefix, usr.id, usr.userPrincipalName);
                    if (addUser && usr.userPrincipalName.equals(upn)) {
                        log.info("{}User {} is already a member of group {} - doing nothing", logPrefix, upn, groupName);
                        return true;
                    }
                    if (!addUser && usr.userPrincipalName.equals(upn)) {
                        log.debug("{}Found user, ready to remove", logPrefix);
                        doOperation = true;
                        break;
                    }
                }
                if (doOperation || ucp.getNextPage() == null) {
                    break;
                }
                ucp = ucp.getNextPage().buildRequest().get();

            }
        }
        log.debug("{}Finished iterating group", logPrefix);

        if (!doOperation && !addUser) {
            log.warn("{}User not found in group and request was to remove. Nothing to do.", logPrefix);
            return true;
        }

        if (addUser) {
            log.debug("{}About to add user {} to group {}", logPrefix, upn, groupName);
            DirectoryObject dObj = new DirectoryObject();
            dObj.id = adUser.id;
            try {
                graphClient.groups(adGroup.id).members().references().buildRequest().post(dObj);
                log.info("{}Successfully added {}", logPrefix, upn);
                return true;
            }
            catch (ClientException ex) {
                log.error("{}Exception encountered adding user to group", logPrefix, ex);
                return false;
            }
        }
        else {
            log.debug("{}Deleting {} from group {}", logPrefix, adUser.id, adGroup.id);
            try {
                graphClient.groups(adGroup.id).members(adUser.id).reference().buildRequest().delete();
                log.info("{}Successfully removed {}", logPrefix, upn);
                return true;
            }
            catch (ClientException ex) {
                log.error("{}Exception encountered removing user from group", logPrefix, ex);
                return false;
            }
        }
    }

    boolean removeUserFromGroup(String upn, String groupName) {
        final String logPrefix = "removeUserFromGroup() - ";
        log.trace("{}Entering Method", logPrefix);
        log.info("{}Removing {} from Azure Group {}", logPrefix, upn, groupName);
        return modifyGroup(upn, groupName, false);
    }

}
