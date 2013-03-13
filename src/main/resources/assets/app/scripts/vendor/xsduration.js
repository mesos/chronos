// https://github.com/revington/xsdurationjs
(function (xsdurationjs) {
    "use strict";
    var xsdurationRegEx = /(-?)P((\d{1,4})Y)?((\d{1,4})M)?((\d{1,4})D)?(T((\d{1,4})H)?((\d{1,4})M)?((\d{1,4}(\.\d{1,3})?)S)?)?/,
        years = 3,
        months = 5,
        days = 7,
        hours = 10,
        minutes = 12,
        seconds = 14,
        daysMonth = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31],
        daysMonthLeap = [31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
    var fQuotient = xsdurationjs.fQuotient = function () {
            var a = arguments;
            if (a.length === 2) {
                // fQuotient(a, b) = the greatest integer less than or equal to a/b 
                return Math.floor(a[0] / a[1]);
            } else if (a.length === 3) {
                // fQuotient(a, low, high) = fQuotient(a - low, high - low) 
                return fQuotient(a[0] - a[1], a[2] - a[1]);
            }
        };
    var modulo = xsdurationjs.modulo = function modulo() {
            var a = arguments;
            if (a.length === 2) {
                // modulo(a, b) = a - fQuotient(a,b)*b 
                return a[0] - fQuotient(a[0], a[1]) * a[1];
            } else if (a.length === 3) {
                // modulo(a, low, high) = modulo(a - low, high - low) + low 
                return modulo(a[0] - a[1], a[2] - a[1]) + a[1];
            }
        };

    function isLeapYear(year) {
        return ((year % 4 === 0 && year % 100 !== 0) || year % 400 === 0);
    }

    function maximumDayInMonthFor(year, month) {
        return (isLeapYear(year) ? daysMonthLeap : daysMonth)[month - 1];
    }
    /**
     * See http://www.w3.org/TR/xmlschema-2/#adding-durations-to-dateTimes
     */

    function w3calgo(s, d) {
        var carry, temp, tempDays, e = {};
        //months
        temp = s.M + d.M;
        e.M = modulo(temp, 1, 13);
        carry = fQuotient(temp, 1, 13);
        // years
        e.Y = s.Y + d.Y + carry;
        // zone
        // e.Z = s.Z;
        // seconds 
        temp = s.S + d.S;
        e.S = modulo(temp, 60);
        carry = fQuotient(temp, 60);
        // minutes
        temp = s.m + d.m + carry;
        e.m = modulo(temp, 60);
        carry = fQuotient(temp, 60);
        // hours  
        temp = s.H + d.H + carry;
        e.H = modulo(temp, 24);
        carry = fQuotient(temp, 24);
        // days
        if (s.D > maximumDayInMonthFor(e.Y, e.M)) {
            tempDays = maximumDayInMonthFor(e.Y, e.M);
        } else if (s.D < 1) {
            tempDays = 1;
        } else {
            tempDays = s.D;
        }
        e.D = tempDays + d.D + carry;
        // start loop
        while (true) {
            if (e.D < 1) {
                e.D = e.D + maximumDayInMonthFor(e.D, e.M - 1);
                carry = -1;
            } else if (e.D > maximumDayInMonthFor(e.Y, e.M)) {
                e.D = e.D - maximumDayInMonthFor(e.Y, e.M);
                carry = 1;
            } else {
                break;
            }
            temp = e.M + carry;
            e.M = modulo(temp, 1, 13);
            e.Y = e.Y + fQuotient(temp, 1, 13);
        }
        e.Y = e.Y | 0;
        // in js dates month start at 0.
        e.M = e.M - 1 | 0;
        e.D = e.D | 0;
        e.H = e.H | 0;
        e.m = e.m | 0;
        e.S = e.S | 0;
        return new Date(e.Y, e.M, e.D, e.H, e.m, e.S);
    }

    function ISO8601Duration(str) {
        var match = str.match(xsdurationRegEx);
        var substract = match[1] === '-';
        var fn = function (x) {
                return !x ? x : (substract ? x * -1 : x);
            };
        this.Y = fn(match[years]) | 0;
        this.M = fn(match[months]) | 0;
        this.D = fn(match[days]) | 0;
        this.H = fn(match[hours]) | 0;
        this.m = fn(match[minutes]) | 0;
        this.S = fn(match[seconds]) | 0;
    }
    var add = xsdurationjs.add = function (xs, date) {
            var d = new ISO8601Duration(xs),
                s = {
                    Y: date.getFullYear(),
                    M: date.getMonth() + 1,
                    D: date.getDate(),
                    H: date.getHours(),
                    m: date.getMinutes(),
                    S: date.getSeconds() + (date.getMilliseconds() / 1000)
                };
            return w3calgo(s, d);
        }
})(typeof exports === "undefined" ? xsdurationjs = {} : exports);
