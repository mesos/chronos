/**
 * Base Jobs Collection
 *
 */
define([
  'backbone',
  'underscore',
  'models/dependent_job',
  'models/scheduled_job',
  'components/functor'
], function(Backbone,
            _,
            DependentJobModel,
            ScheduledJobModel,
            Functor) {

  var BaseJobsCollection;

  BaseJobsCollection = Backbone.Collection.extend({

    model: function(attributes, options) {
      if (attributes.parents && attributes.parents.length) {
        return new DependentJobModel(attributes, options);
      } else {
        return new ScheduledJobModel(attributes, options);
      }
    },

    initialize: function() {
      this.listenTo(this, {
        add: this.parseSchedule,
        reset: this.parseAllSchedules,
        'toggle:count': this.toggleCount,
        'toggle:name': this.toggleName
      });
    },

    parseAllSchedules: function(collection) {
      collection.each(this.parseSchedule, this);
    },

    parseSchedule: function(model) {
      model.parseSchedule();
      return this;
    },

    comparator: function(job) {
      return job.getInvocationCount();
    },

    setComparator: function(fn) {
      this.comparator = fn;
      this.sort();
    },

    makeComparatorByAttribute: function(attributeName, reverse) {
      return function(model1, model2) {
        var val1 = model1.get(attributeName).toString(),
            val2 = model2.get(attributeName).toString(),
            compareVal = val1.localeCompare(val2),
            retVal = compareVal / Math.abs(compareVal);

        if ((compareVal === 0) || isNaN(compareVal)) {
          return 0;
        } else {
          return reverse ? (-1 * retVal) : retVal;
        }
      };
    },

    toggleCount: function(countType) {
      if (countType === 'success') {
        this.toggleCountSuccess();
      } else if (countType === 'error') {
        this.toggleCountError();
      }
    },

    toggleCountSuccess: function() {
      var reverse = this.countSuccessDirection === 'up'
      this.setComparator(this.makeComparatorByAttribute('successCount', reverse));

      this.countSuccessDirection = reverse ? 'down' : 'up';
    },

    errCount: function() {
      var ec = this.where({'lastRunStatus': 'failure'}).length;
      return ec;
    },

    toggleCountError: function() {
      var reverse = this.countErrorDirection === 'up'
      this.setComparator(this.makeComparatorByAttribute('errorCount', reverse));

      this.countErrorDirection = reverse ? 'down' : 'up';
    },

    toggleName: function() {
      var reverse = this.sortNameDirection === 'up';
      this.setComparator(this.makeComparatorByAttribute('displayName', reverse));

      this.sortNameDirection = reverse ? 'down' : 'up';
    },

    toggleLastRun: function() {
      var reverse = this.sortLastRunDirection === 'up';
      this.setComparator(this.makeComparatorByAttribute('lastRunStatus', reverse));

      this.sortLastRunDirection = reverse ? 'down' : 'up';
    },

    reverse: function() {
      this.models.reverse();
      this.trigger('filter');
    },

    /**
     * Keys: dayYears
     * Values: counts
     */
    counts: {},

    /**
     * count increments the count of jobs
     * on a given date
     *
     * @param {Int} dayYear Equals day [0,366] + year (2012)
     * @see app.Helpers.dayYear
     * @returns this
     */
    count: function(dayYear) {
      var before = this.counts[dayYear] || 0;
      this.counts[dayYear] = before + 1;
      return this;
    },

    /**
     * getCounts returns an array of the
     * count values.
     * 
     * @returns {Array}
     */
    getCounts: function() {
      return _.values(this.counts);
    },

    /**
     * getDates returns an array of dates.
     * Strips undefineds from the array.
     *
     * @returns {Array}
     */
    getDates: function() {
      var dates = this.pluck('startDate');
      return _.reject(dates, function(d) {
        return _.isUndefined(d);
      });
    },

    /**
     * getRelatedTo returns an object literal with the IDs of
     * related jobs as keys and booleans as values (always true).
     *
     * @param {String} jobName
     *    Name/ID of job to get related jobs for.
     * @param {Number} depth
     *    Depth of `jobName job`'s parents to look through.
     *    Default: 1
     *
     * @return {Object}
     */
    getRelatedTo: function(jobName, depth) {
      var targetJob = this.get(jobName),
          curJobs   = [targetJob];
      depth || (depth === 0 ? depth : (depth = 1));

      return this.reduce(function(memo, job) {
        var name = job.get('name'),
            parents = job.get('parents'),
            isTargetJob = (name === jobName),
            targetIsParent = _.contains(parents, jobName);

        if (isTargetJob || targetIsParent || memo[name]) {
          var list = parents.concat(name);
          _.extend(memo, _.object(list, _.times(list.length, Functor(true))));
        }

        return memo;
      }, {});
    }

  });

  return BaseJobsCollection;
});
