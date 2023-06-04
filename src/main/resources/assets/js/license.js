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


let ajaxBase = contextPath + 'api/v1/';
if (debug)
    console.log("AJAX Base:", ajaxBase);

let requestInProgress = false;

let extendIconCss = 'fas fa-square-plus';
let allocateIconCss = 'fas fa-up-right-from-square';
let revokeIconCss = 'fas fa-flag-checkered';

let revokeModal = new bootstrap.Modal(document.getElementById("revokeConfimDialog"), {});

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
                    console.log("Rendering table row cell 4 for data", data);
                if (row.licenseAllocated) {
                    if (!row.expired && row.canExtend) {
                        buttonHtml += '<button class="btn btn-outline-warning btnLicense' + meta.row + '" onclick="extendLicense(\'' + data + '\', ' + meta.row + ');"><span class="' + extendIconCss + '" id="iconExtend' + data + '"></span> Extend</button>&nbsp;';
                    }
                    buttonHtml += '<button class="btn btn-outline-danger btnLicense' + meta.row + '" onclick="revokeLicense(\'' + data + '\', ' + meta.row + ');"><span class="' + revokeIconCss + '" id="iconRevoke' + data + '"></span> Return</button>';
                }
                else {
                    buttonHtml += '<button class="btn btn-outline-success btnLicense' + meta.row + '" onclick="allocateLicense(\'' + row.cloudPlatformId + '\', ' + meta.row + ');"><span class="' + allocateIconCss + '" id="iconAllocate' + row.cloudPlatformId + '"></span> Allocate License</button>';
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
                console.log("Callback: Is Expired");
            $("td", row).eq(3).addClass("bg-danger bg-gradient text-white");
        }
        else if (data.canExtend === true) {
            if (debug)
                console.log("Callback: Can Extend");
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


function extendLicense(licenseId, rowid) {
    if (debug)
        console.log("Extending license ", licenseId);
    if (requestInProgress) {
        console.warn("Request in progress, not proceeding");
        return;
    }
    $("#iconExtend" + licenseId).removeClass(extendIconCss).addClass('spinner-border spinner-border-sm');
    requestInProgress = true;
    let licenseRequest = {
        "cloudPlatformId": null,
        "licenseId": licenseId,
        "requestType": "EXTEND"
    };

    performLicenseRequest(licenseRequest, rowid);

}


function allocateLicense(platformId, rowid) {
    if (debug)
        console.log("Allocating license for platform", platformId);
    if (requestInProgress) {
        console.warn("Request in progress, not proceeding");
        return;
    }
    $("#iconAllocate" + platformId).removeClass(allocateIconCss).addClass('spinner-border spinner-border-sm');
    requestInProgress = true;
    let licenseRequest = {
        "cloudPlatformId": platformId,
        "licenseId": 0,
        "requestType": "CREATE"
    };

    performLicenseRequest(licenseRequest, rowid);
}

function revokeLicense(licenseId, rowid) {
    if (debug)
        console.log("Revoking license", licenseId);
    if (requestInProgress) {
        console.warn("Request in progress, not proceeding");
        return;
    }
    $('#btnRevokeConfirm').data('license-id', licenseId);
    $('#btnRevokeConfirm').data('row-id', rowid);
    revokeModal.show();
}

function cancelRevoke() {
    if (debug)
        console.log("Hiding revoke dialog");
    revokeModal.hide();
}

function completeRevoke() {
    console.log("Starting revocation. Checking dialog for identifiers");
    let licenseId = $('#btnRevokeConfirm').data('license-id');
    let rowid = $('#btnRevokeConfirm').data('row-id');
    if (licenseId === "NOT_SET" || rowid === "NOT_SET") {
        return;
    }
    revokeModal.hide();

    console.log("Revoking license", licenseId, "buttons on row", rowid);
    $('#btnRevokeConfirm').data('NOT_SET', licenseId);
    $('#btnRevokeConfirm').data('NOT_SET', rowid);

    if (debug)
        console.log("Revoking license", licenseId);
    if (requestInProgress) {
        console.warn("Request in progress, not proceeding");
        return;
    }
    $("#iconRevoke" + licenseId).removeClass(revokeIconCss).addClass('spinner-border spinner-border-sm');
    requestInProgress = true;
    let licenseRequest = {
        "cloudPlatformId": null,
        "licenseId": licenseId,
        "requestType": "REVOKE"
    };

    performLicenseRequest(licenseRequest, rowid);

}


function performLicenseRequest(licenseRequest, rowid) {
    if (debug)
        console.log("Row action on :", rowid);
    button = $('.btnLicense' + rowid);
    button.prop('disabled', true);
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
                requestInProgress = false;
                console.log("Adjusting icons for type", licenseRequest.requestType);
                if (licenseRequest.requestType === "CREATE") {
                    $("#iconAllocate" + licenseRequest.cloudPlatformId).removeClass('spinner-border spinner-border-sm').addClass(allocateIconCss);
                }
                else if (licenseRequest.requestType === "REVOKE") {
                    $("#iconRevoke" + licenseRequest.licenseId).removeClass('spinner-border spinner-border-sm').addClass(revokeIconCss);
                }
                else if (licenseRequest.requestType === "EXTEND") {
                    $("#iconExtend" + licenseRequest.licenseId).removeClass('spinner-border spinner-border-sm').addClass(extendIconCss);
                }
                button.prop('disabled', false);
                console.log("Reloading license table");
                licenseTable.ajax.reload();
            }
            else {
                console.error("API Returned Error:", responseData.detailedMessage);
                $('#jsErrorText').text(responseData.friendlyMessage);
                $('#jsError').show();
                button.prop('disabled', false);
                requestInProgress = false;
                if (licenseRequest.requestType === "CREATE") {
                    $("#iconAllocate" + licenseRequest.cloudPlatformId).removeClass('spinner-border spinner-border-sm').addClass(allocateIconCss);
                }
                else if (licenseRequest.requestType === "REVOKE") {
                    $("#iconRevoke" + licenseRequest.licenseId).removeClass('spinner-border spinner-border-sm').addClass(revokeIconCss);
                }
                else if (licenseRequest.requestType === "EXTEND") {
                    $("#iconExtend" + licenseRequest.licenseId).removeClass('spinner-border spinner-border-sm').addClass(extendIconCss);
                }
            }
        },
        error: function (error) {
            console.error("Error calling API:", error);
            $('#jsErrorText').text(stringMap[licenseRequest.requestType].failure);
            $('#jsError').show();
            button.prop('disabled', false);
            requestInProgress = false;
            if (licenseRequest.requestType === "CREATE") {
                $("#iconAllocate" + licenseRequest.cloudPlatformId).removeClass('spinner-border spinner-border-sm').addClass(allocateIconCss);
            }
            else if (licenseRequest.requestType === "REVOKE") {
                $("#iconRevoke" + licenseRequest.licenseId).removeClass('spinner-border spinner-border-sm').addClass(revokeIconCss);
            }
            else if (licenseRequest.requestType === "EXTEND") {
                $("#iconExtend" + licenseRequest.licenseId).removeClass('spinner-border spinner-border-sm').addClass(extendIconCss);
            }
        }
    });
}


let refreshId = setInterval(refreshDataTable, 30000);

function refreshDataTable() {
    if (debug)
        console.log("Refreshing data table on timer");
    licenseTable.ajax.reload();
}