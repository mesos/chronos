/**
 * Results Header View
 */
define([
  'jquery',
  'backbone',
], function($, Backbone) {

  'use strict';

  var ResultsHeaderView = Backbone.View.extend({
    el: '.results-header',

    events: {
      'click .header-last-run': 'toggleLastRun',
      'click .header-name': 'toggleName'
    },

    toggleLastRun: function(event) {
      var $el = $(event.currentTarget);

      $el.find('.toggle').toggle();
      app.resultsCollection.trigger('toggle:lastRun');
    },

    toggleName: function(event) {
      var $el = $(event.currentTarget);

      $el.find('.down').toggle();
      $el.find('.up').toggle();
      app.resultsCollection.trigger('toggle:name');
    }
  });

  return ResultsHeaderView;
});
