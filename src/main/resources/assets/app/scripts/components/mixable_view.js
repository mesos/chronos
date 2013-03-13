define([
  'jquery',
  'underscore',
  'backbone'
],
function($,
         _,
         Backbone) {
  'use strict';

  var namespace = "_mixable_view",
      extend = Backbone.View.extend,
      ctor   = Backbone.View.prototype.constructor,
      slice  = Array.prototype.slice,
      MixableView;

  function get(t, k) {
    if (!(t[namespace] && t[namespace][k])) { return null; }

    return t[namespace][k];
  }

  function set(t, k, v) {
    if (!t[namespace]) { t[namespace] = {}; }
    if (_.isObject(k)) { _.merge(t[namespace], k); }
    else { t[namespace][k] = v; }
  }

  function MergeMethods(options) {
    options = _.defaults((options || {}), {methodChains: true});

    return function() {
      var args = slice.call(arguments),
          result = args.shift(),
          target;

      while (!!(target = args.shift())) {
        if (_.isObject(target) || _.isArray(target)) {
          _.each(target, function(v, k) {
            if (_.isFunction(result[k]) && _.isFunction(v) && options.methodChains) {
              var oldMethod = result[k];

              if (!oldMethod.isMethodChain) {
                result[k] = function() {
                  var _args = slice.call(arguments);
                };
                result[k].isMethodChain = true;
              }
            } else {
              var vals = {};
              vals[k] = v;
              _.merge(result, vals);
            }
          });
        }
      }

      return result;
    }
  }

  MixableView = Backbone.View.extend({
    constructor: function() {
      ctor.apply(this, Array.prototype.slice.call(arguments));
      set(this, 'methodChains', {});
    }
  }, {
    extend: function(instanceMethods, classMethods) {
      var without, mixins, args, merger, mergeArgs;

      args = _.map([instanceMethods, classMethods], function(methods, i) {
        without = _.omit(methods, 'mixins');
        mixins  = _.result(methods, 'mixins');

        if (mixins && _.isObject(mixins)) {
          merger = MergeMethods({
            methodChains: (i === 0)
          });
          mergeArgs = [{}].concat(_.values(mixins)).concat(without);

          //return merger.apply(null, mergeArgs);
          return _.merge.apply(null, mergeArgs);
        } else {
          return without;
        }
      });

      return extend.apply(this, args);
    }
  });

  return MixableView;
});
