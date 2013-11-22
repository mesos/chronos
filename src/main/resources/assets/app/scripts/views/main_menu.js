define([
  'jquery',
  'backbone',
  'views/bound_view',
  'hbs!templates/main_menu'
],
function($,
         Backbone,
         BoundView,
         MainMenuTpl) {

  var MainMenu = BoundView.extend({
    el: "#main-menu",
    template: MainMenuTpl,

    events: {
      'click #btn-bg-prowess': 'paintItProw',
      'click #btn-bg-tranquility': 'paintItTranquil',
      'submit #search-form': 'submitSearchForm'
    },

    paintItProw: function() {
      this.$wrapper.removeClass("chronos-wrapper-tranquility");
      this.$wrapper.addClass("chronos-wrapper-prowess");
    },

    paintItTranquil: function() {
      this.$wrapper.removeClass("chronos-wrapper-prowess");
      this.$wrapper.addClass("chronos-wrapper-tranquility");
    },

    getBindModels: function() {
      return {
        jobs: this.collection
      };
    },

    render: function() {
      var html = this.template();
      this.$el.html(html);
      this.$wrapper = this.$el.parents(".chronos-wrapper");

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
