/**
 * Job View
 *
 */
define([
  'jquery',
  'backbone',
  'underscore',
  'views/bound_view',
  'components/tooltip_view',
  'hbs!templates/job_item_view',
  'bootstrap/tooltip'
],
function($,
         Backbone,
         _,
         BoundView,
         TooltipView,
         JobItemViewTpl) {

  var JobItemView;

  JobItemView = BoundView.extend({

    mixins: {
      tooltips: TooltipView.InstanceMethods
    },

    tagName: 'li',

    className: 'item',

    template: JobItemViewTpl,

    events: {
      'click': 'click'
    },

    initialize: function() {
      this.$el.addClass(this.model.cid);
      this.listenTo(this.model, {
        'change:lastRunStatus': this.render
      }).listenTo(app.detailsCollection, {
        add: this.setActive,
        remove: this.removeActive
      });

      this.addTooltips();
      this.addRivets();
    },

    getBindModels: function() {
      return {
        job: this.model
      };
    },

    render: function() {
      var data = this.model.toData(),
          html = this.template(data);

      this.$el.html(html);
      this.trigger('render', {sync: true});
      if (this.model.get('disabled')) {
        this.$el.addClass('disabled');
      }
      this.setActive();


      return this;
    },

    removeActive: function(model, collection) {
      if (!model) { return; }
      else if (model.id === this.model.id) {
        this.$el.removeClass('active');
      }
    },

    setActive: function(model, collection) {
      var isAdd = model && collection && (model.id === this.model.id),
          active = app.detailsCollection.get(this.model);

      if (isAdd || active) {
        this.$el.addClass('active');
      } else {
        this.removeActive(model);
      }
    },

    click: function(e) {
      var active, $target;

      active = app.detailsCollection.get(this.model.cid);
      $target = $(e.target);

      if ($target.hasClass('ignore')) {
        return;
      } else if (active) {
        app.detailsCollection.remove(this.model);
      } else {
        app.detailsCollection.add(this.model, {at: 0});
      }
    }
  });

  return JobItemView;
});
