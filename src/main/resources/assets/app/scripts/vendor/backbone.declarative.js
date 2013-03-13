//Wrapped in an outer function to preserve global this
(function (root) { var amdExports; define(['backbone'], function () { (function () {

(function (global) {
  var Backbone = global.Backbone
    , _ = global._;

  if (!_) {
    _ = typeof require !== 'undefined' && require('underscore');
    if (!_) throw new Error('Can\'t find underscore');
  }

  if (!Backbone) {
    Backbone = typeof require !== 'undefined' && require('backbone');
    if (!Backbone) throw new Error('Can\'t find Backbone');
  }

  var _View = Backbone.View
    , viewMethods = {
        model: {}
      , collection: {}
      };

  Backbone.View = _View.extend({
    constructor: function () {
      _View.apply(this, Array.prototype.slice.call(arguments));
      this.bindModelEvents();
      this.bindCollectionEvents();
    }

  , _bindDeclarativeEvents: function (prop, events) {
      var methods = (viewMethods[prop][this.cid] || (viewMethods[prop][this.cid] = {}));
      for (var eventName in events) {
        var method = events[eventName];
        if (!_.isFunction(method)) method = this[events[eventName]];
        if (!method) throw new Error('Method "' + events[eventName] + '" does not exist');
        methods[eventName] = method;
      }
      this.listenTo(this[prop], methods);
    }

  , _unbindDeclarativeEvents: function (prop) {
      var methods = viewMethods[prop][this.cid];
      if (!methods) return;
      this.stopListening(this[prop], methods);
      delete viewMethods[prop][this.cid];
    }

  , bindModelEvents: function (modelEvents) {
      if (!(modelEvents || (modelEvents = _.result(this, 'modelEvents')))) return;
      if (!this.model) throw new Error('View model does not exist');
      this.unbindModelEvents();
      this._bindDeclarativeEvents('model', modelEvents);
    }

  , unbindModelEvents: function () {
      this._unbindDeclarativeEvents('model');
    }

  , bindCollectionEvents: function (collectionEvents) {
      if (!(collectionEvents || (collectionEvents = _.result(this, 'collectionEvents')))) return;
      if (!this.collection) throw new Error('View collection does not exist'); 
      this.unbindCollectionEvents();
      this._bindDeclarativeEvents('collection', collectionEvents);
    }
    
  , unbindCollectionEvents: function () {
      this._unbindDeclarativeEvents('collection');
    }
  });

})(this);



}.call(root));
    return amdExports;
}); }(this));
