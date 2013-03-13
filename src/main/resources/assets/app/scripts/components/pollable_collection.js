define([
  'underscore'
],
function(_) {
  var namespace = "_pollable";

  function get(t, k) {
    if (!(t[namespace] && t[namespace][k])) { return null; }

    return t[namespace][k];
  }

  function set(t, k, v) {
    if (!t[namespace]) { t[namespace] = {}; }
    if (_.isObject(k)) { _.merge(t[namespace], k); }
    else { t[namespace][k] = v; }
  }

  return {
    startPolling: function() {
      if (get(this, 'pollingEnabled')) { return; }
      set(this, 'pollingEnabled', true);
      this.poll();
    },

    poll: function(interval) {
      interval || (interval = 5000);
      var coll = this;

      set(this, 'lastTimeout', setTimeout(function() {
        coll.fetch({update: true});
        coll.poll();
      }, interval));
    },

    stopPolling: function() {
      var lastTimeout = get(this, 'lastTimeout');

      if (!lastTimeout) { return; }
      clearTimeout(lastTimeout);
      set(this, {
        pollingEnabled: false,
        lastTimeout: null
      });
    }
  };
});
