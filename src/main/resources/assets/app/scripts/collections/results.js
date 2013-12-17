/**
 * Results Collection
 */
define([
  'collections/base_jobs'
], function(JobsCollection) {

  'use strict';

  var ResultsCollection = JobsCollection.extend({
    initialize: function() {
      this.listenTo(this, {
        change: this.update,
        'toggle:count': this.toggleCount,
        'toggle:lastRun': this.toggleLastRun,
        'toggle:name': this.toggleName
      });
    },

    update: function() {
      this.updateErrorCount();
    },

    updateErrorCount: function() {
      var errors = this.where({'lastRunStatus': 'failure'});

      this.errorCount = errors.length;
    }
  });

  return ResultsCollection;
});
