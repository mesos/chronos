//Wrapped in an outer function to preserve global this
(function (root) {
  define(
    [
      'backbone',
      'underscore',
      'mousetrap'
    ], function(Backbone, _, Mousetrap) {

  var amdExports;

  (function(_, Backbone, Mousetrap) {

    var oldInitialize, oldRemove, View;

    oldInitialize = Backbone.View.prototype.initialize;
    oldRemove = Backbone.View.prototype.remove;

    View = function BackboneMousetrapView(options) {
      Backbone.View.prototype.constructor.call(this, options);
    };

    _.extend(View.prototype, Backbone.View.prototype, {
      keyboardEvents: {},

      bindKeyboardEvents: function(events) {
        if (!(events || (events = _.result(this, 'keyboardEvents')))) return;
        for (var key in events) {
          var method = events[key];
          if (!_.isFunction(method)) method = this[events[key]];
          if (!method) throw new Error('Method "' + events[key] + '" does not exist');
          method = _.bind(method, this);
          if ('bindGlobal' in Mousetrap && (key.indexOf('command') !== -1 || key.indexOf('ctrl') !== -1)) {
            Mousetrap.bindGlobal(key, method);
          } else {
            Mousetrap.bind(key, method);
          }
        }
        return this;
      },

      unbindKeyboardEvents: function() {
        for (var keys in this.keyboardEvents) {
          Mousetrap.unbind(keys);
        }
        return this;
      },

      initialize: function() {
        var ret = oldInitialize.apply(this, arguments);
        this.bindKeyboardEvents();
        return ret;
      },

      remove: function() {
        var ret = oldRemove.apply(this, arguments);
        if (this.unbindKeyboardEvents) this.unbindKeyboardEvents();
        return ret;
      }
    });

    amdExports = View;
  })(_, Backbone, Mousetrap);

  return amdExports;

}); }(this));
