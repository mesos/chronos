define([
  'jquery',
  'backbone',
  'underscore',
  'backpack',
  'views/graph_view',
  'views/graph_viz_view'
], function($,
            Backbone,
            _,
            Backpack,
            GraphView,
            GraphVizView) {
  'use strict';

  var GraphboxView,
      lbInitialize,
      lbEvents,
      lbRender,
      lbClose,
      slice = Array.prototype.slice;

  lbInitialize = Backpack.Lightbox.prototype.initialize;
  lbEvents     = Backpack.Lightbox.prototype.events;
  lbRender     = Backpack.Lightbox.prototype.render;
  lbClose      = Backpack.Lightbox.prototype.close;

  GraphboxView = Backpack.Lightbox.extend({
    initialize: function() {
      lbInitialize.apply(this, slice.call(arguments));
      this._graphTypes = {
        'static': GraphVizView
      };
    },

    events: _.extend({
      'click [data-job-type]': 'toggleGraph'
    }, lbEvents),

    render: function() {
      lbRender.call(this);
      this.addClass('graph-wrapper');

      return this;
    },

    close: function() {
      this.closeGraphView();
      lbClose.apply(this, slice.call(arguments));

      return this;
    },

    closeGraphView: function() {
      var view = this.model ? this.model.get('content') : null;
      if (view) { view.trigger('close'); }
    },

    showGraphView: function(graphType, targetId, options) {
      var model = this.model,
          oldView = model ? model.get('content') : null,
          newViewClass = this.getRegisteredGraphType(graphType),
          isOpen = model && model.get('open'),
          options = (_(options).isObject() ? options : {}),
          transferSelection = (oldView && isOpen && !targetId),
          newView,
          collection;

      collection = transferSelection ? oldView.getSelections() : null;
      this.closeGraphView();

      if (!!newViewClass) {
        newView = new newViewClass(_.extend({}, {
          selections: collection
        }, options));
        if (collection) { collection.trigger('reset', collection, {}); }
        else if (!!targetId) { newView.setTarget(targetId); }

        this.content(newView).open();
        newView.trigger('show');
      }

      return this;
    },

    toggleGraph: function(e) {
      var $target = $(e.currentTarget),
          jobType = $target.data('job-type'),
          options = $target.data('job-options');

      e.preventDefault();
      this.showGraphView(jobType, null, options);
    },

    getRegisteredGraphType: function(name) {
      return this._graphTypes[name] || null;
    },

    registerGraphType: function(name, classFn) {
      if (_.isObject(name)) {
        _(name).forEach(function(v, k) {
          this.registerGraphType(k, v);
        }, this);
      } else if (_.isString(name)) {
        this._graphTypes[name] = classFn;
      }

      return this;
    }
  });

  return GraphboxView;
});
