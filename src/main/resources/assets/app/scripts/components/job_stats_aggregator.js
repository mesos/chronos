define([
  'underscore'
],
function(_) {
  'use strict';

  /**
   * JobStatsAggregator takes a Backbone.Collection of models with
   * fields 'stats' and 'parents'. JobStatsAggregator will aggregate
   * the time spent in the specified percentile 
   */
  function JobStatsAggregator(collection, statsKey, idKey) {
    this.statsKey = statsKey || '95thPercentile';
    this.idKey    = idKey    || 'name';
    this.processCollection(collection);
  }

  _.extend(JobStatsAggregator.prototype, {
    processCollection: function(coll) {
      if (!!coll) { this.collection = coll; }
      else if (!this.collection) { return this; }

      this.aggregates = {};
      this.collection.forEach(function(model) {
        var id = model.get(this.idKey);
        this.aggregates[id] = this.processTimeFor(model);
      }, this);

    },

    processTimeFor: function(model) {
      var stats     = model.get('stats'),
          id        = model.get(this.idKey),
          parents   = model.get('parents'),
          baseTime  = stats ? stats[this.statsKey] : null,
          otherTime = 0;

      if (parents && parents.length) {
        otherTime = _(parents).reduce(function(maxTime, parentName) {
          var parent = this.collection.get(parentName),
              t      = this.processTimeFor(parent);
          return (t > maxTime) ? t : maxTime;
        }, 0, this);
      }

      return baseTime + otherTime;
    },

    getAggregateFor: function(id) {
      var agg   = _(this.aggregates).has(id) ? this.aggregates[id] : null,
          model = this.collection.get(id),
          stats = (model && model.get('stats')) ? model.get('stats') : {},
          own   = stats[this.statsKey] ? stats[this.statsKey] : null;

      return {
        own: own,
        total: agg
      };
    }
  });

  return JobStatsAggregator;
});
