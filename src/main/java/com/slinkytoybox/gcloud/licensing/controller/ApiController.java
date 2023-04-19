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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @Value("${license.extend-time:7200}")
    private Long canExtendTime;

    @PostMapping(path = "/myLicenses", produces = "application/json")
    public ResponseEntity<UserLicenseResponse> getMyLicenses(Principal principal) {
        final String logPrefix = "getMyLicenses() - ";
        log.trace("{}Entering Method", logPrefix);
        log.info("{}Processing GET for /myLicenses for {}", logPrefix, principal.getName());
        UserLicenseResponse response = new UserLicenseResponse();
        List<PlatformDTO> platforms = licMgmt.getUserPlatforms(principal.getName());
        Map<Long, LicenseDTO> licenses = licMgmt.getUserLicenses(principal.getName());

        List<UserLicenseResponse.LicenseResponse> licResp = new ArrayList<>();

        for (PlatformDTO plat : platforms) {
            UserLicenseResponse.LicenseResponse row = new UserLicenseResponse.LicenseResponse()
                    .setCloudPlatformId(plat.getId())
                    .setPlatformName(plat.getName())
                    .setOrganisationName(plat.getOrganisationName())
                    .setOrganisationId(plat.getOrganisationId());
            if (licenses.containsKey(plat.getId())) {
                LicenseDTO lic = licenses.get(plat.getId());
                row.setLicenseAllocated(true)
                        .setExpiryDate(lic.getExpiryDate())
                        .setIssueDate(lic.getIssueDate())
                        .setUpn(lic.getUpn())
                        .setLicenseId(lic.getId())
                        .setCanExtend(lic.getExpiryDate().isBefore(LocalDateTime.now().plusSeconds(canExtendTime)))
                        .setExpired(lic.getExpiryDate().isBefore(LocalDateTime.now()))
                        ;
            }
            else {
                row.setLicenseAllocated(false)
                        .setExpiryDate(null)
                        .setIssueDate(null)
                        .setUpn(null)
                        .setLicenseId(null);

            }
            licResp.add(row);
            log.trace("{}Added license row: {}", logPrefix, row);
        }
        Collections.sort(licResp);
        response.setLicenseResponse(licResp);
        return ResponseEntity.ok().body(response);

    }

}
