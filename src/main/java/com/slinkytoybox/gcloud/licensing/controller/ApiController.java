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

import com.slinkytoybox.gcloud.licensing.dto.internal.LicenseDTO;
import com.slinkytoybox.gcloud.licensing.businesslogic.LicenseManagement;
import com.slinkytoybox.gcloud.licensing.businesslogic.LicenseManagement.ReturnReason;
import com.slinkytoybox.gcloud.licensing.dto.request.*;
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
        log.info("{}Processing POST for /myLicenses for {}", logPrefix, principal.getName());
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
                        .setExpired(lic.getExpiryDate().isBefore(LocalDateTime.now()));
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

    @PostMapping(path = "/modifyLicense", produces = "application/json", consumes = "application/json")
    public ResponseEntity<BooleanResponse> modifyLicense(Principal principal, @RequestBody UserLicenseRequest licenseRequest) {
        final String logPrefix = "modifyLicense() - ";
        log.trace("{}Entering Method", logPrefix);
        log.info("{}Processing POST for /modifyLicense for {} -> {}", logPrefix, principal.getName(), licenseRequest);
        BooleanResponse resp = new BooleanResponse();

        switch (licenseRequest.getRequestType()) {

            case CREATE -> {
                log.debug("{}Detected license creation request", logPrefix);
                if (licenseRequest.getCloudPlatformId() == null || licenseRequest.getCloudPlatformId() == 0) {
                    log.error("{}Cloud Platform ID must be supplied in request", logPrefix);
                    resp.setSuccess(false);
                    resp.setDetailedMessage("Cloud Platform ID must be supplied in request");
                    resp.setFriendlyMessage("License allocation request is invalid");
                    return ResponseEntity.badRequest().body(resp);
                }
                resp = licMgmt.createUserLicense(principal.getName(), licenseRequest.getCloudPlatformId());
            }

            case EXTEND -> {
                log.debug("{}Detected license extension request", logPrefix);
                if (licenseRequest.getLicenseId() == null || licenseRequest.getLicenseId() == 0) {
                    log.error("{}License ID must be supplied in request", logPrefix);
                    resp.setSuccess(false);
                    resp.setDetailedMessage("License ID must be supplied in request");
                    resp.setFriendlyMessage("License extension request is invalid");
                    return ResponseEntity.badRequest().body(resp);
                }
                resp = licMgmt.extendUserLicense(licenseRequest.getLicenseId());
            }

            case REVOKE -> {
                log.debug("{}Detected license return request", logPrefix);
                if (licenseRequest.getLicenseId() == null || licenseRequest.getLicenseId() == 0) {
                    log.error("{}License ID must be supplied in request", logPrefix);
                    resp.setSuccess(false);
                    resp.setDetailedMessage("License ID must be supplied in request");
                    resp.setFriendlyMessage("License return request is invalid");
                    return ResponseEntity.badRequest().body(resp);
                }
                resp = licMgmt.returnUserLicense(licenseRequest.getLicenseId(), ReturnReason.AGENT_REQEUST);
            }

            default -> {
                log.error("{}Could not detect request type!", logPrefix);
                resp.setSuccess(false);
                resp.setDetailedMessage("Unknown request type: " + licenseRequest.getRequestType());
                resp.setFriendlyMessage("Unknown license request");
                return ResponseEntity.badRequest().body(resp);

            }
        }
        log.debug("{}Returning response: {}", logPrefix, resp);
        return ResponseEntity.ok().body(resp);
    }

}
