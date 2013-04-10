define([
  'underscore'
],
function(_) {
  'use strict';

  var msInSecond = 1000,
      secondsInMinute = 60,
      minutesInHour = 60,
      hoursInDay = 24;

  function formatMS(ms, shortenUnit) {
    var results = [],
        shortenUnit = shortenUnit || false,
        x, s, m, h, days;

    if (!_.isNumber(ms)) { return '0'; }
    x = ms / msInSecond;

    s = x % secondsInMinute;
    x = x / secondsInMinute;

    m = x % minutesInHour;
    x = x / minutesInHour;

    h = x % hoursInDay;
    x = x / hoursInDay;

    days = x;
    if (days > 1.0) {
      results.push({days: days});
    } else if (h > 1.0) {
      results.push({hours: h});
    } else if (m > 1.0) {
      results.push({minutes: m});
    } else {
      results.push({seconds: s});
    }

    var fmt = _.map(results, function(val, i) {
      var k = _.chain(val).keys().first().value(),
          v = val[k],
          result = [parseFloat(v).toFixed(2)];

      result.push((shortenUnit ? k.substring(0, 1) : k));
      return result.join(' ');
    });

    return fmt.join(', ');
  }

  return formatMS;
});
