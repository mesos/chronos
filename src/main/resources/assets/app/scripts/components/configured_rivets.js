define([
  'backbone',
  'underscore',
  'rivets'
], function(Backbone, _, rivets) {

  'use strict';

  var collectionEvents = 'add remove reset';

  function isColl(o) {
    return o instanceof Backbone.Collection;
  }

  rivets.configure({
    preloadData: false,
    prefix: 'rv',
    adapter: {
      subscribe: function(obj, keypath, callback) {
        if (isColl(obj)) {
          obj.on(collectionEvents, callback).
            on('change:' + keypath, callback);
        } else {
          obj.on('change:' + keypath, callback);
        }
      },
      unsubscribe: function(obj, keypath, callback) {
        if (isColl(obj)) {
          obj.off(collectionEvents, callback).
            off('change:' + keypath, callback);
        } else {
          obj.off('change:' + keypath, callback);
        }
      },
      read: function(obj, keypath) {
        if (isColl(obj)) {
          return obj[keypath] || obj;
        } else {
          return obj.get(keypath);
        }
      },
      publish: function(obj, keypath, value) {
        if (isColl(obj)) {
          obj[keypath] = value;
        } else {
          obj.set(keypath, value);
        }
      }
    }
  });

  _.extend(rivets.formatters, {

    boolEnabled: function(val) {
      return !!val ? 'Enabled' : 'Disabled';
    },

    boolDisabled: function(val) {
      return !!val ? 'Disabled' : 'Enabled';
    },

    inverse: function(val) {
      return !val;
    },

    eq: function(val, comparisonVal) {
      return val === comparisonVal;
    },

    orNone: {
      publish: false,
      read: function(val) {
        return !val ? 'none' : val;
      }
    },

    filterBy: function(c, prop) {
      return c.filter(function(m) {
        var propVal = rivets.config.adapter.read(m, prop);
        return !!propVal;
      });
    },

    mapToList: function(map) {
      var list = _.reduce(map, function(memo, v, k) {
        return memo.concat({
          key: k,
          value: v
        });
      }, []);
      return list;
    }

  });

  return rivets;
});
