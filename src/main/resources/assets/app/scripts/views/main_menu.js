define([
  'jquery',
  'backbone',
  'underscore',
  'views/bound_view',
  'hbs!templates/main_menu'
],
function($,
         Backbone,
         _,
         BoundView,
         MainMenuTpl) {

  var MainMenu;

  MainMenu = BoundView.extend({
    el: "#main-menu",
    template: MainMenuTpl,

    events: {
      'submit #search-form': 'submitSearchForm'
    },

    getBindModels: function() {
      return {
        jobs: this.collection
      };
    },

    render: function() {
      var html = this.template();
      this.$el.html(html);

      this.addRivets();
      this.trigger('render');

      return this;
    },

    submitSearchForm: function(e) {
      e && e.preventDefault();
      return false;
    }
  });

  return MainMenu;
});
