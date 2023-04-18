/*
 *   gcloudlicensemanagement - ApiController.java
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
package com.slinkytoybox.gcloud.licensing.controller;

import com.slinkytoybox.gcloud.licensing.businesslogic.LicenseManagement;
import com.slinkytoybox.gcloud.licensing.dto.response.*;
import com.slinkytoybox.gcloud.licensing.security.roles.RoleUser;
import java.security.Principal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Michael Junek (michael@juneks.com.au)
 */
@RestController
@Slf4j
@RequestMapping(path = "/api/v1")
@RoleUser
public class ApiController {

    @Autowired
    private LicenseManagement licMgmt;

    @GetMapping("/myLicenses")
    public ResponseEntity<UserLicenseResponse> getMyLicenses(Principal principal) {
        final String logPrefix = "getMyLicenses() - ";
        log.trace("{}Entering Method", logPrefix);
        log.info("{}Processing GET for /myLicenses for {}", logPrefix, principal.getName());
        UserLicenseResponse response = new UserLicenseResponse();
        response.setAvailablePlatforms(licMgmt.getUserPlatforms(principal.getName()));
        response.setCurrentLicenses(licMgmt.getUserLicenses(principal.getName()));
        return ResponseEntity.ok().body(response);

    }

}
