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
      'click .header-disabled': 'toggleDisabled',
      'click .header-owner': 'toggleOwner',
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
    },

    toggleDisabled: function(event) {
      var $el = $(event.currentTarget);

      $el.find('.toggle').toggle();
      app.resultsCollection.trigger('toggle:disabled');
    },

    toggleOwner: function(event) {
      var $el = $(event.currentTarget);

      $el.find('.down').toggle();
      $el.find('.up').toggle();
      app.resultsCollection.trigger('toggle:owner');
    }

 });

  return ResultsHeaderView;
});
