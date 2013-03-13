/**
 * Base Job Model
 *
 */
define([
  'backbone',
  'underscore',
  'moment',
  'validations/base_job'
],
function(Backbone, _, moment, BaseJobValidations) {

  var BaseWhiteList, BaseJobModel;

  BaseWhiteList = [
    'name', 'command', 'owner', 'async', 'epsilon', 'executor'
  ];

  BaseJobModel = Backbone.Model.extend({
    defaults: function() {
      var d = new Date();
      return {
        name: '-',
        owner: 'ateam@airbnb.com',
        startTime: moment(d).format('HH:mm:ss'),
        startDate: moment(d).format('YYYY-MM-DD'),
        repeats: '',
        duration: 'T24H',
        epsilon: 'PT15M',
        command: '-',
        schedule: '-',
        parents: [],
        retries: 2,
        lastSuccess: '-',
        lastError: '-',
        successCount: 0,
        errorCount: 0,
        persisted: false,
        async: true
      };
    },

    idAttribute: 'name',

    url: function(action) {
      if (action === 'put') {
        return '/scheduler/job/' + encodeURIComponent(this.get('name'));
      }
    },

    initialize: function() {
      this.bindings();
      this.trigger('add');

      return this;
    },

    bindings: function() {
      this.listenTo(this, {
        setSchedule: this.updateSchedule,
        add: this.parseDisplayName,
        'before:validate': this.parseSchedule,
        'change:name': this.parseDisplayName,
        'change:repeats': this.updateSchedule,
        'change:startTime': this.updateSchedule,
        'change:startDate': this.updateSchedule,
        'change:duration': this.updateSchedule,
        'change:lastRunStatus': this.updateLastRunInfo
      });
    },

    hasSchedule: function() {
      return true;
    },

    updateSchedule: function() {
      var repeats = this.get('repeats'),
          startDate = this.get('startDate'),
          startTime = this.get('startTime'),
          duration  = this.get('duration'),
          schedule,
          parts;

      parts = [
        'R' + repeats,
        startDate + 'T' + startTime + 'Z',
        'P' + duration
      ];
      schedule = parts.join('/');

      console.log('schedule', schedule);

      this.set({schedule: schedule}, {silent: true});
    },

    validate: function(attributes, options) {
      return "cannot validate base jobs";
    },

    run: function(options) {
      return this.sync('run', this, options);
    },

    sync: function(method, model, options) {
      var _options,
          syncUrl,
          _method = method;

      if (method === 'run') { _method = 'update'; }
      switch (method) {
        case 'delete':
        case 'run':
          syncUrl = this.url('put');
          options.data = null;
          break;
        default:
          syncUrl = this.url();
          break;
      }

      return Backbone.sync.apply(this, [
        _method,
        model,
        _.extend({}, options, {url: syncUrl})
      ]).done(_.bind(function() {
        this.set('persisted', true);
      }, this));
    },

    isNew: function() {
      return !this.get('persisted');
    },

    clone: function() {
      var m = new BaseJobModel(_.extend({}, this.attributes, {
        persisted: false
      }));
      m.id = null;

      return m;
    },

    toJSON: function() {
      var baseJSON;

      if (this.get('schedule') === '-') {
        this.trigger('setSchedule');
      }

      baseJSON = this.toData();
      return _.pick.apply(null, ([baseJSON]).concat(this.getWhitelist()));
    },

    toData: function() {
      var data = Backbone.Model.prototype.toJSON.call(this);

      return _.extend({}, data, {
        parentsList: this.get('parents').join(', '),
        isNew: this.isNew(),
        hasSchedule: this.hasSchedule(),
        lastError: data.lastError || 'none',
        lastSuccess: data.lastSuccess || 'none'
      });
    },

    updateLastRunInfo: function(model, lastRunStatus, options) {
      var lastRunFailed  = (lastRunStatus === 'failure'),
          lastRunFresh   = (lastRunStatus === 'fresh'),
          lastRunSuccess = (lastRunStatus === 'success');

      model.set({
        lastRunSuccess: !!lastRunSuccess,
        lastRunError: !!lastRunFailed,
        lastRunFresh: !!lastRunFresh,
        lastRunTime: model.get((lastRunFailed ? 'lastError' : 'lastSuccess'))
      });
    },

    updateLeaf: function() {
      return true;
      this.set('leaf', !this.get('parents').length);
      return this;
    },

    getWhitelist: function() {
      return [];
    },

    nextDate: function() {
      var duration = this.get('duration'),
          currentDate = this.get('currentDate'),
          nextDate = xsdurationjs.add(duration, currentDate);

      this.set('currentDate', nextDate);

      return nextDate;
    },

    parentsList: function(parents) {
      return this.get('parents').join(', ');
    },

    parseDisplayName: function() {
      var name = this.get('name');

      this.set('displayName', (name ? name.split('_').join(' ') : ''));
    },

    getInvocationCount: function() {
      return this.get('successCount') + this.get('errorCount');
    },

    validate: _.extend({}, BaseJobValidations),

    parseSchedule: function() {
      return this;
      /*
      var schedule = this.get('schedule'),
          parts;

      if (!schedule) {
        return this;
      }

      parts = schedule.split('/');

      this.set({
        repeats: this.parseRepeats(parts[0]),
        startDate: this.parseStartDate(parts[1]),
        startTime: this.parseStartTime(parts[1]),
        duration: this.parseDuration(parts[2])
      }, {silent: true});

      return this;
     */
    }
  }, {
    getWhitelist: function() {
      return BaseWhiteList.slice();
    }
  });

  return BaseJobModel;
});
