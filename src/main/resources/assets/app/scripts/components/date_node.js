define([
  'underscore',
  'backbone',
  'moment'
],
function(_, Backbone, moment) {

  var slice = Array.prototype.slice,
      BareNode, NodeTable,
      DateTime, DateYear, DateMonth, DateDay,
      TimeInterval, Duration, DurationParts,
      IDate, Time, CalendarDate, WeekDate,
      OrdinalDate, RepeatingInterval;

  function flatString(v) {
    if (isListOfStr(v)) {
      return v.join('');
    } else if (_.isFunction(v.toString)) {
      return v.toString();
    } else {
      return '' + v;
    }
  }

  function isValid(value) {
    return (!!value || (value === 0)) && (value !== '');
  }

  function IntWrap(value, _default) {
    if (isValid(value)) {
      if (_.isArray(value) && _.isFunction(value.join)) {
        return IntWrap(value.join(''), _default);
      } else if (_.isString(value)) {
        return parseInt(value, 10);
      } else if (_.isNumber(value)) {
        return value;
      }
    } else {
      return _default;
    }
  }

  function CallIf(v, context, val) {
    if (_.isFunction(v)) {
      return v.apply(context, slice.call(arguments, 2));
    } else { return val; }
  }

  function Wrap(value, _default, wrapFn) {
    return isValid(value) ? CallIf(wrapFn, null, value) : _default;
  }

  function isListOfStr(val) {
    return _.chain(val).toArray().every(_.isString).value();
  }

  function BaseNode() {}

  BaseNode.prototype.getValue = function BaseNode_getValue() {
    return null;
  };

  BaseNode.prototype.toMap = function BaseNode_getMap() {
    return {};
  };

  BaseNode.prototype.set = function BaseNode_set(valName, val) {
    this._attributes || (this._attributes = {});

    if (_.isObject(valName)) {
      _.merge(this._attributes, valName);
    } else if (_.isString(valName)) {
      this._attributes[valName] = val;
    }

    return this;
  };

  BaseNode.prototype.get = function BaseNode_get(attrName) {
    this._attributes || (this._attributes = {});

    return this._attributes[attrName];
  }

  BaseNode.prototype.pick = function BaseNode_get() {
    this._attributes || (this._attributes = {});

    return _.pick.apply(null, ([this._attributes]).concat(slice.call(arguments)));
  }

  function isA(v, fn) {
    return !!(v instanceof fn);
  };

  BaseNode.extend = Backbone.Model.extend;

  DateYear = BaseNode.extend({
    constructor: function DateYear(valueMap) {
      if (isValid(valueMap.sign) && (valueMap.sign === '-')) {
        this.sign = -1;
      } else {
        this.sign = 1;
      }

      this.baseYear = flatString(valueMap.year);
    },
    getValue: function() {
      return IntWrap(this.baseYear) * this.sign;
    },
    toString: function() {
      return this.getValue() + '';
    }
  }, {});

  DateMonth = BaseNode.extend({
    constructor: function DateMonth(valueMap) {
      this.set({
        'original': valueMap.month,
        'month': valueMap.month
      });
    },

    toString: function() {
      return this.get('month');
    }
  });

  DateDay = BaseNode.extend({
    constructor: function DateDay(valueMap) {
      this.set({
        'original': valueMap.day,
        'day': valueMap.day
      });
    },
    toString: function() {
      return this.get('day');
    }
  });

  TimeInterval = BaseNode.extend({
    constructor: function TimeInterval(valueMap) {
      var start    = valueMap.start,
          duration = valueMap.duration,
          rest     = valueMap.rest;

      if (start && rest) {
        return TimeInterval.CreateWithStart(start, rest);
      } else if (duration && rest) {
        return TimeInterval.CreateWithDuration(duration, rest);
      }

      this.set({
        start: valueMap.start,
        end:   valueMap.end,
        original: _.pick(valueMap, 'start', 'end', 'original')
      });
    }
  }, {
    CreateWithStart: function(start, rest) {
      var end;

      if (isA(rest, DateTime)) {
        end = rest;
      } else if (isA(rest, Duration)) {
        end = start.add(rest);
      }

      return new TimeInterval({
        start: start, end: end, original: {start: start, rest: rest}
      });
    },
    CreateWithDuration: function(duration, _rest) {
      var rest = (_.isArray(_rest) ? _.last(_rest) : _rest),
          start;

      if (isA(rest, DateTime)) {
        start = rest.subtract(duration);
      }

      return new TimeInterval({
        start: start, end: rest, original: {end: rest, duration: duration}
      });
    }
  });

  DateTime = BaseNode.extend({
    constructor: function DateTime(valueMap) {
      this.date = valueMap.date;
      this.time = valueMap.time;
    },

    compareTo: function(v) {
      if (!isA(v, DateTime)) {
        return null;
      } else {
        return 0;
      }

    },

    mapParts: function(fn, ctx) {
      return _.map([this.date, this.time], fn, (ctx || this));
    },

    toString: function() {
      return this.mapParts(function(v) { return v.toString(); }).join('T');
    },

    toDate: function() {
      return Date.parse(this.toString());
    },

    toMoment: function() {
      var moments = this.mapParts(function(v) {
        return v.toMoment();
      });
    },

    add: function(val) {
      if (isA(val, DateTime)) {
        return (this.compareTo(val) >= 0) ? this : val;
      } else if (isA(val, Duration)) {
        var str  = this.toString(),
            date = new Date(str),
            m    = moment(date),
            inc;

        inc = _.reduce(val.getParts(), function(memo, part, partName) {
          return memo.add(partName, part);
        }, m);

        return new DateTime({
          date: null,
          time: null
        });
      }
    },

    subtract: function(val) {
      if (isA(val, Duration)) {
        var duration = val.toMoment();

        var str  = this.toString(),
            date = new Date(str),
            dateParts = val.getParts(),
            m    = moment(date),
            inc;

        inc = _.reduce(val.getParts(), function(memo, part, partName) {
          return memo.subtract(partName, part);
        }, m);

        return new DateTime({
          date: null,
          time: null
        });
      }
    }
  });

  Duration = BaseNode.extend({
    constructor: function Duration(valueMap) {
      if (!!valueMap.rest) {
        this.setNormalized(valueMap.rest);
      }
    },

    setNormalized: function(value) {
      if (isA(value, DurationParts)) {
        this.parts = value;
      }
    },

    getParts: function() {
      return !!(this.parts && this.parts.toMap) ? this.parts.toMap() : {};
    },

    toMoment: function() {
      return moment.duration(this.getParts());
    }
  });

  DurationParts = (function() {
    var props = ['years', 'months', 'days', 'hours', 'minutes', 'seconds'];

    return BaseNode.extend({
      constructor: function DurationParts(valueMap) {
        var rest  = valueMap.rest || {};

        _.each(props, function(prop) {
          if (!!valueMap[prop]) {
            this[prop] = valueMap[prop];
          } else if (!!rest[prop]) {
            this[prop] = rest[prop];
          } else {
            this[prop] = 0;
          }
        }, this);
      },
      toMap: function() {
        return _.pick.apply(null, ([this]).concat(props));
      }
    });
  }());

  IDate = BaseNode.extend({
    delimeter: '',
    strFields: ['year'],

    constructor: function IDate(valueMap) {
      if (!!valueMap.rest && isA(valueMap.rest, IDate)) {
        return valueMap.rest.merge({year: valueMap.year});
      }

      this.merge(_.omit(valueMap, 'rest'));
    },
    toString: function() {
      var strValues = _.chain(this.getToStrFields()).compact().map(function(field) {
        return field.toString();
      }).flatten().value();
      return strValues.join(this.delimeter);
    },
    getToStrFields: function() {
      return _.map(this.strFields, function(p) { return this[p]; }, this);
    },
    merge: function(valueMap) {
      return _.extend(this, valueMap);
    }
  });

  Time = BaseNode.extend({
    constructor: function Time(valueMap) {
      if (valueMap.timeZone) {
        this.set(_.extend({},
          valueMap.time.pick('original', 'hour', 'minute', 'second'), {
            timeZone: valueMap.timeZone
          }));

      } else {
        var mins = IntWrap(valueMap.minute, 0),
            s    = valueMap.second && valueMap.second.length,
            seconds = s && _.map(valueMap.second, function(v) {
              if (_.isString(v)) {
                return v.length ? v : '0'
              } else {
                return v.join('').replace(/[\.\/]/, '');
              }
            }).join('.');

        this.set({
          original: _.pick(valueMap, 'hour', 'minute', 'second'),
          hour: IntWrap(valueMap.hour, 0),
          minute: mins ? IntWrap(mins, 0) : 0,
          second: seconds ? IntWrap(seconds, 0) : 0
        });
      }

    },
    toString: function() {
      var tz = this.get('timeZone'),
          vals;

      vals = _.map(['hour', 'minute', 'second'], function(attr) {
        return this.get(attr) || 0;
      }, this).join(':');

      if (!!tz) {
        if (!!tz.offset) {
          vals += ([tz.sign, tz.offset]).join('');
        } else {
          vals += tz.sign;
        }
      }

      return vals;
    }
  });

  CalendarDate = IDate.extend({
    type: 'CalendarDate',
    strFields: ['year', 'month', 'day'],

    constructor: function CalendarDate(valueMap) {
      var day = _.last(valueMap && valueMap.rest);

      if (!!day) { this.day = day; }
      this.month = valueMap.month;
    }
  });

  WeekDate = IDate.extend({
    type: 'WeekDate',
    strFields: ['year', 'week', 'dayOfWeek'],

    constructor: function WeekDate(valueMap) {
      this.merge(_.omit(valueMap, 'rest'));
    }
  });

  OrdinalDate = IDate.extend({
    type: 'OrdinalDate',
    strFields: ['year', 'day'],

    constructor: function OrdinalDate(valueMap) {
      this.merge(_.omit(valueMap, 'rest'));
    }
  });

  RepeatingInterval = BaseNode.extend({
    constructor: function RepeatingInterval(valueMap) {
      this.set(_.pick(valueMap, 'repetitions', 'interval'));
    }
  });

  NodeTable = {
    'DateTime': DateTime,
    'Duration': Duration,
    'DurationParts': DurationParts,
    'Date:Day': DateDay,
    'Date:Month': DateMonth,
    'Date:Year': DateYear,
    'Interval': TimeInterval,
    'Date': IDate,
    'Time': Time,
    'Date:CalendarDate': CalendarDate,
    'Date:WeekDate': WeekDate,
    'Date:OrdinalDate': OrdinalDate,
    'RepeatingInterval': RepeatingInterval
  };

  BareNode = BaseNode.extend({
    constructor: function (type, valueMap) {
      this.value = _.extend({}, valueMap, {type: type});
    },
    getValue: function BNodeGetValue() {
      return this.value;
    }
  }, {
    create: function BNodeCreate(type, valueMap) {
      var recognized = !!NodeTable[type];

      if (recognized) {
        return new NodeTable[type](valueMap);
      } else {
        return new BareNode(type, valueMap);
      }
    },
    isValid: function(v) {
      return isValid(v);
    },
    intWrap: function() {
      return IntWrap.apply(null, slice.call(arguments));
    }
  });

  return BareNode;
});
