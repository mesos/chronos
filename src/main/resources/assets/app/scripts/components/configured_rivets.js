define([
  'jquery',
  'underscore',
  'cs!vendor/rivets'
], function($, _, rivets) {

  rivets.configure({
    preloadData: false,
    prefix: 'rv',
    adapter: {
      subscribe: function(obj, keypath, callback) {
        obj.on('change:' + keypath, callback)
      },
      unsubscribe: function(obj, keypath, callback) {
        obj.off('change:' + keypath, callback)
      },
      read: function(obj, keypath) {
        return obj.get(keypath)
      },
      publish: function(obj, keypath, value) {
        obj.set(keypath, value)
      }
    }
  });

  _.extend(rivets.binders, {
  });

  _.extend(rivets.formatters, {

    boolEnabled: function(val) {
      return !!val ? 'Enabled' : 'Disabled';
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
    }

  });

  return rivets;
});
