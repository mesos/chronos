define([
  'underscore'
],
function(_) {
  'use strict';

  var namespace = "_tooltip_view";

  function get(t, k) {
    if (!(t[namespace] && t[namespace][k])) { return null; }

    return t[namespace][k];
  }

  function set(t, k, v) {
    if (!t[namespace]) { t[namespace] = {}; }
    if (_.isObject(k)) { _.merge(t[namespace], k); }
    else { t[namespace][k] = v; }
  }

  var InstanceMethods = {
    addTooltips: function(eventName) {
      if (get(this, 'tooltipsObserved')) { return; }
      set(this, 'tooltipsObserved', true);

      eventName = eventName || 'render';

      this.listenTo(this, eventName, function() {
        this.$('[data-toggle="tooltip"]').tooltip();
      });
    }
  };

  return {
    InstanceMethods: InstanceMethods
  };
});
