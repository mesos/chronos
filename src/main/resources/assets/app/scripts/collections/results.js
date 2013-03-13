/**
 * Results Collection
 *
 */
define([
         'backbone',
         'underscore',
         'collections/jobs'
       ], function(Backbone, _, JobsCollection) {

  var ResultsCollection;

  ResultsCollection = JobsCollection.extend({
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
      var count = 0,
          errors = this.where({'lastRunStatus': 'failure'});

      // _.each(errors, function(d) {
      //   count = count + d;
      // });

      this.errorCount = errors.length;
    }

  });

  return ResultsCollection;
});
