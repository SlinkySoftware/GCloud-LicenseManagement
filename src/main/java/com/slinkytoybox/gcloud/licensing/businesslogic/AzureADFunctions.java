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

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 *
 * @author Michael Junek (michael@juneks.com.au)
 */
@Slf4j
@Component
public class AzureADFunctions {

    boolean addUserToGroup(String upn, String groupName) {
        final String logPrefix = "addUserToGroup() - ";
        log.trace("{}Entering Method", logPrefix);
        log.info("{}Adding {} to Azure Group {}", logPrefix, upn, groupName);
//TODO: Implement this
        return true;
    }

    boolean removeUserFromGroup(String upn, String groupName) {
        final String logPrefix = "addUserToGroup() - ";
        log.trace("{}Entering Method", logPrefix);
        log.info("{}Removing {} from Azure Group {}", logPrefix, upn, groupName);
//TODO: Implement this
        return true;
    }

}
