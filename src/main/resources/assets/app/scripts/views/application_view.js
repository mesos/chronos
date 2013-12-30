/**
 * Application View
 *
 */
define([
  'jquery',
  'backbone',
  'models/job'
],
function($,
         Backbone,
         JobModel) {

  var ApplicationView;

  ApplicationView = Backbone.View.extend({

    el: '.app',

    events: {
      'click .new-job': 'newJob',
      'click .view-graph': 'showGraph',
      'click .view-alt-graph': 'showGraph',
      'click .total-jobs': 'showAll'
    },

    initialize: function() {
      this.listenTo(app.jobsCollection, {
        add: this.updateJobsCount,
        remove: this.updateJobsCount,
        reset: this.updateJobsCount,
        sync: this.setPersisted
      });
      this.updateJobsCount();
    },

    setPersisted: function(collection, resp, options) {
      collection.each && collection.each(function(job) {
        job.set({persisted: true}, {silent: true});
      });
    },

    newJob: function(event) {
      var model = new JobModel({persisted: false});

      app.detailsCollection.unshift(model, {
        validate: false,
        create: true
      });

      $('.right-pane').scrollTop(0);
    },

    showGraph: function(e) {
      var graphView, targetName, $target, related, graphType;
      e && e.preventDefault() && e.stopPropagation() && e.stopImmediatePropagation();

      $target    = $(e.currentTarget);
      targetName = $target.data('job-id');
      graphType = 'static';
      app.lightbox.showGraphView(graphType, targetName);
    },

    showAll: function() {
      app.resultsCollection.reset(app.jobsCollection.models);
      $('#search-filter').val('')
    },

    updateJobsCount: function() {
      this.$('.all-jobs-count').text(app.jobsCollection.length);
    }

  });

  return ApplicationView;
});
