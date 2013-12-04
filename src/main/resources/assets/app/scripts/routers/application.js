/**
 * Application Router
 */
define([
  'jquery',
  'backbone',
  'underscore',
  'views/application_view',
  'views/jobs_collection_view',
  'views/job_detail_collection_view',
  'views/main_menu',
  'views/graphbox_view'
],
function($,
        Backbone,
        _,
        ApplicationView,
        JobsCollectionView,
        JobDetailCollectionView,
        MainMenuView,
        GraphboxView) {

  var ApplicationRouter = Backbone.Router.extend({
    routes: {
      ''           : 'index',
      'jobs/*path' : 'showJob'
    },

    initialize: function() {
      window.app || (window.app = {});

      _.extend(window.app, {
        applicationView: new ApplicationView({
          collection: window.app.jobsCollection
        }).render(),

        jobsCollectionView: new JobsCollectionView({
          collection: window.app.resultsCollection
        }),

        detailsCollectionView: new JobDetailCollectionView({
          collection: window.app.detailsCollection
        }),

        mainMenuView: new MainMenuView({
          collection: window.app.jobsCollection
        }).render()
      });

      window.app.lightbox = new GraphboxView();
      window.app.resultsCollection.trigger('reset');

      window.app.jobsCollectionView.$el.tooltip({
        container: window.app.applicationView.$el,
        selector: '[data-toggle="tooltip"]'
      });
    },

    navigateJob: function(jobName) {
      this.navigate('jobs/' + jobName, {trigger: true});
    },

    index: function() {
      app.detailsCollection.reset();
    },

    showJob: function(path) {
      app.detailsCollection.deserialize(path);
    }
  });

  return ApplicationRouter;
});
