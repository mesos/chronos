/**
 * Job Graph Collection
 *
 */
define([
  'jquery',
  'backbone',
  'underscore',
  'components/pollable_collection'
], function($,
            Backbone,
            _,
            Pollable) {

  var Sync = Backbone.sync,
      CSVModel,
      JobNodeModel,
      JobPathModel,
      JobGraph,
      csvKeys,
      Sync;

  csvKeys = {
    node: ['type', 'name', 'lastRunStatus'],
    link: ['type', 'source', 'target']
  };

  CSVModel = Backbone.Model.extend({
  });

  JobNodeModel = CSVModel.extend({
    idAttribute: 'name',

    lastRunWasFailure: function() {
      return (this.get('lastRunStatus') === 'failure');
    },

    getLastRunDate: function() {
      return this.get((this.lastRunWasFailure() ? 'lastError' : 'lastSuccess'));
    },

    getChildren: function() {
      if (this.sources && this.targets) {
        return this.sources.concat(this.targets);
      } else {
        return [];
      }
    },

    getRelatedAsTree: function(seen) {
      var n = this.toJSON(),
          children = this.getChildren(),
          unseenChildren;

      seen || (seen = [this]);

      unseenChildren = _.filter(children, function(child) {
        return !_.any(seen, function(seenChild) {
          return seenChild.get('name') === child.get('name');
        });
      });

      n.children = _.map(unseenChildren, function(child) {
        return child.getRelatedAsTree(children.concat(seen));
      });

      return n;
    },

    getRelated: function(seen) {
      seen || (seen = []);
      var children = this.getChildren(),
          unseenChildren, nextGen;

      unseenChildren = _.filter(children, function(child) {
        return !_.any(seen, function(seenChild) {
          return seenChild.get('name') === child.get('name');
        });
      });

      nextGen  = _.map(unseenChildren, function(c) {
        return c.getRelated(children.concat(seen));
      });

      return _.flatten(children.concat(nextGen));
    }
  });

  JobPathModel = CSVModel.extend({});

  JobGraph = Backbone.Collection.extend(_.extend({}, Pollable, {
    url: '/scheduler/graph/csv',
    //url: '/stubs/graph.csv',

    parse: function(response, options) {
      var rows = _.compact(response.split('\n'));
      return _.map(rows, function(_row) {
        var row = _row.split(',');
        return _.object(csvKeys[row[0]], row);
      });
    },

    sync: function(method, model, options) {
      return Sync.call(this, method, model, _.extend({}, options, {
        dataType: 'text'
      }));
    },

    model: function(attributes, options) {
      var model = (attributes.type === 'link') ? JobPathModel : JobNodeModel;

      return new model(attributes, options);
    },

    initialize: function(attrs, options) {
      this.listenTo(this, {
        add: this.buildGraph,
        remove: this.buildGraph,
        reset: this.buildGraph
      });
    },

    buildGraph: function() {
      var paths = this.where({type: 'link'}),
          nodes = this.where({type: 'node'}),
          nodeName,
          sources,
          targets;

      _.each(nodes, function(node) {
        nodeName = node.get('name');
        sources = [];
        targets = [];

        _.each(paths, function(path) {
          if (path.get('target') === nodeName) {
            sources.push(this.get(path.get('source')));
          } else if (path.get('source') === nodeName) {
            targets.push(this.get(path.get('target')));
          }
        }, this);

        _.extend(node, {
          sources: sources,
          targets: targets
        });
      }, this);
    },

    getPaths: function(scope) {
      var target, source;
      scope || (scope = {});

      return _.chain(this.where({type: 'link'})).filter(function(path) {
        if (scope.parents) {
          target = path.get('target');
          source = path.get('source');

          return (scope.parents[target] || scope.parents[source]);
        } else {
          return _.isEmpty(scope);
        }
      }).value();
    },

    getNodes: function(scope) {
      var name;
      scope || (scope = {});

      return _.chain(this.where({type: 'node'})).filter(function(node) {
        if (!_.isEmpty(scope)) {
          return scope[node.get('name')];
        } else {
          return true;
        }
      }).value();
    },

    getWithRelatedNodes: function(name) {
      var node = this.get(name),
          related2 = node.getRelated(),
          relations;

      relations = _.reduce(related2, function(memo, relNode) {
        memo[relNode.get('name')] = true;
        return memo;
      }, {});

      relations[name] = true;

      return relations;
    },

    getPlainNodes: function(options) {
      var scope;
      options || (options = {});
      scope = options.scope;

      return _.chain(this.getNodes()).filter(function(n) {
        return !!scope ? scope[n.get('name')] : true;
      }).map(function(n, i) {
        return {
          name: n.get('name'),
          fail: n.lastRunWasFailure(),
          date: n.getLastRunDate(),
          id: i
        };
      }).value();
    },

    getPlainPaths: function(options) {
      var scope, paths, matchedPaths;
      options || (options = {});
      scope  = options.scope;
      paths  = this.getPaths();

      if (!!scope) {
        matchedPaths = _.reduce(scope, function(memo, v, k) {
          var source, target, isUnlockedAsTarget,
              isLocked, isLockedToTarget, isLockedFromSource;

          _.each(paths, function(path) {
            source = path.get('source');
            target = path.get('target');
            isLocked = !v.locked;
            isUnlockedAsTarget = !isLocked && (target === v.id);
            isLockedToTarget = isLocked && (source === v.id) && scope[target];
            isLockedFromSource = isLocked && (target === v.id) && scope[source];

            if (isUnlockedAsTarget || isLockedToTarget || isLockedFromSource) {
              memo.push(path);
            }
          });

          return memo;
        }, []);
      } else {
        matchedPaths = paths;
      }

      return _.map(matchedPaths, function(p) {
        return {
          sourceName: p.get('source'),
          targetName: p.get('target'),
          inScope: true,
          weight: 1
        };
      });
    },

    getLastStatus: function(m) {
      var mId = _.has(m, 'id') ? m.id : m,
          match;

      match = _.first(this.getNodes({id: mId}) || []);

      return !!match ? match.get('lastRunStatus') : null;
    },

    watchAccessoryLastSuccess: function(accessoryModel, value, options) {
      var m = this.get(accessoryModel.id);

      if (!!m) { m.set('lastSuccess', value); }
    },

    watchAccessoryLastError: function(accessoryModel, value, options) {
      var m = this.get(accessoryModel.id);

      if (!!m) { m.set('lastError', value); }
    },

    registerAccessoryCollection: function(c) {
      var _this = this,
          updateInAccessoryCollection;

      updateInAccessoryCollection = function(model, value) {
        var cModel = c.get(model.id);

        if (!!cModel) {
          cModel.set('lastRunStatus', value);
          model.set({
            lastError: cModel.get('lastError'),
            lastSuccess: cModel.get('lastSuccess')
          });
        }
      };

      this.listenTo(c, {
        'change:lastSuccess': this.watchAccessoryLastSuccess,
        'change:lastError': this.watchAccessoryLastError,
        sync: function(coll) {
          _this.each(function(model) {
            if (!(JobGraph.isNode(model))) { return; }
            updateInAccessoryCollection(model, model.get('lastRunStatus'));
          });
        }
      });

      c.listenTo(this, {
        'change:lastRunStatus': updateInAccessoryCollection,
        reset: function(collection, options) {
          collection.each(function(model) {
            updateInAccessoryCollection(model, model.get('lastRunStatus'));
          });
        }
      });

      return this;
    }
  }), {
    isNode: function(model) {
      return model.get('type') === 'node';
    },

    isPath: function(model) {
      return model.get('type') === 'link';
    }
  });

  return JobGraph;
});
