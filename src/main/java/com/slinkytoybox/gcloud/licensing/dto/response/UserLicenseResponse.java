/*
 *   gcloudlicensemanagement - UserLicenseResponse.java
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
package com.slinkytoybox.gcloud.licensing.dto.response;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 *
 * @author Michael Junek (michael@juneks.com.au)
 */
@Data
@Accessors(chain = true)
public class UserLicenseResponse implements Serializable {

    private List<LicenseResponse> licenseResponse;

    @Data
    @Accessors(chain = true)
    public static class LicenseResponse implements Comparable<LicenseResponse>, Serializable {

        private Long cloudPlatformId;
        private String platformName;
        private String organisationName;
        private String organisationId;
        private LocalDateTime issueDate;
        private LocalDateTime expiryDate;
        private String upn;
        private boolean licenseAllocated;
        private Long licenseId;
        private boolean canExtend;
        private boolean expired;

        @Override
        public int compareTo(LicenseResponse o1) {
            return this.platformName.compareToIgnoreCase(o1.platformName);
        }

    }
}
