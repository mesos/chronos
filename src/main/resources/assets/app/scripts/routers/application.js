/**
 * Application Router
 *
 */
define([
         'jquery',
         'backbone',
         'underscore',
         'views/application_view',
         'views/jobs_collection_view',
         'views/job_detail_collection_view',
         'views/graph_view',
         'backpack'
       ],
       function($,
                Backbone,
                _,
                ApplicationView,
                JobsCollectionView,
                JobDetailCollectionView,
                GraphView,
                Backpack) {

  var ApplicationRouter;

  ApplicationRouter = Backbone.Router.extend({
    routes: {
      ''           : 'index',
      'jobs/*path' : 'showJob'
    },

    initialize: function() {
      window.app || (window.app = {});

      _.extend(window.app, {
        applicationView: new ApplicationView({
          collection: window.app.jobsCollection
        }),

        jobsCollectionView: new JobsCollectionView({
          collection: window.app.resultsCollection
        }),

        detailsCollectionView: new JobDetailCollectionView({
          collection: window.app.detailsCollection
        })
      });

      window.app.resultsCollection.on('change', function() {
        $('.failed-jobs-count').html(this.errorCount);
        $('.fresh-jobs-count').html(this.freshCount);
      }, window.app.resultsCollection).trigger('reset')

      window.app.lightbox = new Backpack.Lightbox();

      $('.all-jobs-count').html(window.app.resultsCollection.size()); 

      $('#search-form').on('submit', function(event){
        event.preventDefault();
        return false;
      });
    },

    index: function() {
      app.detailsCollection.reset();
      app.resultsCollection.reset(app.jobsCollection.models);
    },

    graph: function() {
      console.log('graph')
      var graphView = new GraphView();

      app.lightbox
        .addClass('graph-wrapper')
        .content(graphView)
        .open();

      app.graph.init();
    },

    showJob: function(path) {
      app.detailsCollection.deserialize(path);
    }
  });

  return ApplicationRouter;
});
