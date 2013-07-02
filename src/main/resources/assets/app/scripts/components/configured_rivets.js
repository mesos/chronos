define([
  'jquery',
  'backbone',
  'underscore',
  'cs!vendor/rivets'
], function($, Backbone, _, rivets) {

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
          obj.on('change:' + keypath, callback)
        }
      },
      unsubscribe: function(obj, keypath, callback) {
        if (isColl(obj)) {
          obj.off(collectionEvents, callback).
            off('change:' + keypath, callback);
        } else {
          obj.off('change:' + keypath, callback)
        }
      },
      read: function(obj, keypath) {
        if (isColl(obj)) {
          return obj[keypath] || obj;
        } else {
          return obj.get(keypath);
        };
      },
      publish: function(obj, keypath, value) {
        if (isColl(obj)) {
          obj[keypath] = value;
        } else {
          obj.set(keypath, value);
        };
      }
    }
  });

  _.extend(rivets.binders, {
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

    lastRunDescr: {
      read: function(val) {
        if (!!val.lastRunError) {
          return 'Last run @ ' + val.lastRunTime + ' was successful.';
        } else if (!!val.lastRunSuccess) {
          return 'Last run @ ' + val.lastRunTime + ' failed.';
        } else {
          return 'Job has not run yet.';
        }
      },
      publish: false
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
