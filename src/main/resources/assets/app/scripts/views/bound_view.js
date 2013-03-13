define([
  'jquery',
  'underscore',
  'backbone',
  'components/configured_rivets',
  'components/mixable_view'
],
function($,
         _,
         Backbone,
         rivets,
         View) {

  var undelegateEvents = View.prototype.undelegateEvents,
      namespace = "_rivets";

  function get(t, k) {
    if (!(t[namespace] && t[namespace][k])) { return null; }

    return t[namespace][k];
  }

  function set(t, k, v) {
    if (!t[namespace]) { t[namespace] = {}; }
    if (_.isObject(k)) { _.merge(t[namespace], k); }
    else { t[namespace][k] = v; }
  }

  var BoundView = View.extend({
    getBindModels: function() {
      return _.pick(this, 'model', 'collection');
    },

    addRivets: function() {
      if (get(this, 'enabled')) { return; }
      set(this, 'enabled', true);

      this.listenTo(this, {
        render: function(options) {
          var rivetsView;

          options = _.defaults((options || {}), {
            sync: true
          });
          this.disableRivets();

          rivetsView = rivets.bind(this.$el, this.getBindModels());
          set(this, {view: rivetsView});

          if (options.sync) { this.syncRivets(); }
        }
      });
    },

    syncRivets: function() {
      var view = get(this, 'view');

      if (!view || !view.sync) { return; }
      return view.sync();
    },

    disableRivets: function() {
      var view = get(this, 'view');
      if (view && view.unbind) {
        view.unbind();
        set(this, 'view', null);
      }
    },

    removeRivets: function() {
      this.disableRivets();
      set(this, 'enabled', false);
    },

    undelegateEvents: function() {
      this.removeRivets();
      return undelegateEvents.call(this);
    }
  });

  return BoundView;
});
