define([
  'underscore'
],
function(_) {

  var msInSecond = 1000,
      secondsInMinute = 60,
      minutesInHour = 60,
      hoursInDay = 24;

  function formatMS(ms) {
    var results = [],
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
      results.push({days: days}, {hours: h});
    } else if (h > 1.0) {
      results.push({hours: h}, {minutes: m});
    } else if (m > 1.0) {
      results.push({minutes: m}, {seconds: s});
    } else {
      results.push({seconds: s});
    }

    var fmt = _.map(results, function(val, i) {
      var k = _.chain(val).keys().first().value(),
          v = val[k];

      return [parseFloat(v).toFixed(2), k].join(' ');
    });

    return fmt.join(', ');
  }

  return formatMS;
});
