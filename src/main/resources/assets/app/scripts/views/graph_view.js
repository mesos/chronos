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
  'bootstrap/tooltip'
],
function(Backbone,
         _,
         Graph,
         MixableView,
         Filterable,
         GraphViewTpl) {

  var GraphView;

  /*
   * RenderChildren renders related elements into select2 results.
   * this should be bound to a Backbone view.
   *
   * @params {Integer} resultId The ID (name) of a query result.
   * @params {Array}  related A list of related jobs.
   *
   * @returns this
   */
  function RenderChildren(resultId, related) {
    var sel = ['[data-select2-id="', resultId, '"]'].join(''),
        relatedNames = _.chain(related).keys().without(resultId).value();

    this.$(sel).find('ul.children').html(Select2ChildTpl({
      children: relatedNames
    }));

    return this;
  }

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

    render: function() {
      var html = this.template();

      this.$el.html(html);
      this.trigger('render');

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
