/**
 * Results Header View
 *
 */
define([
         'jquery',
         'backbone',
         'underscore',
         'hbs!templates/results_header_view'
       ],
       function($, Backbone, _, ResultsHeaderViewTpl) {

  var ResultsHeaderView;

  ResultsHeaderView = Backbone.View.extend({
    el: '.results-header',

    template: ResultsHeaderViewTpl,

    events: {
      'click .header-last-run': 'toggleLastRun',
      'click .header-name': 'toggleName'
    },

    render: function() {
      var html = this.template();
      this.$el.html(html);
      return this;
    },

    toggleLastRun: function(event) {
      var $el = $(event.target);

      $el.find('.toggle').toggle();
      app.resultsCollection.trigger('toggle:lastRun');
    },

    toggleName: function(event) {
      var $el = $(event.target);

      $el.find('.down').toggle();
      $el.find('.up').toggle();
      app.resultsCollection.trigger('toggle:name');
    }
  });

  return ResultsHeaderView;
});
