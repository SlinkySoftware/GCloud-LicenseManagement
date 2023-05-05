/*
 *   gcloudlicensemanagement - LicenseManagement.java
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

import com.slinkytoybox.gcloud.licensing.connection.CloudDatabaseConnection;
import com.slinkytoybox.gcloud.licensing.dto.response.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Michael Junek (michael@juneks.com.au)
 */
@Component
@Slf4j
public class LicenseManagement {

    @Autowired
    private CloudDatabaseConnection cdc;

    // Check license available
    public Boolean isLicenseAvailable(String upn, Long cloudPlatformId) {
        return Boolean.TRUE;
    }

    // Create new license
    public BooleanResponse createUserLicense(String upn, Long cloudPlatformId) {
        final String logPrefix = "createUserLicense() - ";
        log.trace("{}Entering Method", logPrefix);
        if (upn == null || upn.isBlank()) {
            log.error("{}UPN cannot be null or empty", logPrefix);
            throw new IllegalArgumentException("UPN cannot be null or empty");
        }
        if (cloudPlatformId == null || cloudPlatformId == 0) {
            log.error("{}CloudPlatformId cannot be null or zero", logPrefix);
            throw new IllegalArgumentException("CloudPlatformId cannot be null or zero");
        }
        BooleanResponse response = new BooleanResponse();

        log.info("{}Creating new license for {} on platform {}", logPrefix, upn, cloudPlatformId);
        LicenseDTO license = new LicenseDTO()
                .setUpn(upn);

        return response;
    }

    // Get existing license from database
    public Map<Long, LicenseDTO> getUserLicenses(String upn) {
        final String logPrefix = "getUserLicenses() - ";
        log.trace("{}Entering Method", logPrefix);

        if (upn == null || upn.isBlank()) {
            log.error("{}UPN cannot be null or empty", logPrefix);
            throw new IllegalArgumentException("UPN cannot be null or empty");
        }

        Map<Long, LicenseDTO> licenses = new HashMap<>();

        log.info("{}Looking up existing licenses for {}", logPrefix, upn);
        String platformSql = "SELECT LIC.Id, LIC.LicenseIssueDateTime, LIC.LicenseExpiryDateTime, U.UPN, LIC.CloudPlatformID FROM LIC_ISSUED_LICENSE LIC"
                + " INNER JOIN PROV_USER U ON U.Id = LIC.UserId"
                + " WHERE U.UPN = ?";
        try (Connection dbConnection = cdc.getDatabaseConnection()) {
            try (PreparedStatement ps = dbConnection.prepareStatement(platformSql)) {
                ps.setNString(1, upn);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        LicenseDTO dto = new LicenseDTO()
                                .setId(rs.getLong("Id"))
                                .setCloudPlatformId(rs.getLong("CloudPlatformId"))
                                .setExpiryDate(rs.getTimestamp("LicenseExpiryDateTime").toLocalDateTime())
                                .setIssueDate(rs.getTimestamp("LicenseIssueDateTime").toLocalDateTime())
                                .setUpn(upn);

                        log.debug("{}Got a license {}", logPrefix, dto);
                        licenses.put(rs.getLong("CloudPlatformId"), dto);
                    }
                }
            }
        }
        catch (SQLException ex) {
            log.error("{}SQL Exception encountered", logPrefix, ex);
        }
        log.info("{}Found a total of {} issued licenses for user {}", logPrefix, licenses.size(), upn);
        return licenses;

    }

    // Extend license
    public BooleanResponse extendUserLicense(Long licenseId) {
        final String logPrefix = "extendUserLicense() - ";
        log.trace("{}Entering Method", logPrefix);
        if (licenseId == null || licenseId == 0) {
            log.error("{}licenseId cannot be null or zero", logPrefix);
            throw new IllegalArgumentException("licenseId cannot be null or zero");
        }
        BooleanResponse response = new BooleanResponse();

        log.info("{}Extending license {}", logPrefix, licenseId);

        return response;
    }

    // Return License
    public BooleanResponse returnUserLicense(Long licenseId) {
        final String logPrefix = "returnUserLicense() - ";
        log.trace("{}Entering Method", logPrefix);
        if (licenseId == null || licenseId == 0) {
            log.error("{}licenseId cannot be null or zero", logPrefix);
            throw new IllegalArgumentException("licenseId cannot be null or zero");
        }
        BooleanResponse response = new BooleanResponse();

        log.info("{}Returning license {}", logPrefix, licenseId);

        return response;
    }

    // Get User Platforms
    public List<PlatformDTO> getUserPlatforms(String upn) {
        final String logPrefix = "getUserPlatforms() - ";
        log.trace("{}Entering Method", logPrefix);

        if (upn == null || upn.isBlank()) {
            log.error("{}UPN cannot be null or empty", logPrefix);
            throw new IllegalArgumentException("UPN cannot be null or empty");
        }

        List<PlatformDTO> platforms = new ArrayList<>();

        log.info("{}Looking up available platforms for {}", logPrefix, upn);
        String platformSql = "SELECT CP.Id, CP.Name, CP.OrganisationName, CP.OrganisationId FROM COM_CLOUD_PLATFORM CP"
                + " INNER JOIN PROV_USER_TYPE UT ON CP.Id = UT.CloudPlatformId "
                + " INNER JOIN PROV_MAP_USER_TO_USER_TYPE UTM ON UT.Id = UTM.UserTypeId AND UTM.CloudPlatformId = CP.Id"
                + " INNER JOIN PROV_USER U ON U.Id = UTM.UserId"
                + " WHERE U.UPN = ? AND CP.Enabled=1";
        try (Connection dbConnection = cdc.getDatabaseConnection()) {
            try (PreparedStatement ps = dbConnection.prepareStatement(platformSql)) {
                ps.setNString(1, upn);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        PlatformDTO dto = new PlatformDTO()
                                .setId(rs.getLong("Id"))
                                .setName(rs.getString("Name"))
                                .setOrganisationName(rs.getNString("OrganisationName"))
                                .setOrganisationId(rs.getNString("OrganisationId"));
                        log.debug("{}Got available platform {}", logPrefix, dto);
                        platforms.add(dto);
                    }
                }
            }
        }
        catch (SQLException ex) {
            log.error("{}SQL Exception encountered", logPrefix, ex);
        }
        log.info("{}Found a total of {} platforms for user {}", logPrefix, platforms.size(), upn);
        return platforms;
    }

}
