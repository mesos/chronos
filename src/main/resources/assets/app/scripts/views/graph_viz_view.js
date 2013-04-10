define([
  'jquery',
  'backbone',
  'underscore',
  'components/duration_humanizer',
  'components/d3_shadowed_text',
  'components/filterable',
  'components/job_stats_aggregator',
  'components/mixable_view',
  'd3',
  'vendor/viz',
  'hbs!templates/graph_view',
  'moment',
  'cs!vendor/dotgraph/dotgraph',
  'vendor/dotgraph/dotparser',
  'bootstrap/tooltip',
  'bootstrap/dropdown'
],
function($,
         Backbone,
         _,
         FormatMS,
         shadowedText,
         Filterable,
         JobStatsAggregator,
         MixableView,
         d3,
         Viz,
         GraphViewTpl,
         moment,
         DotGraph,
         DotParser) {
  'use strict';

  var DOT_PATH = "/scheduler/graph/dot",
      GraphVizView,
      StandardGraphDecorator,
      StatsGraphDecorator;

  function rightClick(data, i) {
    _(['stopImmediatePropagation', 'preventDefault']).each(function(name) {
      if (d3.event && _.isFunction(d3.event[name])) {
        d3.event[name]();
      }
    });

    return false;
  }

  function zoomFn(graph) {
    return function() {
      var t = d3.select(this);
      graph.attr('transform', [
        'translate(',
        d3.event.translate,
        ')scale(',
        d3.event.scale,
        ')'
      ].join(''))
    }
  }

  function GraphDecorator() {}
  _.extend(GraphDecorator, {extend: Backbone.Model.extend});
  _.extend(GraphDecorator.prototype, Backbone.Events, {
    'decorateGraph': function() {
      throw new Error('Decorate graph must be defined by a subclass.');
    }
  });

  StandardGraphDecorator = GraphDecorator.extend({
    decorateGraph: function(svg) {
      svg.selectAll('.node').on('mouseover', function() {
          var jId = $(this).data('job-id'),
              job = app.jobsCollection.get(jId),
              lastRunTime = job.get('lastRunTime'),
              d3_textNode = d3.select(this).select('text'),
              textNode = d3_textNode.node(),
              offset = (textNode.getBBox().height + 5),
              text = [],
              newTextNode;

          if (!lastRunTime) {
            text.push('Job has not run yet');
          } else {
            if (job.get('lastRunError')) {
              text.push('Last error: ');
            } else if (job.get('lastRunSuccess')) {
              text.push('Last success: ');
            }
            text.push(moment(lastRunTime).fromNow());
          }

          shadowedText(d3.select(this), {
            text: text.join(''),
            attributes: {
              'text-anchor': 'middle',
              x: d3_textNode.attr('x'),
              y: (parseInt(d3_textNode.attr('y')) + offset),
              'class': [
                'last-run-time',
                (job.get('lastRunError') ? 'failure' : ''),
                (job.get('lastRunSuccess') ? 'success' : '')
              ].join(' ')
            }
          });
        }).
        on('mouseout', function() {
          d3.select(this).selectAll('.last-run-time').remove();
        });
    }
  });

  StatsGraphDecorator = GraphDecorator.extend({
    decorateGraph: function(svg) {
      app.jobsCollection.forEach(function(job) {
        if (!job.get('stats')) { job.fetchStats(); }
      });

      this.listenTo(app.jobsCollection, 'change:stats',
                    _.bind(this.addText, this, svg));

      this.addText(svg);
    },

    addText: function(svg) {
      var timeSums = {},
          aggregator = new JobStatsAggregator(app.jobsCollection);

      svg.selectAll('.last-run-time').remove();
      svg.selectAll('.node').each(function() {
        var jId = $(this).data('job-id'),
            aggStats = aggregator.getAggregateFor(jId),
            d3_textNode = d3.select(this).select('text'),
            textNode = d3_textNode.node(),
            offset = (textNode.getBBox().height + 5),
            text = '',
            aggregateTime;

        if (aggStats && aggStats.own && aggStats.total) {
          text = [
            FormatMS(aggStats.own, true),
            FormatMS(aggStats.total, true)
          ].join(' / ');
        }

        shadowedText(d3.select(this), {
          text: text,
          attributes: {
            x: d3_textNode.attr('x'),
            y: (parseInt(d3_textNode.attr('y')) + offset),
            'text-anchor': 'middle',
            'class': 'last-run-time'
          }
        });
      });
    }
  });

  GraphVizView = MixableView.extend({
    mixins: {
      filterable: Filterable.InstanceMethods
    },

    className: 'lb-graph graph-viz-view',

    template: GraphViewTpl,

    initialize: function(options) {
      var useStatsDecorator = (this.options.decorator === 'stats');
      this.initFilterableView();
      this.decorators = {
        'graph': (useStatsDecorator ? StatsGraphDecorator : StandardGraphDecorator)
      };
      this.listenTo(this, 'selections:updated', this.renderDotFile);
    },

    getTemplateData: function() {
      var viewType = 'static';
      if (this.options.decorator === 'stats') {
        viewType = 'static-stats';
      }

      return {
        currentView: viewType
      };
    },

    render: function() {
      this.$el.html(this.template(this.getTemplateData()));
      this.trigger('render');
      this.$('[data-toggle="dropdown"]').dropdown();
      this.renderDotFile();

      return this;
    },

    fetchDot: function() {
      return $.ajax({
        context: this,
        dataType: 'text',
        url: DOT_PATH
      });
    },

    filterDot: function(dotData) {
      var scope = this.getSelectionScope(),
          dotAst,
          dotGraph;

      if (_.isEmpty(scope)) { return dotData; }

      dotAst = DotParser.parse(dotData);
      dotGraph = new DotGraph.DotGraph(dotAst);

      _.chain(dotGraph.walk().nodes).reduce(function(memo, node, nId) {
        if (!scope[node.attrs.label]) {
          memo[nId] = node;
        }
        return memo;
      }, {}).forEach(function(node, nId) {
        dotGraph.removeNode(nId);
      }).value();

      return dotGraph.toDot();
    },

    renderDotFile: function() {
      var view = this,
          dotData = this.dotData;

      if (!dotData) {
        this.fetchDot().done(function(data, textStatus) {
          view.dotData = data;
          view.renderDot();
        });
      } else {
        this.renderDot();
      }
    },

    renderDot: function() {
      var svg = Viz(this.filterDot(this.dotData), 'svg'),
          $container = $('.lightbox'),
          width = $container.width(),
          height = $container.height(),
          $svg,
          zoomBehavior,
          graphTransform,
          x,
          y,
          bbox,
          graph;

      this.$('#graph-area').html(svg);

      $svg = d3.select(this.$('svg').get(0)).
        attr('width', width).
        attr('height', height).
        attr('viewBox', null);

      graph = $svg.select('.graph');
      graph.select('polygon').
        attr('fill', 'none').
        attr('stroke', 'none');

      bbox = graph.node().getBBox();
      graphTransform = d3.transform(graph.attr('transform'));
      var graphTranslation = [
        graphTransform.translate[0] + ((width / 2.0) - (bbox.width / 2.0)),
        graphTransform.translate[1] + ((height / 2.0) - (bbox.height / 2.0))
      ];
      graph.attr('transform', _.extend(graphTransform, {
        translate: graphTranslation
      }).toString());

      zoomBehavior = d3.behavior.zoom().
        translate(graphTranslation).
        on('zoom', zoomFn(graph)).
        on('contextmenu', rightClick);

      $svg.call(zoomBehavior);
      this.decorateDot($svg);
    },

    getDecorator: function() {
      if (this.options.decorator === 'stats') {
        return new StatsGraphDecorator;
      } else {
        return new StandardGraphDecorator;
      }
    },

    getDecorators: function() {
      return [this.getDecorator()];
    },

    decorateDot: function(svg) {
      var decorators = this.getDecorators();

      svg.selectAll('title').remove();
      svg.selectAll('.node').each(function() {
        var node = d3.select(this),
            id  = node.select('text').text(),
            job = app.jobsCollection.get(id);

        if (job) { node.attr('data-job-id', job.id) };

        if (job && job.get('lastRunError')) {
          node.classed('fail', true);
        } else if (job && job.get('lastRunSuccess')) {
          node.classed('success', true);
        }
      });

      svg.selectAll('.node').
        on('click', function() {
          var jId = $(this).data('job-id');

          app.lightbox.close();
          app.router.navigateJob(jId);
        });

      _.each(decorators, function(decorator) {
        decorator.decorateGraph(svg);
      });
    }
  });

  return GraphVizView;
});
