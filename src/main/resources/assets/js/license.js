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
                    console.log("Rendering able row cell 4 for", row);
                if (row.licenseAllocated) {
                    if (!row.expired) {
                        buttonHtml += '<button data-mdb-ripple-color="dark" class="btn btn-outline-info" onclick="loadPlatform(\'' + row.organisationId + '\');"><span class="fas fa-right-to-bracket"></span> Log In</button>&nbsp;';
                        if (row.canExtend) {
                            buttonHtml += '<button data-mdb-ripple-color="dark" class="btn btn-outline-warning" onclick="extendLicense(\'' + data + '\');"><span class="fas fa-square-plus"></span> Extend</button>&nbsp;';
                        }
                    }
                    buttonHtml += '<button data-mdb-ripple-color="dark" class="btn btn-outline-danger" onclick="revokeLicense(\'' + data + '\');"><span class="fas fa-flag-checkered"></span> Return</button>';
                }
                else {
                    buttonHtml += '<button data-mdb-ripple-color="dark" class="btn btn-outline-success" onclick="allocateLicense(\'' + row.cloudPlatformId + '\');"><span class="fas fa-up-right-from-square"></span> Allocate License</button>';
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
                    console.log("Rendering able row cell 1 for", row);
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




function loadPlatform(organisationId) {
    if (debug)
        console.log("Logging in to platform", organisationId);
    window.open('https://login.mypurecloud.com.au/#/authenticate-adv/org/' + organisationId + "?provider=adfs", '_blank');
}


function extendLicense(licenseId) {
    if (debug)
        console.log("Extending license ID", licenseId);
}


function allocateLicense(platformId) {
    if (debug)
        console.log("Allocating license for platform", platformId);
}


function revokeLicense(licenseId) {
    if (debug)
        console.log("Revoking license ID", licenseId);
}
