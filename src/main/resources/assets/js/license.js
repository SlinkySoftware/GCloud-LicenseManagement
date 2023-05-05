/* 
 *   gcloudlicensemanagement - license.js
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

let debug = true;
let ajaxBase = contextPath + 'api/v1/';
if (debug)
    console.log("AJAX Base:", ajaxBase);

let licenseTable = $('#licenseTable').DataTable({
    "autoWidth": false,
    "lengthChange": false,
    "processing": true,
    "searching": false,
    "pageLength": 20,
    "serverSide": false,
    "paging": false,
    "info": false,
    "ordering": false,
    "ajax": {
        "url": ajaxBase + "myLicenses",
        "contentType": "application/json",
        "type": "POST",
        "dataType": "json",
        "dataSrc": "licenseResponse",
        "data": function (d) {
            return JSON.stringify(d);
        }
    },
    "columns": [
        {"data": "platformName"},
        {"data": "licenseAllocated"},
        {"data": "issueDate"},
        {"data": "expiryDate"}
    ],
    "columnDefs": [
        {
            "targets": 4,
            "data": "licenseId",
            "render": function (data, type, row, meta) {
                let buttonHtml = "";
                if (debug)
                    console.log("Rendering able row cell 4 for data", data);
                if (row.licenseAllocated) {
                    if (!row.expired) {
                        buttonHtml += '<button class="btn btn-outline-info" onclick="loadPlatform(\'' + row.organisationId + '\');"><span class="fas fa-right-to-bracket"></span> Log In</button>&nbsp;';
                        if (row.canExtend) {
                            buttonHtml += '<button class="btn btn-outline-warning" onclick="extendLicense(\'' + data + '\', this);"><span class="fas fa-square-plus"></span> Extend</button>&nbsp;';
                        }
                    }
                    buttonHtml += '<button class="btn btn-outline-danger" onclick="revokeLicense(\'' + data + '\', this);"><span class="fas fa-flag-checkered"></span> Return</button>';
                }
                else {
                    buttonHtml += '<button class="btn btn-outline-success" onclick="allocateLicense(\'' + row.cloudPlatformId + '\', this);"><span class="fas fa-up-right-from-square"></span> Allocate License</button>';
                }
                if (debug)
                    console.log("ButtonHTML", buttonHtml);
                return buttonHtml;
            }
        },
        {
            "targets": 1,
            "render": function (data, type, row, meta) {
                let iconHtml = "";
                if (debug)
                    console.log("Rendering able row cell 1 for data:", data);
                if (data) {
                    iconHtml += '<span class="fas fa-circle-check text-success"></span>';
                }
                else {
                    iconHtml += '<span class="fas fa-circle-xmark text-danger"></span>';
                }
                if (debug)
                    console.log("IconHtml", iconHtml);
                return iconHtml;
            }
        },
        {
            "targets": [2, 3],
            "render": function (data, type, row, meta) {
                if (debug)
                    console.log("Rendering able row cell 2/3 for data:", data);
                if (data === null)
                    return '';
                return localDateTimeArrayToDDMMYYYY_HHMMSS(data, '-', ':', ' ');
            }

        }
    ]

    ,
    "rowCallback": function (row, data, index) {
        $("td", row).addClass("align-middle");
        if (debug)
            console.log("RowCallback - row", row, "data", data, "index", index);
        if (data.expired === true) {
            if (debug)
                console.log("Callback: Expired");
            $("td", row).eq(3).addClass("bg-danger bg-gradient text-white");
        }
        else if (data.canExtend === true) {
            if (debug)
                console.log("Callback: Extend");
            $("td", row).eq(3).addClass("bg-warning bg-gradient text-white");
        }
    }
});


let stringMap = {
    CREATE: {
        success: "A license was allocated successfully.",
        failure: "An error was encountered whilst allocating a license."
    },
    REVOKE: {
        success: "The license was returned successfully.",
        failure: "An error was encountered whilst returning the license."
    },
    EXTEND: {
        success: "The license was extended successfully.",
        failure: "An error was encountered whilst extending the license."
    }
};


function loadPlatform(organisationId) {
    if (debug)
        console.log("Logging in to platform", organisationId);
    window.open('https://login.mypurecloud.com.au/#/authenticate-adv/org/' + organisationId, '_blank');
}


function extendLicense(licenseId, button) {
    if (debug)
        console.log("Extending license ", licenseId);

    let licenseRequest = {
        "cloudPlatformId": null,
        "licenseId": licenseId,
        "requestType": "EXTEND"
    };

    performLicenseRequest(licenseRequest, button);

}


function allocateLicense(platformId, button) {
    if (debug)
        console.log("Allocating license for platform", platformId);

    let licenseRequest = {
        "cloudPlatformId": platformId,
        "licenseId": 0,
        "requestType": "CREATE"
    };

    performLicenseRequest(licenseRequest, button);
}

function revokeLicense(licenseId, button) {
    if (debug)
        console.log("Revoking license ", licenseId);

    let licenseRequest = {
        "cloudPlatformId": null,
        "licenseId": licenseId,
        "requestType": "REVOKE"
    };

    performLicenseRequest(licenseRequest, button);

}


function performLicenseRequest(licenseRequest, button) {
    if (debug)
        console.log("Button clicked:", button);

    $.ajax({
        url: ajaxBase + "modifyLicense",
        method: "POST",
        contentType: "application/json",
        dataType: "json",
        "data": JSON.stringify(licenseRequest),
        success: function (responseData) {
            console.log("AJAX finished - ResponseData:", responseData);
            if (responseData.success) {
                console.log("Success creating license");
                $('#jsSuccessText').text(stringMap[licenseRequest.requestType].success);
                $('#jsSuccess').show();
                console.log("Reloading license table");
                licenseTable.ajax.reload();
            }
            else {
                console.log("Error:", responseData.errorMessage);
                $('#jsErrorText').text(stringMap[licenseRequest.requestType].failure);
                $('#jsError').show();

            }
        },
        error: function (error) {
            console.log("Error:", error);
            $('#jsErrorText').text(stringMap[licenseRequest.requestType].failure);
            $('#jsError').show();
        }
    });
}


