/**
 * Graph View
 *
 */
define([
  'backbone',
  'underscore',
  'views/graph',
  'components/mixable_view',
  'components/filterable',
  'hbs!templates/graph_view',
  'bootstrap/tooltip',
  'bootstrap/dropdown'
],
function(Backbone,
         _,
         Graph,
         MixableView,
         Filterable,
         GraphViewTpl) {

  var GraphView;

  GraphView = MixableView.extend({
    mixins: {
      filterable: Filterable.InstanceMethods
    },

    className: 'graph-area lb-graph',

    template: GraphViewTpl,

    events: {},

    initialize: function() {
      this.initFilterableView();
      this.listenTo(this, {
        'show': this.showGraph,
        'selections:updated': this.showGraph
      });
    },

    getTemplateData: function() {
      return {
        currentView: 'static-stats'
      };
    },

    render: function() {
      var html = this.template(this.getTemplateData());

      this.$el.html(html);
      this.trigger('render');
      this.$('[data-toggle="dropdown"]').dropdown();

      return this;
    },

    showGraph: function() {
      var scope = this.getSelectionScope();

      if (_.isEmpty(scope)) { scope = null; }
      Graph.showScoped(scope, app.jobsGraphCollection);
    }

  });

  return GraphView;
});
