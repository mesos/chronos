/** 3-Part ISO8601 Parser **/

{
  var lastParse, Node, IntWrap, IsValid;

  lastParse = new ParserState();
  Node = (function() { return DateNode.create; }());
  IntWrap = (function() { return DateNode.intWrap; }());
  IsValid = (function() { return DateNode.isValid; }());
}

RepeatingInterval 'repeating-interval'
  = 'R' n:Int* '/' i:Interval {
    return Node('RepeatingInterval', {
      repetitions: IntWrap(n, Infinity) || Infinity,
      interval: i
    });
  }

Interval 'interval'
  = start:DateTime '/' rest:(DateTime / Duration) {
    return Node('Interval', {start: start, rest: rest});
  }
  / d:Duration rest:('/' DateTime)? {
    return Node('Interval', {duration: d, rest: rest});
  }

Int 'int'
  = [0-9]

UnsafeIntegers 'plain-integers'
  = digits:Int+ { return IntWrap(digits.join('')); }

Integers '[Integers]'
  = digits:UnsafeIntegers { return IntWrap(digits, 0); }

/*
 * Interval designators
 */
Y 'Years'
  = cnt:Integers 'Y' { return cnt; }
M 'Months'
  = cnt:Integers 'M' { return cnt; }
W 'Weeks'
  = cnt:Integers 'W' { return cnt; }
D 'Days'
  = cnt:Integers 'D' { return cnt; }
H 'Hours'
  = cnt:Integers 'H' { return cnt; }
Mi 'Minutes'
  = cnt:Integers 'M' { return cnt; }
S 'Seconds'
  = cnt:Integers 'S' { return cnt; }

/*
 * Date designators
 */
YYYY '[Date::Year]'
  = sign:[+-] year:(Int Int Int Int Int) {
      return Node('Date:Year', {year: year, sign: sign});
    }
  / year:(Int Int Int Int) {
      return Node('Date:Year', {year: year});
    }

MM '[Date::Month]'
  = month:(ot9 / '1' [0-2]) {
    return Node('Date:Month', {
      month: month
    });
  }

DD '[Date::Day]'
  = day:(ot9 / [1-2] [0-9] / '3' [0-1]) {
    return Node('Date:Day', {
      day: day
    });
  }

ot9
  = '0' [1-9]

zt9
  = '00' / ot9

/*
 * Week date designators
 */
Www '[Date:::Week]'
  = 'W' week:(ot9 / [1-4] [0-9] / '5' [0-3]) { return week; }
DoW '[Date::DayOfWeek]'
  = day:([1-7]) { return day; }

/*
 * Ordinal Date Designator
 */
DDD '[Date::DayOfYear]'
  = ('00' day:[1-9] {
    return day;
  })
  / ('0' day:([1-9][0-9]) {
    return day;
  })
  / (day:([1-3][0-9][0-9]) {
    return day;
  })

/*
 * Time designators
 */
hh '[Time::Hour]'
  = zt9 / '1' [0-9] / '2' [0-4]
mm '[Time::Minute]'
  = zt9 / ([1-5][0-9])
ss '[Time::Second]'
  = (zt9 / [1-5] [0-9] / '60') ('.' [0-9] [0-9] [0-9])?

CalendarDate '[Date::CalendarDate]'
  = m:MM
    rest:(delim:'-'? d:DD?)? !{
      var hadStartMatch = lastParse.hasBalance('Date', '-'),
          makesPair = lastParse.getBalance('Date', '-', rest[0]);

      return !makesPair ||
        (!hadStartMatch &&
          !(DateNode.isValid(rest[0]) || DateNode.isValid(rest[1])));
    } {
      return Node('Date:CalendarDate', {month: m, rest: rest});
    }

WeekDate '[Date::WeekDate]'
  = w:Www d:('-'? DoW)? !{
    var makesPair = lastParse.getBalance('Date', '-', d[0]);
    return !makesPair;
  } {
    return Node('Date:WeekDate', {week: w, dayOfWeek: d});
  }

OrdinalDate '[Date::OrdinalDate]'
  = day:DDD {
    lastParse.forceBalance('Date', '-');
    return Node('Date:OrdinalDate', {dayOfYear: day});
  }

Date '[Date]'
  = year:YYYY (d:'-'? !{
      return lastParse.startBalance('Date', '-', d);
    })
    rest:(CalendarDate / WeekDate / OrdinalDate) {
    return Node('Date', {
      year: year, rest: rest
    });
  }

Time '[Time]'
  = time:LocalTime tz:TimeZone? {
    return Node('Time', {time: time, timeZone: tz});
  }

TimeZone '[Time::TimeZone]'
  = 'Z' { return {offset: 0, sign: 'Z'}; }
  / sign:[+-] &{ return DateNode.isValid(sign); }
    tz:TimeZoneOffset { return {offset: tz, sign: sign}; }

TimeZoneOffset '[Time::TimeZoneOffset]'
  = hours:hh (c1:':'? !{ return lastParse.startBalance('TimeZoneOffset', ':', c1); })
    minutes:mm? {
    return {hours: hours, minutes: minutes};
  }

LocalTime '[Time::LocalTime]'
  = hour:hh
    c1:(c1:':'? !{
      return lastParse.startBalance('LocalTime', ':', c1);
    })
    m:mm?
    c2:':'?
    s:ss? !{
      return ((!!c2 && !!s) &&
        !lastParse.getBalance('LocalTime', ':', c2 && c2[0]));
    } {
      return Node('Time', {hour: hour, minute: m, second: s});
    }

DurationParts '[Duration::Parts]'
  = y:Y? m:M? d:D? rest:('T' h:H? min:Mi? s:S? {
      return Node('DurationParts', {hours: h, minutes: min, seconds: s});
    })? {
      if (IsValid(y) || IsValid(m) || IsValid(d) || IsValid(rest)) {
        return Node('DurationParts', {years: y, months: m, days: d, rest: rest});
      } else {
        return null;
      }
    }
  / w:W {
      return Node('DurationParts', {weeks: w});
    }

Duration '[Duration]'
  = 'P' rest:(DurationParts / DateTime) {
    return Node('Duration', {rest: rest});
  }

DateTime '[Date Time]'
  = d:Date 'T' t:Time {
    return Node('DateTime', {
      date: d,
      time: t
    });
  }

