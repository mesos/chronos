define([
  'jquery',
  'underscore',
  'views/bound_view'
],
function($,
         _,
         BoundView) {

  'use strict';

  var MainMenu = BoundView.extend({
    defaults: {
      theme: 'tranquility'
    },

    el: "#main-menu",

    events: {
      'click [data-theme]': 'switchTheme',
      'submit #search-form': 'submitSearchForm'
    },

    getBindModels: function() {
      return {
        jobs: this.collection
      };
    },

    initialize: function(options) {
      this.options = _.extend({}, this.defaults, options);
    },

    render: function() {
      this.$wrapper = this.$el.parents('.chronos-wrapper');

      this.addRivets();
      this.trigger('render');
      return this;
    },

    submitSearchForm: function(e) {
      e && e.preventDefault();
      return false;
    },

    switchTheme: function(e) {
      var $button = $(e.target);
      this.$wrapper.removeClass('chronos-wrapper-' + this.options.theme);

      this.options.theme = $button.data('theme');
      this.$wrapper.addClass('chronos-wrapper-' + this.options.theme);
    }
  });

  return MainMenu;
});
