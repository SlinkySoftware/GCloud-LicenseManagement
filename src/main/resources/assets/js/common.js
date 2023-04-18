/* 
 *   gcloudlicensing - common.js
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


// Converts a Serialized Java LocalDateTime into a text format
function localDateTimeArrayToYYYYMMDD_HHMMSS(data, dateSeparator, timeSeparator, dateTimeSeparator) {
    var year = (data[0] == null ? 2021 : data[0]);
    var month = zeroPad((data[1] == null ? 1 : data[1]));
    var day = zeroPad((data[2] == null ? 1 : data[2]));
    var hour = zeroPad((data[3] == null ? 0 : data[3]));
    var minute = zeroPad((data[4] == null ? 0 : data[4]));
    var second = zeroPad((data[5] == null ? 0 : data[5]));
    var output = year + dateSeparator + month + dateSeparator + day + dateTimeSeparator + hour + timeSeparator + minute + timeSeparator + second;
    return output;
}

// Converts a Serialized Java LocalDateTime into a text format
function localDateTimeArrayToDDMMYYYY_HHMMSS(data, dateSeparator, timeSeparator, dateTimeSeparator) {
    monthOption = {month: "short"};


    var year = (data[0] == null ? 2021 : data[0]);
    var tempMonth = zeroPad((data[1] == null ? 1 : data[1]));
    var day = zeroPad((data[2] == null ? 1 : data[2]));
    var hour = zeroPad((data[3] == null ? 0 : data[3]));
    var minute = zeroPad((data[4] == null ? 0 : data[4]));
    var second = zeroPad((data[5] == null ? 0 : data[5]));

    var d = new Date(year, tempMonth - 1, day, hour, minute, second, 0);
    var month = d.toLocaleDateString('en-au', monthOption);
    var output = day + dateSeparator + month + dateSeparator + year + dateTimeSeparator + hour + timeSeparator + minute + timeSeparator + second;

    return output;
}


function zeroPad(stringToPad) {
    var strTemp = "00" + stringToPad;
    strTemp = strTemp.substring(strTemp.length - 2, strTemp.length);
    return strTemp;

}