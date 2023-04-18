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
if (debug) console.log("AJAX Base:", ajaxBase);

let licenseTable = $('#licenseTable').DataTable({
    "autoWidth": false,
    "lengthChange": false,
    "processing": true,
    "searching": false,
    "pageLength": 20,
    "serverSide": false,
    "order": [],
    "ajax": {
        "url": ajaxBase + "myLicenses",
        "contentType": "application/json",
        "type": "POST",
        "dataType": "json",
        "dataSrc": "licenseResponse",
        "data": function (d) {
            if (debug) console.log("Table data:", JSON.stringify(d));
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
            "orderable": false,
            "targets": 4,
            "data": "licenseId",
            "render": function (data, type, row, meta) {
                let buttonHtml = "";
                if (debug) console.log("Rendering able row cell 4 for", row);
                if (row.licenseAllocated) {
                    buttonHtml += "<button>Extend License</button>";
                    buttonHtml += "<button>Return License</button>";
                    buttonHtml += "<button>Log In</button>";
                }
                else {
                    buttonHtml += "<button>Allocate License</button>";
                }
                if (debug) console.log("ButtonHTML", buttonHtml);
//                var iconHtml = '<i class="fas fa-edit pr-2 fa-lg text-primary" title="Edit ' + row.name + '" onclick="editPP(' + data + ')"></i>';
//                iconHtml += '<i class="fas fa-sitemap pr-2 fa-lg text-secondary" title="Show dependent items" onclick="showDependents(' + data + ')"></i>';
//                iconHtml += '<i class="fas fa-map-marker-alt pr-2 fa-lg text-success" title="Show associated places" onclick="showPlaces(' + data + ')"></i>';
                return buttonHtml;
            }
        }
    ]

//,
//    "rowCallback": function (row, data, index) {
//        if (data.enabled == false) {
//            $("td", row).addClass("text-muted strikethrough");
//        }
//    }
});




function populateLicenseTable() {
    if (debug)
        console.log("Populate license table starting");


}
