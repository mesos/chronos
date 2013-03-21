define([
  'jquery',
  'underscore',
  'd3',
  'moment',
  'components/d3_shadowed_text'
],
function($,
         _,
         d3,
         moment,
         shadowedText) {
  'use strict';
  var svg,
      force1,
      graph,
      r,
      markerWidth,
      markerHeight,
      xOffset,
      yOffset,
      forceGravity,
      forceDistance,
      forceCharge;

  function deg(rads) { return rads * (180.0 / Math.PI); }

  forceGravity = 0.1; //0.01;
  forceDistance = 200;
  forceCharge  = -1000.0;

  markerWidth  = 10;
  markerHeight = 10;
  yOffset      = -5;
  xOffset      = 0;
  r            = 20;

  function safeUpdate(scope, jobGraph) {
    if ($('.graph-wrapper svg').length < 1) {
      init();
    } else {
      d3.selectAll('#graph-area svg g').remove();
    }

    return update(scope, jobGraph);
  };

  function init() {
    var $graph = $('.graph-wrapper'),
        width = $('#graph-area').width(),
        height = $graph.height(),
        defs, assignMarker;

    svg = d3.select('#graph-area').append('svg:svg')
      .attr('width', width)
      .attr('height', height);

    /*
     * Define the arrowhead marker style.
     *
     * viewBox:       (x, y, width, height)
     * refX, refY:    point on the arrowhead that connects to the line.
     * orient='auto': point the arrowhead in the direction of the line.
     *
     */
    defs = svg.append('svg:defs');

    assignMarker = function AssignMarker(xOffset, yOffset, markerWidth, markerHeight, r) {
      return function(markers) {
        _.each(markers, function(marker) {
          _.each(marker, function(el) {
            d3.select(el).attr('viewBox', function() {
                return _.map([
                  xOffset, yOffset,
                  markerWidth, markerHeight
                ], String).join(' ');
              })
              .attr('refX', markerWidth + xOffset + (r / 2))
              .attr('refY', ((markerHeight / 2) + yOffset))
              .attr('markerWidth', markerWidth)
              .attr('markerHeight', markerHeight)
              .attr('orient', 'auto')
            .append('svg:path')
              .attr('d', function() {
                return [
                  'M', xOffset, ',', yOffset,
                  ' ', 'L', xOffset, ',', (markerHeight + yOffset),
                  ' ', 'L', (markerWidth + xOffset), ',', ((markerHeight / 2) + yOffset),
                  ' ', 'L', xOffset, ',', yOffset,
                  ' ', 'Z'
                ].join('');
              });
          });
        });
      };
    };

    defs.selectAll('marker')
        .data(['arrowhead', 'arrowhead-hover'])
      .enter().append('svg:marker')
        .attr('id', String)
        .call(assignMarker(xOffset, yOffset, markerWidth, markerHeight, r));

    defs.select('#arrowhead-hover')
        .call(assignMarker(xOffset, yOffset, markerWidth, markerHeight, r * 1.65));

    force1 = d3.layout.force()
      .gravity(forceGravity)
      .distance(forceDistance)
      .charge(forceCharge)
      .size([width, height]);

    return graph;
  }

  function update(scope, jobGraph) {
    var related = {},
        anchorNodes = [],
        anchorLinks = [],
        linkGroup,
        lookup,
        parts, nodes, links,
        link, node;

    nodes  = jobGraph.getPlainNodes({scope: scope});
    lookup = _.reduce(nodes, function(memo, n) {
      memo[n.name] = n;
      return memo;
    }, {});

    links = _.chain(jobGraph.getPlainPaths({scope: scope})).map(function(p) {
      return _.extend(p, {
        source: lookup[p.sourceName],
        target: lookup[p.targetName]
      });
    }).filter(function(p) {
      return !!p.source && !!p.target;
    }).value();

    force1.
        nodes(nodes).
        charge(function(d, i) {
          var targetName = d.name;

          if (!!lookup[targetName]) {
            return -1000;
          } else {
            return -100;
          }
        }).
        links(links).
        start();

    linkGroup = svg.selectAll('.link')
        .data(force1.links())
      .enter().append('svg:g').
        attr('class', function(d) {
          var _classes = d.fake ? ['fake-link'] : [];

          return _classes.concat([
            'link',
            'source-' + d.source.id,
            'target-' + d.target.id
          ]).join(' ');
        });

    link = linkGroup.append('svg:line').
      attr('class', 'link').
      attr('marker-end', 'url(#arrowhead)');

    node = svg.selectAll('.node').data(force1.nodes())
      .enter().append('svg:g').attr({
        'class': 'node',
        'data-job-id': function(d) {
          return d.name;
        }
      }).call(force1.drag);

    node.append('svg:circle').
      attr({
        'class': function(d){
          if (d.fail) return 'fail';
        },
        'cx': function(d) { return 0; -1 * (r / 2); },
        'cy': function(d) { return 0; -1 * (r / 2); },
        'r':  function(d) { return r; }
      }).
        on('click', click).
        on('contextmenu', rightClick).
        on('mouseover', mouseOver).
        on('mouseout', mouseOut);

    var text = node.append('svg:g').attr('class', function(d) {
      return ['text ', 'text-', d.id].join('')
    });

    shadowedText(text, {
      attributes: {
        dx: r,
        dy: '0.35em'
      },
      text: function(d) {
        return d.name;
      }
    });

    shadowedText(text, {
      attributes: {
        dx: r,
        dy: '1.45em',
        'class': 'subtitle inv-subtitle'
      },
      text: function(d) {
        if (!d.date) {
          return 'Has never ran';
        } else {
          var descriptor = d.fail ? 'error' : 'success';
          return [
            'Last', descriptor + ':', moment(d.date).fromNow()
          ].join(' ');
        }
      }
    });

    shadowedText(text, {
      attributes: {
        dx: r,
        dy: '1.45em',
        'class': 'subtitle'
      },
      text: function(d) {
        if (!d.date) {
          return 'Has never ran';
        } else {
          var descriptor = d.fail ? 'error' : 'success';
          return [
            'Last', descriptor + ':', d.date
          ].join(' ');
        }
      }
    });

    force1.on('tick', function() {
      link.attr('x1', function(d) { return d.source.x; })
          .attr('y1', function(d) { return d.source.y; })
          .attr('x2', function(d) { return d.target.x; })
          .attr('y2', function(d) { return d.target.y; });

      node.attr('transform', function(d) { return 'translate(' + d.x + ',' + d.y + ')'; });
    });

    function mouseOver(d, i) {
      var name = '.target-' + d.id,
          text = '.text-' + d.id;

      if ($(name).hasClass('hover')) { return; }

      d3.select(this.parentNode).classed('hover', true);
      console.log('added hover class to', this.parentNode, 'this is', this);

      d3.selectAll(name).
        classed('hover', true).
        select('.link').transition().
          attr('marker-end', 'url(#arrowhead-hover)');

      d3.select(text).transition().
        attr('transform', function(d) {
          return 'translate(' + (r * 1.65) + ', 0)';
        });

      d3.select(this).transition().attr('r', r * 1.65);

      lookup[d.name].isMouseovered = true;
    };

    function mouseOut(d, i) {
      var name = '.target-' + d.id,
          text = '.text-' + d.id;

      d3.select(this.parentNode).classed('hover', false);

      d3.selectAll(name).
        classed('hover', false).
        select('.link').transition().
          attr('marker-end', 'url(#arrowhead)');

      d3.select(text).transition().
        attr('transform', 'translate(0,0)');

      d3.select(this).transition().attr('r', r);

      lookup[d.name].isMouseovered = false;
    };

    function click(node) {
      app.lightbox.close();
      app.router.navigateJob(node.name);
    }

    function rightClick(data, i) {
      _(['stopImmediatePropagation', 'preventDefault']).each(function(name) {
        if (d3.event && _.isFunction(d3.event[name])) {
          d3.event[name]();
        }
      });

      return false;
    }

  }

  function stop() {
    force1 && force1.stop();
  }

  function start() {
    force1 && force1.start();
  }

  return graph = {
    showScoped: safeUpdate,
    start: start,
    stop: stop
  };

});
