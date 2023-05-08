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

import com.slinkytoybox.gcloud.licensing.dto.internal.AlertMessage;
import com.slinkytoybox.gcloud.licensing.dto.internal.LicenseDTO;
import com.slinkytoybox.gcloud.licensing.dto.response.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
    private DatabaseFunctions dbFunc;

    @Autowired
    private AzureADFunctions adFunc;

    @Autowired
    private GCloudFunctions cloudFunc;

    @Autowired
    private AlertFunctions alertFunc;

    // Check license available
    public Boolean isLicenseAvailable(String upn, Long cloudPlatformId) {
        final String logPrefix = "isLicenseAvailable() - ";
        log.trace("{}Entering Method", logPrefix);
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

        // look up license group for user
        Map<String, Object> licenseGroupDetails = dbFunc.getUserLicenseGroupDetails(upn, cloudPlatformId);
        if (licenseGroupDetails == null) {
            response.setFriendlyMessage("Your account is not configured correctly. Please contact your team leader.");
            response.setDetailedMessage("Unable to retrieve license group details");
            response.setSuccess(false);
            AlertMessage am = new AlertMessage()
                    .setSubject("GCloud Licensing - User not configured")
                    .setMessage("User: " + upn + " is not configured and could not check out a license")
                    .setSource("LicenseManagement.createUserLicense()")
                    .setDetails("Cloud Platform ID " + cloudPlatformId + " | License Group ID: null");
            ;
            alertFunc.alertPlatformAdmins(am);
            return response;
        }

        // get count of licenses in use
        log.debug("{}Getting existing license counts", logPrefix);
        Map<Long, Long> licenseCount = dbFunc.getCurrentLicenseUsage();
        Long lgCount = licenseCount.get((Long) licenseGroupDetails.get("LICENSEGROUPID"));

        // check current license count
        if (lgCount >= (Long) licenseGroupDetails.get("HARDLIMIT")) {
            // over hard limit
            log.error("{}License Group {} has {} issued licenses, over hard limit of {}", logPrefix, licenseGroupDetails.get("LICENSEGROUPID"), lgCount, licenseGroupDetails.get("HARDLIMIT"));
            response.setFriendlyMessage("There are insufficent licenses available at this time. Please contact your team leader.");
            response.setDetailedMessage("License Group " + (Long) licenseGroupDetails.get("LICENSEGROUPID") + " has " + lgCount + " >  Hard Limit: " + (Long) licenseGroupDetails.get("HARDLIMIT") + "");
            response.setSuccess(false);
            AlertMessage am = new AlertMessage()
                    .setSubject("GCloud Licensing - License Hard Limit Reached")
                    .setMessage("License Group: " + (String) licenseGroupDetails.get("LICENSEGROUPNAME") + " has exceeded its hard limit of licenses. A user has been affected!")
                    .setSource("LicenseManagement.createUserLicense()")
                    .setDetails(
                            "Cloud Platform ID: " + cloudPlatformId
                            + " | License Group ID: " + (Long) licenseGroupDetails.get("LICENSEGROUPID")
                            + " | Hard Limit: " + (Long) licenseGroupDetails.get("HARDLIMIT")
                            + " | Current Licenses: " + lgCount
                            + " | User UPN: " + upn
                            + " | User Name: " + (String) licenseGroupDetails.get("USERFULLNAME")
                            + " | User Type: " + (String) licenseGroupDetails.get("USERTYPENAME")
                    );
            alertFunc.alertPlatformAdmins(am);
            return response;
        }
        else if (lgCount >= (Long) licenseGroupDetails.get("SOFTLIMIT")) {
            // over soft limit
            log.warn("{}License Group {} has {} issued licenses, over soft limit of {}", logPrefix, lgCount, licenseGroupDetails.get("SOFTLIMIT"));

            AlertMessage am = new AlertMessage()
                    .setSubject("GCloud Licensing - License Soft Limit Reached")
                    .setMessage("License Group: " + (String) licenseGroupDetails.get("LICENSEGROUPNAME") + " has exceeded its soft limit of licenses. There is no user impact yet.")
                    .setSource("LicenseManagement.createUserLicense()")
                    .setDetails(
                            "Cloud Platform ID: " + cloudPlatformId
                            + " | License Group ID: " + (Long) licenseGroupDetails.get("LICENSEGROUPID")
                            + " | Soft Limit: " + (Long) licenseGroupDetails.get("SOFTLIMIT")
                            + " | Hard Limit: " + (Long) licenseGroupDetails.get("HARDLIMIT")
                            + " | Current Licenses: " + lgCount + ""
                    );
            alertFunc.alertPlatformAdmins(am);
        }

        log.info("{}Creating new license for {} on platform {}", logPrefix, upn, cloudPlatformId);

        // update AzureAd Group
        String groupName = cloudFunc.getAzureAdAccessGroup(cloudPlatformId);
        log.trace("{}Find Azure group '{}' for PlatformID: {}", logPrefix, groupName, cloudPlatformId);

        if (groupName != null && !groupName.isBlank()) {
            Boolean success = adFunc.addUserToGroup(upn, groupName);
            if (!success) {
                log.error("{}An error occurred adding user to the AD group", logPrefix);
                response.setFriendlyMessage("A system error occurred allocating a license. Please contact your team leader.");
                response.setDetailedMessage("AzureAD addUserToGroup function returned error");
                response.setSuccess(false);
                AlertMessage am = new AlertMessage()
                        .setSubject("GCloud Licensing - AzureAD Group Addition Failed")
                        .setMessage("User: " + upn + " could not be added to AzureAD group " + groupName)
                        .setSource("LicenseManagement.createUserLicense()")
                        .setDetails(
                                "Cloud Platform ID: " + cloudPlatformId
                                + " | License Group ID: " + (Long) licenseGroupDetails.get("LICENSEGROUPID")
                                + " | Current Licenses: " + lgCount
                                + " | User UPN: " + upn
                                + " | User Name: " + (String) licenseGroupDetails.get("USERFULLNAME")
                                + " | User Type: " + (String) licenseGroupDetails.get("USERTYPENAME")
                                + " | Azure AD Group: " + groupName
                        );
                alertFunc.alertPlatformAdmins(am);
                return response;
            }
        }
        else {
            log.warn("{}No AzureAd group defined for Cloud Platform {}. Not adding to the group", logPrefix, cloudPlatformId);
        }

        // write license to database
        Long secondsToAdd = (Long) licenseGroupDetails.get("DEFAULTISSUESECONDS");
        LocalDateTime expiryTime = LocalDateTime.now().plusSeconds(secondsToAdd);
        if (dbFunc.writeLicenseToDatabase((Long) licenseGroupDetails.get("USERID"), (Long) licenseGroupDetails.get("LICENSEGROUPID"), cloudPlatformId, expiryTime)) {
            log.info("{}Successfully wrote license to database. Returning license allocation success", logPrefix);
            response.setSuccess(true);
        }
        else {
            log.error("{}An error occurred writing the license to the database", logPrefix);
            response.setFriendlyMessage("A system error occurred allocating a license. Please contact your team leader.");
            response.setDetailedMessage("Database writeLicense function returned error");
            response.setSuccess(false);
            AlertMessage am = new AlertMessage()
                    .setSubject("GCloud Licensing - Database Write Failed")
                    .setMessage("User: " + upn + " license could not be written to the database")
                    .setSource("LicenseManagement.createUserLicense()")
                    .setDetails(
                            "Cloud Platform ID: " + cloudPlatformId
                            + " | License Group ID: " + (Long) licenseGroupDetails.get("LICENSEGROUPID")
                            + " | Current Licenses: " + lgCount
                            + " | User UPN: " + upn
                            + " | User Name: " + (String) licenseGroupDetails.get("USERFULLNAME")
                            + " | User Type: " + (String) licenseGroupDetails.get("USERTYPENAME")
                            + " | Azure AD Group: " + groupName
                    );
            alertFunc.alertPlatformAdmins(am);
        }

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

        Map<Long, LicenseDTO> licenses = dbFunc.getUserLicensesFromDB(upn);
        log.info("{}Found a total of {} issued licenses for user {}", logPrefix, licenses.size(), upn);
        return licenses;

    }

    // check to see if a user owns a particular license
    private Boolean checkUserOwnsLicense(String upn, Long licenseId) {
        final String logPrefix = "checkUserOwnsLicense() - ";
        log.trace("{}Entering Method", logPrefix);
        if (licenseId == null || licenseId == 0) {
            log.error("{}licenseId cannot be null or zero", logPrefix);
            throw new IllegalArgumentException("licenseId cannot be null or zero");
        }
        if (upn == null || upn.isBlank()) {
            log.error("{}UPN cannot be null or empty", logPrefix);
            throw new IllegalArgumentException("UPN cannot be null or empty");
        }
        log.info("{}Checking if user: {} owns license ID {}", logPrefix);

        LicenseDTO license = dbFunc.getLicenseFromDB(licenseId);
        if (license == null) {
            log.warn("{}License does not exist", logPrefix);
            return false;
        }

        if (license.getUpn().equals(upn)) {
            log.info("{}License {} is valid for user {}", logPrefix, license, upn);
            return true;
        }

        log.warn("{}The license for ID {} is not associated to user {}", logPrefix, licenseId, upn);
        return false;

    }

    // Extend license
    public BooleanResponse extendUserLicense(String upn, Long licenseId) {
        final String logPrefix = "extendUserLicense(S,L) - ";
        log.trace("{}Entering Method", logPrefix);
        if (licenseId == null || licenseId == 0) {
            log.error("{}licenseId cannot be null or zero", logPrefix);
            throw new IllegalArgumentException("licenseId cannot be null or zero");
        }
        if (upn == null || upn.isBlank()) {
            log.error("{}UPN cannot be null or empty", logPrefix);
            throw new IllegalArgumentException("UPN cannot be null or empty");
        }

        BooleanResponse response = new BooleanResponse();

        if (!checkUserOwnsLicense(upn, licenseId)) {
            response.setSuccess(false)
                    .setFriendlyMessage("The license you are trying to extend is invalid")
                    .setDetailedMessage("User does not own license ID #" + licenseId + "");
            return response;
        }

        return extendUserLicense(licenseId);
    }

    // Return License
    public BooleanResponse returnUserLicense(String upn, Long licenseId) {
        final String logPrefix = "returnUserLicense(S,L) - ";
        log.trace("{}Entering Method", logPrefix);
        if (licenseId == null || licenseId == 0) {
            log.error("{}licenseId cannot be null or zero", logPrefix);
            throw new IllegalArgumentException("licenseId cannot be null or zero");
        }
        if (upn == null || upn.isBlank()) {
            log.error("{}UPN cannot be null or empty", logPrefix);
            throw new IllegalArgumentException("UPN cannot be null or empty");
        }

        BooleanResponse response = new BooleanResponse();

        if (!checkUserOwnsLicense(upn, licenseId)) {
            response.setSuccess(false)
                    .setFriendlyMessage("The license you are trying to return is invalid")
                    .setDetailedMessage("User does not own license ID #" + licenseId + "");
            return response;
        }

        return returnUserLicense(licenseId);
    }

    // Extend license
    public BooleanResponse extendUserLicense(Long licenseId) {
        final String logPrefix = "extendUserLicense(L) - ";
        log.trace("{}Entering Method", logPrefix);
        if (licenseId == null || licenseId == 0) {
            log.error("{}licenseId cannot be null or zero", logPrefix);
            throw new IllegalArgumentException("licenseId cannot be null or zero");
        }

        BooleanResponse response = new BooleanResponse();

        log.info("{}Extending license {}", logPrefix, licenseId);
        LicenseDTO license = dbFunc.getLicenseFromDB(licenseId);
        LocalDateTime currentExpiry = license.getExpiryDate();
        if (currentExpiry.isBefore(LocalDateTime.now())) {
            log.error("{}Not renewing expired license!", logPrefix);
            response.setSuccess(false)
                    .setFriendlyMessage("Your license has expired and cannot be renewed")
                    .setDetailedMessage("License expired at " + currentExpiry.format(DateTimeFormatter.ISO_DATE_TIME) + " - current time is " + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
            return response;
        }
        String upn = license.getUpn();
        Long cloudPlatformId = license.getCloudPlatformId();
        log.debug("{}Getting license group details", logPrefix);
        Map<String, Object> licenseGroupDetails = dbFunc.getUserLicenseGroupDetails(upn, cloudPlatformId);
        if (licenseGroupDetails == null) {
            response.setFriendlyMessage("Your account is not configured correctly. Please contact your team leader.");
            response.setDetailedMessage("Unable to retrieve license group details");
            response.setSuccess(false);
            AlertMessage am = new AlertMessage()
                    .setSubject("GCloud Licensing - User not configured")
                    .setMessage("User: " + upn + " is not configured and could not extend their license")
                    .setSource("LicenseManagement.createUserLicense()")
                    .setDetails("Cloud Platform ID " + cloudPlatformId + " | License Group ID: null");
            alertFunc.alertPlatformAdmins(am);
            return response;
        }

        Long secondsToAdd = (Long) licenseGroupDetails.get("EXTENSIONTIMESECONDS");
        LocalDateTime newExpiry = currentExpiry.plusSeconds(secondsToAdd);
        log.debug("{}Setting new expiry {}", logPrefix, newExpiry.format(DateTimeFormatter.ISO_DATE_TIME));
        log.debug("{}About to write to database", logPrefix);
        if (dbFunc.extendLicense(licenseId, newExpiry)) {
            response.setSuccess(true);
            return response;
        }

        response.setSuccess(false)
                .setDetailedMessage("Error extending license in database")
                .setFriendlyMessage("There was an error extending your license");
        return response;
    }

    // Return License
    public BooleanResponse returnUserLicense(Long licenseId) {
        final String logPrefix = "returnUserLicense(L,B) - ";
        log.trace("{}Entering Method", logPrefix);
        if (licenseId == null || licenseId == 0) {
            log.error("{}licenseId cannot be null or zero", logPrefix);
            throw new IllegalArgumentException("licenseId cannot be null or zero");
        }

        BooleanResponse response = new BooleanResponse();

        log.info("{}Returning license {}", logPrefix, licenseId);

        LicenseDTO license = dbFunc.getLicenseFromDB(licenseId);
        String upn = license.getUpn();
        Long cloudPlatformId = license.getCloudPlatformId();
        log.debug("{}Getting license group details", logPrefix);
        Map<String, Object> licenseGroupDetails = dbFunc.getUserLicenseGroupDetails(upn, cloudPlatformId);
        if (licenseGroupDetails == null) {
            response.setFriendlyMessage("Your account is not configured correctly. Please contact your team leader.");
            response.setDetailedMessage("Unable to retrieve license group details");
            response.setSuccess(false);
            AlertMessage am = new AlertMessage()
                    .setSubject("GCloud Licensing - User not configured")
                    .setMessage("User: " + upn + " is not configured and could not extend their license")
                    .setSource("LicenseManagement.createUserLicense()")
                    .setDetails("Cloud Platform ID " + cloudPlatformId + " | License Group ID: null");
            alertFunc.alertPlatformAdmins(am);
            return response;
        }

        // Remove from AD Group
        log.debug("{}About to remove user from AD Group", logPrefix);
        String groupName = cloudFunc.getAzureAdAccessGroup(cloudPlatformId);
        log.trace("{}Find Azure group '{}' for PlatformID: {}", logPrefix, groupName, cloudPlatformId);

        if (groupName != null && !groupName.isBlank()) {
            Boolean success = adFunc.removeUserFromGroup(upn, groupName);
            if (!success) {
                log.error("{}An error occurred removing the user from the AD group", logPrefix);
                response.setFriendlyMessage("A system error occurred returning this license. Please contact your team leader.");
                response.setDetailedMessage("AzureAD removeUserFromGroup function returned error");
                response.setSuccess(false);
                AlertMessage am = new AlertMessage()
                        .setSubject("GCloud Licensing - AzureAD Group Removal Failed")
                        .setMessage("User: " + upn + " could not be removed from to AzureAD group " + groupName)
                        .setSource("LicenseManagement.createUserLicense()")
                        .setDetails(
                                "Cloud Platform ID: " + cloudPlatformId
                                + " | License Group ID: " + (Long) licenseGroupDetails.get("LICENSEGROUPID")
                                + " | User UPN: " + upn
                                + " | User Name: " + (String) licenseGroupDetails.get("USERFULLNAME")
                                + " | User Type: " + (String) licenseGroupDetails.get("USERTYPENAME")
                                + " | Azure AD Group: " + groupName
                        );
                alertFunc.alertPlatformAdmins(am);
                return response;
            }
        }
        else {
            log.warn("{}No AzureAd group defined for Cloud Platform {}. Not removing user from the group", logPrefix, cloudPlatformId);
        }

        // Log out agent
        log.debug("{}About to force user out of Genesys", logPrefix);
        if (!cloudFunc.forceLogOutUser(upn)) {
            response.setSuccess(false)
                    .setDetailedMessage("Error logging user out of Cloud")
                    .setFriendlyMessage("There was an error returning your license");
            return response;
        }

        // Delete from Database
        log.debug("{}About to delete from database", logPrefix);
        if (dbFunc.deleteLicense(licenseId)) {
            response.setSuccess(true);
            return response;
        }
        else {
            response.setSuccess(false)
                    .setDetailedMessage("Error deleting license from database")
                    .setFriendlyMessage("There was an error returning your license");
            return response;
        }
    }

    // Get User Platforms
    public List<PlatformDTO> getUserPlatforms(String upn) {
        final String logPrefix = "getUserPlatforms() - ";
        log.trace("{}Entering Method", logPrefix);

        if (upn == null || upn.isBlank()) {
            log.error("{}UPN cannot be null or empty", logPrefix);
            throw new IllegalArgumentException("UPN cannot be null or empty");
        }

        List<PlatformDTO> platforms = dbFunc.getPlatformsForUserFromDB(upn);
        log.info("{}Found a total of {} platforms for user {}", logPrefix, platforms.size(), upn);
        return platforms;
    }

}
