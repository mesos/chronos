define([
  'underscore',
  'views/filterable_view'
],
function(_,
         FilterableView) {
  'use strict';

  var namespace = "_filterable",
      InstanceMethods;

  function get(t, k) {
    if (!(t[namespace] && t[namespace][k])) { return null; }

    return t[namespace][k];
  }

  function set(t, k, v) {
    if (!t[namespace]) { t[namespace] = {}; }
    if (_.isObject(k)) { _.merge(t[namespace], k); }
    else { t[namespace][k] = v; }
  }

  function unset(t, k) {
    if (!t || !t[namespace]) { return; }
    else if (!!k && t[namespace][k]) {
      delete t[namespace][k];
    } else {
      delete t[namespace];
    }
  }

  InstanceMethods = {
    initFilterableView: function() {
      var selColl = this.options.selections || null,
          view    = new FilterableView({collection: selColl});

      set(this, {
        filterView: view
      });

      this.listenTo(this, {
        'close': this._filterableClosed
      });

      this.listenToOnce(this, {
        'render': this._renderFilterableView
      });

      this.listenTo(view, {
        'selections:updated': function() {
          this.trigger('selections:updated');
        }
      });

      return this;
    },

    '$filterContainer': function() {
      return this.$('.graph-filter-container');
    },

    _filterableClosed: function(e) {
      var view = get(this, 'filterView');

      if (view) {
        view.closed(e);
        view.undelegateEvents();
        view.remove();
      }
    },

    _renderFilterableView: function() {
      var $el = this.$filterContainer(),
          view = get(this, 'filterView');

      if (!($el && $el.length)) { return null; }

      $el.append(view.render().$el);
      view.collection.trigger('reset', view.collection, {});
    },

    getSelections: function() {
      var view = get(this, 'filterView'),
          coll = view ? view.collection : null;
      return coll;
    },

    getSelectionScope: function() {
      var view = get(this, 'filterView');

      return view ? view.toMap() : {};
    },

    setTarget: function(id) {
      var view = get(this, 'filterView');

      return view ? view.setTarget(id) : null;
    }

  };

  return {
    InstanceMethods: InstanceMethods
  };

});
