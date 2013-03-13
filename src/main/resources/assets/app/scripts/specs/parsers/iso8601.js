define([
  'underscore',
  'jquery',
  'mocha',
  'chai',
  'parsers/iso8601',
  'components/date_node',
  'json!specs/test_data.json'
],
function(_,
         $,
         _mocha,
         Chai,
         Iso8601Parser,
         DateNode,
         TestData) {

  var expect = Chai.expect,
      Assertion = Chai.Assertion,
      parse  = Iso8601Parser.parse,
      slice  = Array.prototype.slice,
      Generators;

  function join() {
    return slice.call(arguments).join('');
  }

  function WriteTestData(name, values) {
    var $el = $('#test-data'),
        content = {};

    content[name] = values;

    if ($el.val().length > 0) {
      _.merge(content, JSON.parse($el.val()));
    }

    $el.val(JSON.stringify(content));
  }

  Chai.use(function(chai, Utils) {
    function IsNode(v) {
      return (v instanceof DateNode.__super__.constructor);
    }

    function IsParserResult(v) {
      return !!_.result(v, 'original') && !!_.result(v, 'parsed');
    }

    Assertion.addMethod('parseableAs', function (parseRule) {
      var obj = this._obj,
          error,
          parseResult;

      new Assertion(this._obj).to.be.a('string');

      try {
        parseResult = parse(obj, parseRule);
      } catch (e) {
        error = _.pick(e,
          'name', 'expected', 'found', 'message', 'offset', 'line', 'column');
      }

      this.assert(
        !error,
        "expected #{this} to be parseable as #{exp} but got #{act}",
        "expected #{this} to not be parseable, but got #{act}",
        parseRule,
        !!error ? JSON.stringify(error) : parseResult
      );

      this._obj = {
        original: obj, parsed: parseResult
      };

      return this;
    });

    Assertion.overwriteMethod('respondTo', function(_super) {
      return function(methodName, arg) {
        var obj = Utils.flag(this, 'object');

        if (IsNode(obj)) {
          var _method = obj[methodName],
              fnAssertion, wAssertion;

          new chai.Assertion(_method).to.be.a('function');

          if (_.isObject(arg) && _.has(arg, 'with')) {
            new chai.Assertion(_method.call(obj)).to.be.eql(arg['with']);
          }
        } else if (IsParserResult(obj)) {
          var ass = new chai.Assertion(obj.parsed);
          ass.to.respondTo.apply(ass, arguments);
        } else {
          _super.apply(this, arguments);
        }
      };
    });
  });

  function T(msg, obj) {
    return ([
      msg, '- testing', _.size(obj), 'objects'
    ]).join(' ');
  }

  function PaddedNumRange(start, end, width, prefix) {
    width || (width = (end + '').length);
    prefix || (prefix = '');

    var steps = _.chain(1).range(width).map(function(padding) {
      var zeros = (new Array(padding + 1)).join('0');
      return {
        i: parseInt(1 + zeros, 10),
        z: zeros
      };
    }).sortBy(function(v) { return v.i; }).value();

    function format(val) {
      var result, zeros;
      result = _.chain(steps).filter(function(v) {
        return v.i > val;
      }).max(function(v) {
        return v.i;
      }).value();

      result = (Math.abs(result) === Infinity) ? null : result;
      if (!!result) {
        var len = result.i.toString().length - val.toString().length;

        return [
          prefix,
          _.first(result.z, len).join(''),
          val
        ].join('');
      } else {
        return [prefix, val].join('');
      }

        //find(steps, function(v) {
        //return (v.i > val) && ([v.z, val].join('').length <= width);
      //});

      return prefix + (!!result ? (result.z + val) : ('' + val));
    }

    return _.chain(start).range(end + 1).map(function(i) {
      return {
        str: format(i), int: i
      };
    }).value();
  }

  function MakeDates(lists, separator) {
    separator || (separator = '');

    function WithRest(a) {
      if (a.length === 1) { return a[0]; }
      else if (a.length > 1) {
        var rest = WithRest(a.slice(1));
        return rest.reduce(function(memo, item) {
          return memo.concat(_.first(a).map(function(w) {
            return {str: [w.str, item.str].join(separator)};
          }));
        }, []);
      }
    }

    return WithRest(lists);
  }

  Generators = (function () {
    var methods = {};

    methods.calendarYears = _.memoize(function() {
      var Y1 = {str: '1984', int: 1984},
          Y2 = {str: '0010', int: 10},
          Y3 = {str: '+00501', int: 501},
          Y4 = {str: '-00501', int: -501},
          Y5 = {str: '-10000', int: -10000};
      return {
        standard: [Y1, Y2],
        extended: [Y3, Y4, Y5],
        all: [Y1, Y2, Y3, Y4, Y5]
      };
    });

    methods.calendarMonths = _.memoize(function() {
      var months = PaddedNumRange(1, 12, 2);

      return {
        standard: months, extended: [], all: months
      };
    });

    methods.calendarDaysOfMonth = _.memoize(function() {
      var days = PaddedNumRange(1, 31);

      return {
        standard: days, extended: [], all: days
      };
    });

    methods.calendarWeeks = _.memoize(function() {
      var weeks = PaddedNumRange(1, 53, 2, 'W');

      return {
        standard: weeks, extended: [], all: weeks
      };
    });

    function makeResult(std, extended, all) {
      extended || (extended = []);
      all || (all = std);
      return {standard: std, extended: extended, all: all};
    }

    methods.calendarDaysOfWeek = _.memoize(function() {
      var days = PaddedNumRange(1, 7);
      return makeResult(days, [], days);
    });

    methods.calendarDaysOfYear = _.memoize(function() {
      var days = PaddedNumRange(1, 365, 3);
      return makeResult(days, [], days);
    });

    methods.time || (methods.time = {});
    methods.time.hours = _.memoize(function() {
      var hours = PaddedNumRange(0, 24);
      return makeResult(hours, [], hours);
    });

    methods.time.minutes = _.memoize(function() {
      var minutes = PaddedNumRange(0, 59);
      return makeResult(minutes, [], minutes);
    });

    methods.time.seconds = _.memoize(function() {
      var s = PaddedNumRange(0, 60);
      return makeResult(s, [], s);
    });

    methods.time.zones = _.memoize(function() {
      var tz = ['Z'];
      (['+', '-']).forEach(function(sign) {
        _.range(0, 10).forEach(function(i) {
        });
      });
    });

    return methods;
  }());

  describe('ISO-8601 parts', function() {
    var calendarYears, calendarMonths, calendarDaysOfMonth,
        calendarDaysOfWeek, calendarWeeks, calendarDaysOfYear;

    var timeHours, timeMinutes, timeSeconds, timeZones;

    before(function() {
      /**
      calendarYears = Generators.calendarYears();
      calendarMonths = Generators.calendarMonths();
      calendarDaysOfMonth = Generators.calendarDaysOfMonth();
      calendarDaysOfWeek = Generators.calendarDaysOfWeek();
      calendarWeeks = Generators.calendarWeeks();
      calendarDaysOfYear = Generators.calendarDaysOfYear();
      var obj = {
        parts: {
          years: calendarYears,
          months: calendarMonths,
          daysOfMonth: calendarDaysOfMonth,
          weeks: calendarWeeks,
          daysOfYear: calendarDaysOfYear
        }
      };
      WriteTestData('dates', obj);
      **/
      calendarYears = TestData.dates.parts.years;
      calendarMonths = TestData.dates.parts.months;
      calendarDaysOfMonth = TestData.dates.parts.daysOfMonth;
      calendarWeeks = TestData.dates.parts.weeks;
      calendarDaysOfYear = TestData.dates.parts.daysOfYear;
    });

    describe('Dates', function() {

      describe('Years part', function() {
        it('should properly parse standard YYYY', function() {
          var parseResult;

          calendarYears.standard.forEach(function(y, i) {
            expect(y.str).to.be.parseableAs('YYYY').
              and.to.respondTo('getValue', {'with': y.int});
          }, this);
        });

        it('should properly parse extended [+-]Y?YYYY', function() {
          var parseResult;

          calendarYears.extended.forEach(function(y, i) {
            expect(y.str).to.be.parseableAs('YYYY').
              and.to.respondTo('getValue', {'with': y.int});
          }, this);
        });
      });

      describe('Month of year part', function() {
        it('should properly parse MM', function() {
          var parseResult;

          calendarMonths.all.forEach(function(m, i) {
            var parseResult = expect(m.str).to.be.parseableAs('MM');
          });
        });
      });

      describe('Day of month part', function() {
        it('should properly parse DD', function() {
          var parseResult;

          calendarDaysOfMonth.all.forEach(function(d, i) {
            var parseResult = expect(d.str).to.be.parseableAs('DD');
          });
        });
      });

      describe('Calendar Dates', function() {
        var cDateFullDash, cDateFull, cDateDash, cDate;

        before(function() {
          /**
          cDateFullDash = MakeDates([
            calendarYears.all,
            calendarMonths.all,
            calendarDaysOfMonth.all
          ], '-');

          cDateFull = MakeDates([
            calendarYears.all,
            calendarMonths.all,
            calendarDaysOfMonth.all
          ]);

          cDateDash = MakeDates([
            calendarYears.all,
            calendarMonths.all
          ], '-');

          cDate = MakeDates([
            calendarYears.all,
            calendarMonths.all
          ]);

          var obj = {
            calendar: {
              dateFullDash: cDateFullDash,
              dateFull: cDateFull,
              dateDash: cDateDash,
              date: cDate
            }
          };

          WriteTestData('dates', obj);
         **/
          cDateFullDash = TestData.dates.calendar.dateFullDash;
          cDateFull = TestData.dates.calendar.dateFull;
          cDateDash = TestData.dates.calendar.dateDash;
          cDate = TestData.dates.calendar.date;
        });

        describe('with dashes', function() {
          it('should parse full dates', function() {

            cDateFullDash.forEach(function(date) {
              expect(date.str).to.be.parseableAs('Date');
            }, this);
          });

          it('should parse dates without [DD]', function() {
            cDateFull.forEach(function(date) {
              expect(date.str).to.be.parseableAs('Date');
            }, this);
          });
        });

        describe('without dashes', function() {
          it('should parse full dates', function() {
            cDateFull.forEach(function(date) {
              expect(date.str).to.be.parseableAs('Date');
            }, this);
          });

          it('should NOT parse dates without [DD]', function() {
            cDate.forEach(function(date) {
              expect(date.str).not.to.be.parseableAs('Date');
            }, this);
          });
        });
      });

      describe('Week Dates', function() {
        var wDateFullDash, wDateFull, wDateDash, wDate;

        before(function() {
          /**
          wDateFullDash = MakeDates([
            calendarYears.all,
            calendarWeeks.all,
            calendarDaysOfWeek.all
          ], '-');
          wDateFull = MakeDates([
            calendarYears.all,
            calendarWeeks.all,
            calendarDaysOfWeek.all
          ]);
          wDateDash = MakeDates([
            calendarYears.all,
            calendarWeeks.all
          ], '-');
          wDate = MakeDates([
            calendarYears.all,
            calendarWeeks.all
          ]);

          var obj = {
            week: {
              dateFullDash: wDateFullDash,
              dateFull: wDateFull,
              dateDash: wDateDash,
              date: wDate
            }
          };

          WriteTestData('dates', obj);
          **/
          wDateFullDash = TestData.dates.week.dateFullDash;
          wDateFull = TestData.dates.week.dateFull;
          wDateDash = TestData.dates.week.dateDash;
          wDate = TestData.dates.week.date;
        });

        describe('with dashes', function() {
          it('should parse full dates', function() {

            wDateFullDash.forEach(function(date) {
              expect(date.str).to.be.parseableAs('Date');
            }, this);
          });

          it('should parse dates without [D]', function() {
            wDateFull.forEach(function(date) {
              expect(date.str).to.be.parseableAs('Date');
            }, this);
          });
        });

        describe('without dashes', function() {
          it('should parse full dates', function() {
            wDateFull.forEach(function(date) {
              expect(date.str).to.be.parseableAs('Date');
            }, this);
          });

          it('should parse dates without [D]', function() {
            wDate.forEach(function(date) {
              expect(date.str).to.be.parseableAs('Date');
            }, this);
          });
        });
      });

      describe('Ordinal Dates', function() {
        var oDateDash, oDate;

        before(function() {
          /**
          oDateDash = MakeDates([
            calendarYears.all,
            calendarDaysOfYear.all
          ], '-');
          oDate = MakeDates([
            calendarYears.all,
            calendarDaysOfYear.all
          ]);

          var obj = {
            ordinal: {
              dateDash: oDateDash,
              date: oDate
            }
          };

          WriteTestData('dates', obj);
          **/
          oDateDash = TestData.dates.ordinal.dateDash;
          oDate = TestData.dates.ordinal.date;
        });

        describe('with dashes', function() {
          it('should parse full dates', function() {

            oDateDash.forEach(function(date) {
              expect(date.str).to.be.parseableAs('Date');
            }, this);
          });
        });

        describe('without dashes', function() {
          it('should parse full dates', function() {
            oDate.forEach(function(date) {
              expect(date.str).to.be.parseableAs('Date');
            }, this);
          });
        });
      });
    });

    before(function() {
      timeHours = Generators.time.hours();
      timeMinutes = Generators.time.minutes();
      timeSeconds = Generators.time.seconds();
      timeZones = Generators.time.zones();
    });

    describe('Times', function() {
      var hh_mm_ss, hh_mm, hhmmss, hhmm, hh,
          hh_mm_ss_tz, hh_mm_tz, hhmmss_tz, hhmm_tz, hh_tz;

      before(function() {
        /**
        hh_mm_ss = MakeDates([
          timeHours.all,
          timeMinutes.all,
          timeSeconds.all
        ], ':');
        hh_mm = MakeDates([
          timeHours.all,
          timeMinutes.all
        ], ':');
        hhmmss = MakeDates([
          timeHours.all,
          timeMinutes.all,
          timeSeconds.all
        ]);
        hhmm = MakeDates([
          timeHours.all,
          timeMinutes.all
        ]);
        hh = MakeDates([
          timeHours.all
        ]);

        var obj = {
          week: {
            hh_mm_ss: hh_mm_ss,
            hh_mm: hh_mm,
            hhmmss: hhmmss,
            hhmm: hhmm,
            hh: hh
          }
        };

        WriteTestData('times', obj);
        **/

        hh_mm_ss = TestData.times.week.hh_mm_ss;
        hh_mm = TestData.times.week.hh_mm;
        hhmmss = TestData.times.week.hhmmss;
        hhmm = TestData.times.week.hhmm;
        hh = TestData.times.week.hh;
      });

      describe('without timezones', function() {

        describe('with colons', function() {
          it(T('should parse hh:mm:ss', hh_mm_ss), function() {

            hh_mm_ss.forEach(function(time) {
              expect(time.str).to.be.parseableAs('Time');
            }, this);
          });

          it(T('should parse hh:mm', hh_mm), function() {
            hh_mm.forEach(function(time) {
              expect(time.str).to.be.parseableAs('Time');
            }, this);
          });
        });

        describe('without colons', function() {
          it(T('should parse hhmmss', hhmmss), function() {
            hhmmss.forEach(function(time) {
              expect(time.str).to.be.parseableAs('Time');
            }, this);
          });

          it(T('should parse hhmm', hhmm), function() {
            hhmm.forEach(function(time) {
              expect(time.str).to.be.parseableAs('Time');
            }, this);
          });

          it(T('should parse hh', hh), function() {
            hh.forEach(function(time) {
              expect(time.str).to.be.parseableAs('Time');
            }, this);
          });
        });
      });

      var datesWithTimezones;

      before(function() {
        /**
        var timezones = ['Z'];
        var tzFormats = {'-': [], '+': []};

        (['+', '-']).forEach(function(sign) {
          ([hh_mm, hhmm, hh]).forEach(function(list, j) {
            tzFormats[sign][j] = [];

            list.forEach(function(item, k) {
              tzFormats[sign][j].push(_.extend({}, item, {
                str: [sign, item.str].join('')
              }));
            });
          });
        });

        timezones = timezones.concat.apply(timezones,
          _.chain(tzFormats).values().flatten());

        function mergeTimezone(timeVal, tz) {
          return _.extend({}, timeVal, {
            str: [timeVal.str, tz.str].join(''),
            tz: tz.str
          });
        }

        function makeTzTime(times, tzs) {
          return times.reduce(function(memo, item) {
            tzs.forEach(function(tz) {
              memo.push(mergeTimezone(item, tz));
            });
            return memo;
          }, []);
        }

        hh_mm_ss_tz = makeTzTime(hh_mm_ss, timezones);
        hh_mm_tz = makeTzTime(hh_mm, timezones);
        hhmmss_tz = makeTzTime(hhmmss, timezones);
        hhmm_tz = makeTzTime(hhmm, timezones);
        hh_tz = makeTzTime(hh, timezones);

        var obj = {
          week: {
            hh_mm_ss_tz: hh_mm_ss_tz,
            hh_mm_tz: hh_mm_tz,
            hhmmss_tz: hhmmss_tz,
            hhmm_tz: hhmm_tz,
            hh_tz: hh_tz
          }
        };
        WriteTestData('time_zones', obj);
        **/

        hh_mm_ss_tz = TestData.time_zones.week.hh_mm_ss_tz;
        hh_mm_tz = TestData.time_zones.week.hh_mm_tz;
        hhmmss_tz = TestData.time_zones.week.hhmmss_tz;
        hhmm_tz = TestData.time_zones.week.hhmm_tz;
        hh_tz = TestData.time_zones.week.hh_tz;
      });

      describe('with timezones', function() {

        describe('just timzones', function() {
          it('should parse Z', function() {
            expect('Z').to.be.parseableAs('TimeZone');
          });

          it(T('should parse +hh:mm', hh_mm), function() {
            hh_mm.forEach(function(time) {
              expect(join('+', time.str)).to.be.parseableAs('TimeZone');
            }, this);
          });

          it(T('should parse -hh:mm', hh_mm), function() {
            hh_mm.forEach(function(time) {
              expect(join('-', time.str)).to.be.parseableAs('TimeZone');
            }, this);
          });

          it(T('should parse +hhmm', hh_mm), function() {
            hhmm.forEach(function(time) {
              expect(join('+', time.str)).to.be.parseableAs('TimeZone');
            }, this);
          });

          it(T('should parse -hhmm', hh_mm), function() {
            hhmm.forEach(function(time) {
              expect(join('-', time.str)).to.be.parseableAs('TimeZone');
            }, this);
          });

          it(T('should parse +hh', hh), function() {
            hh.forEach(function(time) {
              expect(join('+', time.str)).to.be.parseableAs('TimeZone');
            });
          });

          it(T('should parse -hh', hh), function() {
            hh.forEach(function(time) {
              expect(join('-', time.str)).to.be.parseableAs('TimeZone');
            });
          });
        });

        describe('combined timezones and times', function() {

          describe('with colons', function() {
            it(T('should parse hh:mm:ss + tz', hh_mm_ss_tz), function() {

              hh_mm_ss_tz.forEach(function(time) {
                expect(time.str).to.be.parseableAs('Time');
              }, this);
            });

            it(T('should parse hh:mm + tz', hh_mm_tz), function() {
              hh_mm_tz.forEach(function(time) {
                expect(time.str).to.be.parseableAs('Time');
              }, this);
            });
          });

          describe('without colons', function() {
            it(T('should parse hhmmss + tz', hhmmss_tz), function() {
              hhmmss_tz.forEach(function(time) {
                expect(time.str).to.be.parseableAs('Time');
              }, this);
            });

            it(T('should parse hhmm + tz', hhmm_tz), function() {
              hhmm_tz.forEach(function(time) {
                expect(time.str).to.be.parseableAs('Time');
              }, this);
            });

            it(T('should parse hh + tz', hh_tz), function() {
              hh_tz.forEach(function(time) {
                expect(time.str).to.be.parseableAs('Time');
              }, this);
            });
          });
        });
      });
    });

    describe('Date Times', function() {
      var oDates, calendarDates, weekDates, times, tzTimes;

      before(function() {
        //dashed: TestData.dates.ordinal.dateDash,
        //normal: TestData.dates.ordinal.date
        oDates = TestData.dates.ordinal;

        //cDateFullDash = TestData.dates.calendar.dateFullDash;
        //cDateFull = TestData.dates.calendar.dateFull;
        //cDateDash = TestData.dates.calendar.dateDash;
        //cDate = TestData.dates.calendar.date;
        calendarDates = TestData.dates.calendar;

        //wDateFullDash = TestData.dates.week.dateFullDash;
        //wDateFull = TestData.dates.week.dateFull;
        //wDateDash = TestData.dates.week.dateDash;
        //wDate = TestData.dates.week.date;
        weekDates = TestData.dates.week;

        //hh_mm_ss = TestData.times.week.hh_mm_ss;
        //hh_mm = TestData.times.week.hh_mm;
        //hhmmss = TestData.times.week.hhmmss;
        //hhmm = TestData.times.week.hhmm;
        //hh = TestData.times.week.hh;
        times = TestData.times.week;

        //hh_mm_ss_tz = TestData.time_zones.week.hh_mm_ss_tz;
        //hh_mm_tz = TestData.time_zones.week.hh_mm_tz;
        //hhmmss_tz = TestData.time_zones.week.hhmmss_tz;
        //hhmm_tz = TestData.time_zones.week.hhmm_tz;
        //hh_tz = TestData.time_zones.week.hh_tz;
        tzTimes = TestData.time_zones.week;
      });

      describe('with ordinal dates', function() {
        var dates = oDates;

        describe('with timezones', function() {
          it('should parse', function() {
            _.each(dates, function(dateList, k) {
              dateList.forEach(function(date) {
                _.each(tzTimes, function(tzList, tzKey) {
                  tzList.forEach(function(tzTime) {
                    expect(join(date, 'T', tzTime)).to.be.parseableAs('DateTime');
                  }, this);
                }, this);
              }, this);
            }, this);
          });
        });

        describe('without timezones', function() {
          it('should parse', function() {
            _.each(dates, function(dateList, k) {
              dateList.forEach(function(date) {
                _.each(times, function(tzList, tzKey) {
                  tzList.forEach(function(tzTime) {
                    expect(join(date, 'T', tzTime)).to.be.parseableAs('DateTime');
                  }, this);
                }, this);
              }, this);
            }, this);
          });
        });
      });

      describe('with calendar dates', function() {
        var dates = calendarDates;

        describe('with timezones', function() {
          it('should parse', function() {
            _.each(dates, function(dateList, k) {
              dateList.forEach(function(date) {
                _.each(tzTimes, function(tzList, tzKey) {
                  tzList.forEach(function(tzTime) {
                    expect(join(date, 'T', tzTime)).to.be.parseableAs('DateTime');
                  }, this);
                }, this);
              }, this);
            }, this);
          });
        });

        describe('without timezones', function() {
          it('should parse', function() {
            _.each(dates, function(dateList, k) {
              dateList.forEach(function(date) {
                _.each(times, function(tzList, tzKey) {
                  tzList.forEach(function(tzTime) {
                    expect(join(date, 'T', tzTime)).to.be.parseableAs('DateTime');
                  }, this);
                }, this);
              }, this);
            }, this);
          });
        });
      });

      describe('with week dates', function() {
        var dates = weekDates;

        describe('with timezones', function() {
          it('should parse', function() {
            _.each(dates, function(dateList, k) {
              dateList.forEach(function(date) {
                _.each(tzTimes, function(tzList, tzKey) {
                  tzList.forEach(function(tzTime) {
                    expect(join(date, 'T', tzTime)).to.be.parseableAs('DateTime');
                  }, this);
                }, this);
              }, this);
            }, this);
          });
        });

        describe('without timezones', function() {
          it('should parse', function() {
            _.each(dates, function(dateList, k) {
              dateList.forEach(function(date) {
                _.each(times, function(tzList, tzKey) {
                  tzList.forEach(function(tzTime) {
                    expect(join(date, 'T', tzTime)).to.be.parseableAs('DateTime');
                  }, this);
                }, this);
              }, this);
            }, this);
          });
        });
      });

    });

    describe('Durations', function() {
      var oDates, calendarDates, weekDates, times, tzTimes;

      before(function() {
        //dashed: TestData.dates.ordinal.dateDash,
        //normal: TestData.dates.ordinal.date
        oDates = TestData.dates.ordinal;

        //cDateFullDash = TestData.dates.calendar.dateFullDash;
        //cDateFull = TestData.dates.calendar.dateFull;
        //cDateDash = TestData.dates.calendar.dateDash;
        //cDate = TestData.dates.calendar.date;
        calendarDates = TestData.dates.calendar;

        //wDateFullDash = TestData.dates.week.dateFullDash;
        //wDateFull = TestData.dates.week.dateFull;
        //wDateDash = TestData.dates.week.dateDash;
        //wDate = TestData.dates.week.date;
        weekDates = TestData.dates.week;

        //hh_mm_ss = TestData.times.week.hh_mm_ss;
        //hh_mm = TestData.times.week.hh_mm;
        //hhmmss = TestData.times.week.hhmmss;
        //hhmm = TestData.times.week.hhmm;
        //hh = TestData.times.week.hh;
        times = TestData.times.week;

        //hh_mm_ss_tz = TestData.time_zones.week.hh_mm_ss_tz;
        //hh_mm_tz = TestData.time_zones.week.hh_mm_tz;
        //hhmmss_tz = TestData.time_zones.week.hhmmss_tz;
        //hhmm_tz = TestData.time_zones.week.hhmm_tz;
        //hh_tz = TestData.time_zones.week.hh_tz;
        tzTimes = TestData.time_zones.week;
      });

      describe('as PnYnMnDTnHnMnS', function() {
        var firstHalves, secondHalves, together;

        before(function() {
          var n = 10;

          var fHalves, sHalves;

          fHalves = _.chain(n).range().map(function(i) {
            return (['Y', 'M', 'D']).reduce(function(memo, sig) {
              var v = _.random(0, n - 1);
              if (v > 0) { memo.push(join(v, sig)) }
              return memo;
            }, []);
          });

          sHalves = _.chain(n).range().map(function(i) {
            return join.apply(null, (['H', 'M', 'S']).reduce(function(memo, sig) {
              var v = _.random(0, i);
              if (v > 0) { memo.push(join(v, sig)) }
              return memo;
            }, []));
          });

          firstHalves = fHalves.map(function(h) {
            return join.apply(null, (['P']).concat(h));
          });
          secondHalves = sHalves.map(function(h) {
            return join.apply(null, (['P', 'T']).concat(h));
          });

          together = fHalves.reduce(function(memo, fh) {
            sHalves.forEach(function(sh) {
              memo.push(join.apply(null, (['P']).concat(fh).concat('T', sh)));
            });
            return memo;
          }, []);
        });

        describe('full specifier', function() {
          it('should parse', function() {
            together.forEach(function(d) {
              expect(d).to.be.parseableAs('Duration');
            });
          });

          it('should parse just first half', function() {
            firstHalves.forEach(function(d) {
              expect(d).to.be.parseableAs('Duration');
            });
          });

          it('should parse just second half', function() {
            secondHalves.forEach(function(d) {
              expect(d).to.be.parseableAs('Duration');
            });
          });
        });
      });

      describe('as PnW', function() {
        _.range(1, 53).forEach(function(i) {
          expect(join('P', i, 'W')).to.be.parseableAs('Duration');
        }, this);
      });

      describe('as P<date>T<time>', function() {
        var n = 20;

        describe('with week dates', function() {
          it('should parse', function() {
            _.chain(weekDates).first(n).forEach(function(date) {
              _.each(tzTimes, function(tzList, tzKey) {
                tzList.forEach(function(tzTime) {
                  expect(join('P', date.str, 'T', tzTime.str)).to.be.parseableAs('Duration');
                }, this);
              }, this);
            }, this);
          });
        });

        describe('with ordinal dates', function() {
          it('should parse', function() {
            _.chain(oDates).first(n).forEach(function(date) {
              _.each(tzTimes, function(tzList, tzKey) {
                tzList.forEach(function(tzTime) {
                  expect(join('P', date.str, 'T', tzTime.str)).to.be.parseableAs('Duration');
                }, this);
              }, this);
            }, this);
          });
        });

        describe('with calendar dates', function() {
          it('should parse', function() {
            _.chain(calendarDates).first(n).forEach(function(date) {
              _.each(tzTimes, function(tzList, tzKey) {
                tzList.forEach(function(tzTime) {
                  expect(join('P', date.str, 'T', tzTime.str)).to.be.parseableAs('Duration');
                }, this);
              }, this);
            }, this);
          });
        });
      });
    });

    describe('Time intervals', function() {
      var formats = {
        '<start>/<end>': '2007-03-01T13:00:00Z/2008-05-11T15:30:00Z',
        '<start>/<duration>': '2007-03-01T13:00:00Z/P1Y2M10DT2H30M',
        '<duration>/<end>': 'P1Y2M10DT2H30M/2008-05-11T15:30:00Z'
      };

      _.each(formats, function(v, k) {
        it(join('should parse', k), function() {
          expect(v).to.be.parseableAs('Interval');
        });
      }, this);
    });

    describe('Repeating intervals', function() {
      var formats = {
        'Rnn/<interval>': 'R5/2008-03-01T13:00:00Z/P1Y2M10DT2H30M',
        'R/<interval>': 'R/P1Y2M10DT2H30M/2008-05-11T15:30:00Z'
      };

      _.each(formats, function(v, k) {
        it(join('should parse', k), function() {
          expect(v).to.be.parseableAs('RepeatingInterval');
        });
      }, this);
    });

    return true;
  });
});
