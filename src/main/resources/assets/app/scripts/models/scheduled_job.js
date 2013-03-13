/**
 * Scheduled Job Model
 *
 */
define([
  'backbone',
  'underscore',
  'models/base_job',
  'validations/base_job',
  'parsers/iso8601',
  'moment'
],
function(Backbone,
         _,
         BaseJobModel,
         BaseJobValidations,
         Iso8601Parser,
         moment) {

  var ScheduledJobModel, dateParts;

  function Iso8601Validator(str) {
    if (!(this instanceof Iso8601Validator)) { return new Iso8601Validator(str); }
    else if (!str) { return false; }

    this.text = str;
  }

  _.extend(Iso8601Validator.prototype, {
    parseAs: function(type) {
      var parsed;
      type || (type = 'RepeatingInterval');

      try {
        parsed = Iso8601Parser.parse(this.text, type);
      } catch (e) {
        parsed = false;
      }

      return parsed;
    },

    isValid: function() {
      if (!!this.parseAs()) { return true; }
    },

    getErrors: function() {
      var errors = {},
          strParts = this.text.split('/');

      if (this.isValid()) { return {}; }

      if (!strParts[1] || !this.parseAs.call({text: strParts[1]}, 'DateTime')) {
        _.extend(errors, {startDate: true, startTime: true});
      }
      if (!strParts[2] || !this.parseAs.call({text: strParts[2]}, 'Duration')) {
        errors.duration = true;
      }
      if (_.isEmpty(errors)) {
        errors.repeats = true;
      }

      return errors;
    }
  });

  ScheduledJobModel = BaseJobModel.extend({
    constructor: function(attrs, options) {
      BaseJobModel.prototype.constructor.call(this, attrs, options);
    },

    url: function(action) {
      return BaseJobModel.prototype.url.call(this, action) ||
        '/scheduler/iso8601';
    },

    parse: function(attrs, options) {
      var schedule;

      if (!attrs && !_.isEmpty(this.attributes)) {
        return attrs;
      } else if (!(attrs && attrs.schedule)) {
        throw new Error('Scheduled jobs must have schedules');
      }

      schedule = attrs.schedule;
      return _.merge({}, attrs, ScheduledJobModel.scheduleToParts(schedule));
    },

    hasSchedule: function() {
      return true;
    },

    getWhitelist: function() {
      return (['schedule', 'epsilon']).concat(BaseJobModel.getWhitelist());
    },

    parseSchedule: function() {
      var schedule = this.get('schedule'),
          scheduleParts = ScheduledJobModel.scheduleToParts(schedule);

      this.set(scheduleParts, {silent: true});
    },

    validate: _.extend({}, BaseJobValidations, {
      schedule: {
        custom: 'validateSchedule'
      },
      epsilon: {
        custom: 'validateEpsilon'
      }
    }),

    validateSchedule: function(attributeName, attributeValue) {
      var validator = new Iso8601Validator(attributeValue);

      if (!validator.isValid()) {
        return validator.getErrors();
      }
    },

    validateEpsilon: function(attributeName, attributeValue) {
      var parsed;
      try {
        parsed = Iso8601Parser.parse(attributeValue, 'Duration');
      } catch (e) {
        parsed = null;
      }

      if (!parsed) {
        return 'Epsilon is invalid. It should follow the same format as "Duration".';
      }
    }
  }, {
    scheduleToParts: function(schedule) {
      var parts;

      if (!schedule) { return {}; }

      parts = schedule.split('/');

      return {
        repeats: ScheduledJobModel.parseRepeats(parts[0]),
        startDate: ScheduledJobModel.parseStartDate(parts[1]),
        startTime: ScheduledJobModel.parseStartTime(parts[1]),
        duration: ScheduledJobModel.parseDuration(parts[2])
      };
    },

    parseRepeats: function(repeats) {
      var num = repeats.split('R').join('');
      return num || '';
    },

    parseStartDate: function(rawDate) {
      var date = new Date(rawDate);

      if (date == 'Invalid Date') {
        date = '-';
      }

      return moment(date).format('YYYY-MM-DD');
    },

    parseStartTime: function(time) {
      if (time == undefined) {
        return 'undefined';
      }

      return time.split('T')[1].split('Z')[0];
    },

    parseDuration: function(duration) {
      return duration ? duration.slice(1) : '-';
    }
  });

  return ScheduledJobModel;
});
