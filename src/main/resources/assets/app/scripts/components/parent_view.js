define([
  'jquery',
  'underscore'
],
function($, _) {
  'use strict';

  var namespace = "_childViews",
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

  function resolveView(view) {
    if (_.isString(view)) {
      return require(view);
    } else if (_.isFunction(view)) {
      return view;
    } else {
      return null;
    }
  }

  function AddOne(model, collection, options) {
    var views = get(this, 'views'),
        viewOpts  = (options && options.viewOptions),
        view  = views[model.cid],
        viewClassName = get(this, 'viewClassName'),
        ViewClass;

    if (!view) {
      ViewClass = resolveView(viewClassName);
      view = new ViewClass({
        model: model,
        options: viewOpts || {}
      });
      views[model.cid] = view;
    } else {
      view.trigger('change:layout');
    }

    return views[model.cid];
  }

  function RemoveOne(model) {
    var _views = get(this, 'views'),
        view  = _views[model.cid],
        views;

    if (!view) { return; }

    _views[model.cid] = null;
    views = _.omit(_views, model.cid);
    set(this, 'views', views);

    if (_.isFunction(view.terminate)) {
      return view.terminate();
    } else {
      view.undelegateEvents();
      return view.remove();
    }
  }

  function RemoveAll() {
    var views = _.extend({}, get(this, 'views')),
        parentView = this;

    _.each(views, function(v, k) {
      RemoveOne.call(parentView, {cid: k});
    });
  }

  InstanceMethods = {
    initParentView: function(name) {
      if (!!get(this, 'views') || !name) { return false; }
      set(this, {
        views: {},
        viewClassName: name
      });
      return true;
    },

    bindCollectionViews: function(childViewName, collection) {
      if (!this.initParentView(childViewName) || !collection) { return; }

      this.renderChildViews = _.bind(this._renderChildViews, this, collection);

      this.listenTo(collection, {
        add: this._childAdded,
        remove: this._childRemoved,
        reset: this._childrenReset,
        sort: this._childrenSorted
      }).listenTo(this, {
        render: this.renderChildViews
      });

      return this;
    },

    $childViewContainer: function() {
      return this.$el;
    },

    addChildView: function(view) {
      this.$childViewContainer().prepend(view.render().$el);
    },

    _childAdded: function(model, collection, options) {
      var view = AddOne.call(this, model, collection, options),
          eventString = 'parentView:renderChild parentView:renderChild:';

      this.addChildView(view);

      this.trigger(eventString + model.cid, {
        childView: get(this, 'views')[model.cid]
      });

      view.trigger('childView:render', {
        parentView: this
      });

    },

    _childRemoved: function(model, collection, options) {
      RemoveOne.call(this, model, collection, options);
    },

    _childrenReset: function(collection, options) {
      RemoveAll.call(this);
      this.$childViewContainer().html('');
      set(this, 'views', {});
      this.renderChildViews();
    },

    _childrenSorted: function(collection, options) {
      var $container = this.$childViewContainer(),
          views = get(this, 'views'),
          view;

      collection.each(function(model) {
        view = views[model.cid];

        if (!view) { return; }

        view.$el.detach().appendTo($container);
      });
    },

    _renderChildViews: function(collection) {
      var _this = this;

      this.trigger('parentView:beforeRenderChildren');

      // Collect child HTML into a single Array of strings. Rendering to DOM
      // elements too early is expensive with a large number of elements.
      var childrenStrings = new Array(collection.length);
      collection.each(function(model) {
        var view = AddOne.call(_this, model, collection);
        childrenStrings.push(view.toHTML());
      });

      // Render big children String to the DOM.
      this.$childViewContainer().html(childrenStrings.join(''));

      // Iterate over new DOM elements and give their associated views
      // references to them so Rivets can take over updates from here.
      this.$childViewContainer().find('[data-cid]').each(function(index, element) {
        var $el = $(element);
        var view = get(_this, 'views')[$el.data('cid')];

        view.setElement(element);
      });

      this.trigger('parentView:afterRenderChildren');
    },

    unbindCollectionViews: function() {
      RemoveAll.call(this);
      unset(this);

      return this;
    }
  };

  return {
    InstanceMethods: InstanceMethods
  };

});
