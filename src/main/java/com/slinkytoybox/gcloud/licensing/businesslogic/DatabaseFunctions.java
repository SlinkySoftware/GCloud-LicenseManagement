/*
 *   gcloudlicensemanagement - DatabaseFunctions.java
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

import com.slinkytoybox.gcloud.licensing.businesslogic.LicenseManagement.ReturnReason;
import com.slinkytoybox.gcloud.licensing.connection.CloudDatabaseConnection;
import com.slinkytoybox.gcloud.licensing.dto.internal.LicenseDTO;
import com.slinkytoybox.gcloud.licensing.dto.response.PlatformDTO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
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
@Slf4j
@Component

public class DatabaseFunctions {

    @Autowired
    private CloudDatabaseConnection cdc;

    Map<Long, LicenseDTO> getUserLicensesFromDB(String upn) {
        final String logPrefix = "getUserLicensesFromDB() - ";
        log.trace("{}Entering Method", logPrefix);
        Map<Long, LicenseDTO> licenses = new HashMap<>();

        log.info("{}Looking up existing licenses for {}", logPrefix, upn);
        String sql = "SELECT LIC.Id, LIC.LicenseIssueDateTime, LIC.LicenseExpiryDateTime, U.UPN, LIC.CloudPlatformID, LIC.LicenseGroupId, U.ID USERID FROM LIC_ISSUED_LICENSE LIC\n"
                + " INNER JOIN PROV_USER U ON U.Id = LIC.UserId\n"
                + " WHERE U.UPN = ?";
        try (Connection dbConnection = cdc.getDatabaseConnection()) {
            try (PreparedStatement ps = dbConnection.prepareStatement(sql)) {
                ps.setNString(1, upn);
                traceLogSql(logPrefix, sql, upn);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        LicenseDTO dto = new LicenseDTO()
                                .setId(rs.getLong("Id"))
                                .setCloudPlatformId(rs.getLong("CloudPlatformId"))
                                .setExpiryDate(rs.getTimestamp("LicenseExpiryDateTime").toLocalDateTime())
                                .setIssueDate(rs.getTimestamp("LicenseIssueDateTime").toLocalDateTime())
                                .setLicenseGroupId(rs.getLong("LicenseGroupId"))
                                .setUserId(rs.getLong("USERID"))
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
        return licenses;
    }

    List<PlatformDTO> getPlatformsForUserFromDB(String upn) {
        final String logPrefix = "getPlatformsForUserFromDB() - ";
        log.trace("{}Entering Method", logPrefix);
        List<PlatformDTO> platforms = new ArrayList<>();

        log.info("{}Looking up available platforms for {}", logPrefix, upn);
        String sql = "SELECT CP.Id, CP.Name, CP.OrganisationName, CP.OrganisationId FROM COM_CLOUD_PLATFORM CP\n"
                + " INNER JOIN PROV_USER_TYPE UT ON CP.Id = UT.CloudPlatformId \n"
                + " INNER JOIN PROV_MAP_USER_TO_USER_TYPE UTM ON UT.Id = UTM.UserTypeId AND UTM.CloudPlatformId = CP.Id\n"
                + " INNER JOIN PROV_USER U ON U.Id = UTM.UserId\n"
                + " WHERE U.UPN = ? AND CP.Enabled=1";
        try (Connection dbConnection = cdc.getDatabaseConnection()) {
            try (PreparedStatement ps = dbConnection.prepareStatement(sql)) {
                ps.setNString(1, upn);
                traceLogSql(logPrefix, sql, upn);
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
        return platforms;
    }

    Map<Long, Long> getCurrentLicenseUsage() {
        final String logPrefix = "getCurrentLicenseUsage() - ";
        log.trace("{}Entering Method", logPrefix);

        log.info("{}Getting in-use license counts", logPrefix);
        String sql = "SELECT LIC_LICENSE_GROUP.ID, LIC_LICENSE_GROUP.NAME, COUNT(LIC_ISSUED_LICENSE.ID) InUse\n"
                + " FROM LIC_LICENSE_GROUP LEFT JOIN LIC_ISSUED_LICENSE ON LIC_LICENSE_GROUP.ID = LIC_ISSUED_LICENSE.LicenseGroupId\n"
                + " GROUP BY LIC_LICENSE_GROUP.ID,LIC_LICENSE_GROUP.Name ORDER BY LIC_LICENSE_GROUP.ID";

        Map<Long, Long> licenseCountMap = new HashMap<>();

        try (Connection dbConnection = cdc.getDatabaseConnection()) {
            try (PreparedStatement ps = dbConnection.prepareStatement(sql)) {
                traceLogSql(logPrefix, sql);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Long groupId = rs.getLong("ID");
                        Long licenseCount = rs.getLong("InUse");
                        String groupName = rs.getString("Name");
                        log.debug("{}Group {} ({}) has {} licenses in use", logPrefix, groupId, groupName, licenseCount);
                        licenseCountMap.put(groupId, licenseCount);
                    }
                }
            }
        }
        catch (SQLException ex) {
            log.error("{}SQL Exception encountered", logPrefix, ex);
        }
        log.trace("{}License Count Map: {}", logPrefix, licenseCountMap);
        return licenseCountMap;
    }

    Map<String, Object> getUserLicenseGroupDetails(String upn, Long cloudPlatformId) {
        final String logPrefix = "getUserLicenseGroupDetails() - ";
        log.trace("{}Entering Method", logPrefix);

        log.info("{}Getting in-use license counts", logPrefix);
        String sql = "SELECT U.Id USERID, U.FullName USERFULLNAME, UT.NAME USERTYPENAME, LG.Name LicenseGroupName, LG.Id LicenseGroupId,\n"
                + "LG.SoftLimit, LG.HardLimit, LG.DefaultIssueSeconds, LG.ExtensionTimeSeconds FROM PROV_USER U \n"
                + "INNER JOIN PROV_MAP_USER_TO_USER_TYPE UTM ON U.ID = UTM.UserId\n"
                + "INNER JOIN PROV_USER_TYPE UT ON UTM.UserTypeId = UT.ID AND UTM.CloudPlatformId = UT.CloudPlatformId\n"
                + "INNER JOIN LIC_LICENSE_GROUP LG ON UT.LicenseGroupId = LG.ID\n"
                + "WHERE U.UPN = ? AND UT.CloudPlatformId = ?";

        Map<String, Object> licenseGroupMap = new HashMap<>();

        try (Connection dbConnection = cdc.getDatabaseConnection()) {
            try (PreparedStatement ps = dbConnection.prepareStatement(sql)) {
                ps.setNString(1, upn);
                ps.setLong(2, cloudPlatformId);
                traceLogSql(logPrefix, sql, upn, cloudPlatformId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        licenseGroupMap.put("USERID", rs.getLong("USERID"));
                        licenseGroupMap.put("LICENSEGROUPID", rs.getLong("LICENSEGROUPID"));
                        licenseGroupMap.put("SOFTLIMIT", rs.getLong("SOFTLIMIT"));
                        licenseGroupMap.put("HARDLIMIT", rs.getLong("HARDLIMIT"));
                        licenseGroupMap.put("DEFAULTISSUESECONDS", rs.getLong("DEFAULTISSUESECONDS"));
                        licenseGroupMap.put("EXTENSIONTIMESECONDS", rs.getLong("EXTENSIONTIMESECONDS"));
                        licenseGroupMap.put("LICENSEGROUPNAME", rs.getNString("LICENSEGROUPNAME"));
                        licenseGroupMap.put("USERFULLNAME", rs.getNString("USERFULLNAME"));
                        licenseGroupMap.put("USERTYPENAME", rs.getNString("USERTYPENAME"));

                    }
                    else {
                        log.error("{}User {} is not allocated to Cloud Platform {}", logPrefix, upn, cloudPlatformId);
                        return null;
                    }
                }
            }
        }
        catch (SQLException ex) {
            log.error("{}SQL Exception encountered", logPrefix, ex);
            return null;
        }
        log.trace("{}License Group Map: {}", logPrefix, licenseGroupMap);
        return licenseGroupMap;
    }

    boolean writeLicenseToDatabase(Long userId, Long licenseGroupId, Long cloudPlatformId, LocalDateTime expiryDateTime) {

        final String logPrefix = "writeLicenseToDatabase() - ";
        log.trace("{}Entering Method", logPrefix);
        log.info("{}Writing license to database", logPrefix);
        String sql = "INSERT INTO LIC_ISSUED_LICENSE\n "
                + "  (ID, USERID, LICENSEGROUPID, LICENSEISSUEDATETIME, LICENSEEXPIRYDATETIME, CLOUDPLATFORMID)\n"
                + " VALUES\n"
                + "  (NEXT VALUE FOR SEQ_LIC_ISSUED_LICENSE, ?, ?, GETDATE(), ?, ?)\n";

        try (Connection dbConnection = cdc.getDatabaseConnection()) {
            dbConnection.setAutoCommit(false);
            try (PreparedStatement ps = dbConnection.prepareStatement(sql)) {
                ps.setLong(1, userId);
                ps.setLong(2, licenseGroupId);
                ps.setTimestamp(3, Timestamp.valueOf(expiryDateTime));
                ps.setLong(4, cloudPlatformId);
                traceLogSql(logPrefix, sql, userId, licenseGroupId, expiryDateTime, cloudPlatformId);
                int rows = ps.executeUpdate();
                if (rows == 1) {
                    log.debug("{}Successfully wrote {} rows to the database", logPrefix, rows);
                    dbConnection.commit();
                    return true;
                }
                else {
                    log.error("{}A total of {} rows were written. This should have been 1.", logPrefix, rows);
                    dbConnection.rollback();
                    return false;
                }
            }
        }
        catch (SQLException ex) {
            log.error("{}SQL Exception encountered", logPrefix, ex);
            return false;
        }
    }

    boolean extendLicense(Long licenseId, LocalDateTime expiryDateTime) {

        final String logPrefix = "extendLicense() - ";
        log.trace("{}Entering Method", logPrefix);
        log.info("{}Extending license in database", logPrefix);
        String sql = "UPDATE LIC_ISSUED_LICENSE \n"
                + "  SET LICENSEEXPIRYDATETIME=?\n"
                + " WHERE ID = ?";

        try (Connection dbConnection = cdc.getDatabaseConnection()) {
            dbConnection.setAutoCommit(false);
            try (PreparedStatement ps = dbConnection.prepareStatement(sql)) {
                ps.setTimestamp(1, Timestamp.valueOf(expiryDateTime));
                ps.setLong(2, licenseId);
                traceLogSql(logPrefix, sql, expiryDateTime, licenseId);
                int rows = ps.executeUpdate();
                if (rows == 1) {
                    log.debug("{}Successfully updated {} rows in the database", logPrefix, rows);
                    dbConnection.commit();
                    return true;
                }
                else {
                    log.error("{}A total of {} rows were updated. This should have been 1.", logPrefix, rows);
                    dbConnection.rollback();
                    return false;
                }
            }
        }
        catch (SQLException ex) {
            log.error("{}SQL Exception encountered", logPrefix, ex);
            return false;
        }
    }

    boolean deleteLicense(Long licenseId) {

        final String logPrefix = "deleteLicense() - ";
        log.trace("{}Entering Method", logPrefix);
        log.info("{}Deleting license from database", logPrefix);
        String sql = "DELETE FROM LIC_ISSUED_LICENSE WHERE ID = ?";

        try (Connection dbConnection = cdc.getDatabaseConnection()) {
            dbConnection.setAutoCommit(false);
            try (PreparedStatement ps = dbConnection.prepareStatement(sql)) {
                ps.setLong(1, licenseId);
                traceLogSql(logPrefix, sql, licenseId);

                int rows = ps.executeUpdate();
                if (rows == 1) {
                    log.debug("{}Successfully deleted {} rows in the database", logPrefix, rows);
                    dbConnection.commit();
                    return true;
                }
                else {
                    log.error("{}A total of {} rows were deleted. This should have been 1.", logPrefix, rows);
                    dbConnection.rollback();
                    return false;
                }
            }
        }
        catch (SQLException ex) {
            log.error("{}SQL Exception encountered", logPrefix, ex);
            return false;
        }
    }

    LicenseDTO getLicenseFromDB(Long licenseId) {
        final String logPrefix = "getLicenseFromDB() - ";
        log.trace("{}Entering Method", logPrefix);

        log.info("{}Looking up license {}", logPrefix, licenseId);
        String sql = "SELECT LIC.Id, LIC.LicenseIssueDateTime, LIC.LicenseExpiryDateTime, U.UPN, LIC.CloudPlatformID,\n"
                + " LIC.LicenseGroupId, U.ID USERID FROM LIC_ISSUED_LICENSE LIC\n"
                + " INNER JOIN PROV_USER U ON U.Id = LIC.UserId\n"
                + " WHERE LIC.Id = ?";
        try (Connection dbConnection = cdc.getDatabaseConnection()) {
            try (PreparedStatement ps = dbConnection.prepareStatement(sql)) {
                ps.setLong(1, licenseId);
                traceLogSql(logPrefix, sql, licenseId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        LicenseDTO dto = new LicenseDTO()
                                .setId(rs.getLong("Id"))
                                .setCloudPlatformId(rs.getLong("CloudPlatformId"))
                                .setExpiryDate(rs.getTimestamp("LicenseExpiryDateTime").toLocalDateTime())
                                .setIssueDate(rs.getTimestamp("LicenseIssueDateTime").toLocalDateTime())
                                .setLicenseGroupId(rs.getLong("LicenseGroupId"))
                                .setUserId(rs.getLong("USERID"))
                                .setUpn(rs.getNString("UPN"));

                        log.debug("{}Got a license {}", logPrefix, dto);
                        return dto;
                    }
                }
            }
        }
        catch (SQLException ex) {
            log.error("{}SQL Exception encountered", logPrefix, ex);
        }
        return null;
    }

    boolean writeLicenseHistoryRecord(Long licenseId, ReturnReason reason) {
        final String logPrefix = "writeLicenseHistoryRecord() - ";
        log.trace("{}Entering Method", logPrefix);
        log.info("{}Writing license history record", logPrefix);
        LicenseDTO lic = getLicenseFromDB(licenseId);
        if (lic == null) {
            log.warn("{}License ID {} not found. Not writing history record", logPrefix, licenseId);
            return false;
        }

        String sql = "INSERT INTO LIC_LICENSE_HISTORY (ID, USERID, LICENSEGROUPID, CLOUDPLATFORMID, ISSUEDATETIME, RETURNDATETIME, RETURNREASON) "
                + "VALUES (NEXT VALUE FOR SEQ_LIC_LICENSE_HISTORY, ?, ?, ?, ?, ?, ?)";
        try (Connection dbConnection = cdc.getDatabaseConnection()) {
            dbConnection.setAutoCommit(false);
            try (PreparedStatement ps = dbConnection.prepareStatement(sql)) {
                ps.setLong(1, lic.getUserId());
                ps.setLong(2, lic.getLicenseGroupId());
                ps.setLong(3, lic.getCloudPlatformId());
                ps.setTimestamp(4, Timestamp.valueOf(lic.getIssueDate()));
                ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
                ps.setNString(6, reason.name());
                traceLogSql(logPrefix, sql, licenseId, lic.getLicenseGroupId(), lic.getCloudPlatformId(), lic.getIssueDate(), LocalDateTime.now(), reason.name());
                int rows = ps.executeUpdate();
                if (rows == 1) {
                    log.debug("{}Successfully added {} history rows to the database", logPrefix, rows);
                    dbConnection.commit();
                    return true;
                }
                else {
                    log.error("{}A total of {} rows were added. This should have been 1.", logPrefix, rows);
                    dbConnection.rollback();
                    return false;
                }
            }
        }
        catch (SQLException ex) {
            log.error("{}SQL Exception encountered", logPrefix, ex);
        }

        return false;
    }

    List<LicenseDTO> getExpiredLicensesFromDB() {
        final String logPrefix = "getExpiredLicensesFromDB() - ";
        log.trace("{}Entering Method", logPrefix);

        log.info("{}Looking up all expired licenses", logPrefix);
        List<LicenseDTO> expiredLicenses = new ArrayList<>();
        String sql = "SELECT LIC.Id, LIC.LicenseIssueDateTime, LIC.LicenseExpiryDateTime, U.UPN, LIC.CloudPlatformID,\n"
                + " LIC.LicenseGroupId, U.ID USERID FROM LIC_ISSUED_LICENSE LIC\n"
                + " INNER JOIN PROV_USER U ON U.Id = LIC.UserId\n"
                + " WHERE LIC.LicenseExpiryDateTime < GETDATE()\n"
                + " ORDER BY LIC.LicenseExpiryDateTime";
        try (Connection dbConnection = cdc.getDatabaseConnection()) {
            try (PreparedStatement ps = dbConnection.prepareStatement(sql)) {
                traceLogSql(logPrefix, sql);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        LicenseDTO dto = new LicenseDTO()
                                .setId(rs.getLong("Id"))
                                .setCloudPlatformId(rs.getLong("CloudPlatformId"))
                                .setExpiryDate(rs.getTimestamp("LicenseExpiryDateTime").toLocalDateTime())
                                .setIssueDate(rs.getTimestamp("LicenseIssueDateTime").toLocalDateTime())
                                .setLicenseGroupId(rs.getLong("LicenseGroupId"))
                                .setUserId(rs.getLong("USERID"))
                                .setUpn(rs.getNString("UPN"));
                        expiredLicenses.add(dto);
                    }
                }
            }
        }
        catch (SQLException ex) {
            log.error("{}SQL Exception encountered", logPrefix, ex);
        }
        log.info("{}Found {} expired licenses in databsae", logPrefix, expiredLicenses.size());
        return expiredLicenses;
    }

    private void traceLogSql(String logPrefix, String sql, Object... parameters) {
        int parameterId = 0;
        String parameterList = "";
        for (Object parameter : parameters) {
            parameterId++;
            parameterList += " [" + parameterId + "] : [" + parameter.toString() + "]\n";
        }
        log.trace("{}SQL Log:\n[{}]\n{}", logPrefix, sql, parameterList);

    }

}
