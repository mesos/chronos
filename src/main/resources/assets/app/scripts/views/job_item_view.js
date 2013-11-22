/**
 * Job View
 */
define([
  'views/bound_view',
  'components/tooltip_view',
  'hbs!templates/job_item_view',
  'bootstrap/tooltip'
],
function(BoundView,
         TooltipView,
         JobItemViewTpl) {

  var JobItemView = BoundView.extend({

    mixins: {
      tooltips: TooltipView.InstanceMethods
    },

    tagName: 'li',

    className: 'item',

    template: JobItemViewTpl,

    initialize: function() {
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

      this.$el.attr('data-cid', this.model.cid);
      this.$el.html(html);
      this.trigger('render', {sync: true});

      return this;
    }
  });

  return JobItemView;
});
