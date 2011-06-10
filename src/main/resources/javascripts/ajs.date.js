/*
 * The relative date functionality is loosely based on John Resig's JavaScript Pretty Date
 * Copyright (c) 2008 John Resig (jquery.com)
 * Licensed under the MIT license.
 *
 */
 
 var AJS = AJS || {};
 AJS.Date = AJS.Date || {};
 
(function() {
    var i18n = {
        'time.past.moment': 'Moments ago',
        'time.past.minute': '1 minute ago',
        'time.past.minutes': '{0} minutes ago',
        'time.past.hour': '1 hour ago',
        'time.past.hours': '{0} hours ago',
        'time.past.day': 'Yesterday',
        'time.past.dayWithTime': 'Yesterday at {0}',
        'time.past.days': '{0} days ago',
        'time.past.weeks': '{0} weeks ago',
        'time.past.year': 'More than a year ago',

        'time.future.minute': '1 minute from now',
        'time.future.minutes': '{0} minutes from now',
        'time.future.hour': '1 hour from now',
        'time.future.hours': '{0} hours from now',
        'time.future.day': 'Tomorrow',
        'time.future.dayWithTime': 'Tomorrow at {0}',
        'time.future.days': '{0} days from now',
        'time.future.weeks': '{0} weeks from now',
        'time.future.year': 'More than a year from now',

        'time.datetime': '{0} at {1}'
    }


    /**
     * Util function for getting an internationalized string via a unique key
     * @method geti18n
     * @param {String} key Key that corresponds to the desired text string
     * @return {String} Internationalized string
     */
    function geti18n(key) {
        return i18n['time.' + key];
    }

    /**
     * Given a Date returns the number of milliseconds since midnight Jan 1, 1970 in UTC.
     * @method getUtcTime
     * @param {Date} date date to get the time of in milliseconds in UTC
     * @return number of milliseconds since midnight Jan 1, 1970 in UTC
     */
    function getUtcTime(date) {
        return date.getTime() + date.getTimezoneOffset() * 60;
    }

    /**
     * Returns a relative past timestamp
     * @method getRelativePastDate
     * @param {Date} date Date representation of the time in question
     * @param {Number} diff The number of seconds since the event occurred
     * @param {Number} day_diff The number of days since the event occurred
     * @param {Boolean} isFineGrained Determines the granularity of the relative date (seconds if true, days if false)
     * @return {String} String containing relative past timestamp
     */
    function getRelativePastDate(date, diff, day_diff, isFineGrained) {
		
		
        if (day_diff == 0) {
            // date is sometime today
            return isFineGrained && (
                diff < 60 && geti18n('past.moment') || // Moments ago
                diff < 120 && geti18n('past.minute') || // 1 minute ago
                diff < 3600 && AJS.format(geti18n('past.minutes'), Math.floor( diff / 60 )) || // N minutes ago
                diff < 7200 && geti18n('past.hour') || // 1 hour ago
                diff < 86400 && AJS.format(geti18n('past.hours'), Math.floor( diff / 3600 )) // N hours ago
                ) || '';
        } else {
			console.log("DT " + date + "year: " + date.toString('MMM'));
            return day_diff <= 1 && (isFineGrained && AJS.format(geti18n('past.dayWithTime'), date.toString('t')) || geti18n('past.day')) || // Yesterday (at HH:MM)
                   day_diff < 7 && (isFineGrained && AJS.format(geti18n('datetime'), date.toString('dddd'), date.toString('H') + ":" + date.toString('m') + date.toString('tt')) || date.toString('dddd')) || // Day of week (at HH:MM)
                   day_diff < 365 && (isFineGrained && AJS.format(geti18n('datetime'), date.toString('MMM') + " " + date.toString('d'), date.toString('H') + ":" + date.toString('m') + date.toString('tt')) || date.toString('MMM') + " " + date.toString('dS')) || // Month DD (at HH:MM)
                   day_diff >= 365 && (isFineGrained && AJS.format(geti18n('datetime'), date.toString('m'), date.toString('H') + ":" + date.toString('m') + date.toString('tt')) || date.toString('y')); // Month DD YYYY (at HH:MM)
        }
    }

    /**
     * Returns a relative future timestamp
     * @method getRelativeFutureDate
     * @param {Date} date Date representation of the time in question
     * @param {Number} diff The number of seconds from now until the event occurs
     * @param {Number} day_diff The number of days from now until the event occurs
     * @param {Boolean} isFineGrained Determines the granularity of the relative date (seconds if true, days if false)
     * @return {String} String containing relative future timestamp
     */
    function getRelativeFutureDate(date, diff, day_diff, isFineGrained) {
        if (day_diff == 0) {
            // date is sometime today
            return isFineGrained && (
                diff < 60 && geti18n('future.minute') || // 1 minute from now
                diff < 3600 && AJS.format(geti18n('future.minutes'), Math.floor( diff / 60 )) || // N minutes from now
                diff < 7200 && geti18n('future.hour') || // 1 hour from now
                diff < 86400 && AJS.format(geti18n('future.hours'), Math.floor( diff / 3600 )) // N hours from now
                ) || '';
        } else {
			console.log("i18n " + geti18n('datetime') );
	
            return day_diff <= 1 && (isFineGrained && AJS.format(geti18n('future.dayWithTime'), date.toString('t')) || geti18n('future.day')) || // Tomorrow (at HH:MM)
                   day_diff < 7 && (isFineGrained && AJS.format(geti18n('datetime'), date.toString('dddd'), date.toString('t')) || date.toString('dddd')) || // Day of week (at HH:MM)
                   day_diff < 365 && (isFineGrained && AJS.format(geti18n('datetime'), date.toString('m'), date.toString('t')) || date.toString('m')) || // Month DD (at HH:MM)
                   day_diff >= 365 && (isFineGrained && AJS.format(geti18n('datetime'), date.toString('m'), date.toString('t')) || date.toString('y')); // Month DD YYYY (at HH:MM)
        }
    }

    /*
     * Take a string representation of a date and parses it into a date object
     * @method parseDate
     * @param {String|Number} time A timestamp in any of ISO, UTC, or string format
     * @return {Date} A date object representing the specified timestamp
     */
    function parseDate(time) {
        var exp = /([0-9])T([0-9])/,
            date = new Date(time);
        // IE will return NaN, FF and Webkit will return 'Invalid Date' object
        if (date && !(date.toString() === "Invalid Date" || date.toString() === 'NaN')) {
            return date;
        }
        if (time.match(exp)) {
            // ISO time.  We need to do some formatting to be able to parse it into a date object
            time = time
                // ignore any thing less than seconds
                .replace(/\.[0-9]*/g,"")
                // for the Date ctor to use UTC time
                .replace(/Z$/, " -00:00")
                // remove 'T' separator
                .replace(exp,"$1 $2");
        }
        // more formatting to make it parseable
        time = time
            // replace dash-separated dates with forward-slash separated
            .replace(/([0-9]{4})-([0-9]{2})-([0-9]{2})/g,"$1/$2/$3")
            // get rid of semicolon and add space in front of timezone offset (for Safari, Chrome, IE6)
            .replace(/\s?([-\+][0-9]{2}):([0-9]{2})/, ' $1$2');
		console.log("Parsed Time:" + time);
        return new Date(time || "");
    }

    /*
     * Given two dates, returns the difference between them in seconds
     * @method getDifference
     * @param {Number|Date} time A timestamp in any of ISO, UTC, or string format
     * @param {Date} now (optional) Date to use as the current date.  Intended primarily for use in unit tests.
     * @return {Number} The difference between the two dates
     */
    function getDifference(time, now) {
        var date = new Date(time),
            diff;
        if (!date) {
            date = parseDate(time);
        }
        now = now || new Date();
        diff = (getUtcTime(now) - getUtcTime(date)) / 1000;
        if (isNaN(diff)) {
            return null;
        }
        return diff;
    }

    /*
     * Takes the difference between two dates (in s) and returns the difference in days
     * @method getDayDifference
     * @param {Number} diff The difference between two dates, in seconds
     * @return {Number} The number of days represented by the specified difference
     */
    function getDayDifference(diff) {
        return Math.floor(Math.abs(diff) / 86400);
    }

    /*
     * Takes a timestamp and returns a relative ("n minutes ago" or "n minutes from now") string representation
     * @method getRelativeDate
     * @param {String|Number} time A timestamp in any of ISO, UTC, or string format
     * @param {Date} now (optional) Date to use as the current date.  Intended primarily for use in unit tests.
     * @return {String} A natural language representation of the timestamp relative to now
     */
    function getRelativeDate(time, now, isFineGrained) {
		
        now = now || new Date();
        var date = parseDate(time),
            diff = getDifference(date, now),
            day_diff;
        if (diff) {
			
            day_diff = getDayDifference(diff);
            if (day_diff == 0 && date.getDate() != now.getDate()) {
                // if it's less than 24 hours but not today, set the day diff to 1
                day_diff = 1;
            }

            if (diff < 0) {
                return getRelativeFutureDate(date, -diff, day_diff, isFineGrained);
            } else {
                return getRelativePastDate(date, diff, day_diff, isFineGrained);
            }
        }
        return null;
    }

    /*
     * Takes a timestamp and returns a string representation with granularity as fine as seconds
     * @method getFineRelativeDate
     * @param {String|Number} time A timestamp in any of ISO, UTC, or string format
     * @param {Date} now (optional) Date to use as the current date.  Intended primarily for use in unit tests.
     * @return {String} A natural language representation of the timestamp, with up to second granularity
     */
    AJS.Date.getFineRelativeDate = AJS.Date.getFineRelativeDate || function(time, now){
        return getRelativeDate(time, now, true);
    };

    /*
     * Takes a timestamp and returns a string representation with granularity as fine as days
     * @method getCoarseRelativeDate
     * @param {String|Number} time A timestamp in any of ISO, UTC, or string format
     * @param {Date} now (optional) Date to use as the current date.  Intended primarily for use in unit tests.
     * @return {String} A natural language representation of the timestamp, with up to day granularity
     */
    AJS.Date.getCoarseRelativeDate = AJS.Date.getCoarseRelativeDate || function(time, now) {
        return getRelativeDate(time, now, false);
    };

    AJS.Date.parse = AJS.Date.parse || parseDate;

})();