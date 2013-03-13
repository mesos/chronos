/**
 * Graph View
 *
 */
define([
  'backbone',
  'underscore',
  'collections/selections',
  'views/graph',
  'hbs!templates/graph_view',
  'hbs!templates/select2_child',
  'components/fuzzy_select2',
  'bootstrap/tooltip'
],
function(Backbone,
         _,
         Selections,
         Graph,
         GraphViewTpl,
         Select2ChildTpl,
         FuzzySelect2) {

  var GraphView;

  function KeyBy(list, keyName) {
    if (!list || !keyName) { return; }

    return _.reduce(list, function(memo, i) {
      memo[i[keyName]] = i;
      return memo;
    }, {});
  }

  function MapRelatedTo(targetName, related) {
    return _.map(related, function(v, k) {
      return {
        id: k,
        text: k,
        locked: (k !== targetName),
        lockedFor: targetName
      };
    });
  }

  /*
   * RenderChildren renders related elements into select2 results.
   * this should be bound to a Backbone view.
   *
   * @params {Integer} resultId The ID (name) of a query result.
   * @params {Array}  related A list of related jobs.
   *
   * @returns this
   */
  function RenderChildren(resultId, related) {
    var sel = ['[data-select2-id="', resultId, '"]'].join(''),
        relatedNames = _.chain(related).keys().without(resultId).value();

    this.$(sel).find('ul.children').html(Select2ChildTpl({
      children: relatedNames
    }));

    return this;
  }

  GraphView = Backbone.View.extend({
    className: 'graph-area',

    template: GraphViewTpl,

    events: {
      'click [data-lightbox-close="true"]': 'closed',
      'click .icon-play': 'resumed',
      'click .icon-pause': 'paused'
    },

    initialize: function() {
      _.bindAll(this, 'filter');

      this.collection = new Selections();
      this.listenTo(this.collection, {
        add: this.filterSelectionsAdd,
        remove: this.filterSelectionsRemove,
        reset: this.filterSelectionsReset
      });

      this.collection.on('all', function() {
        var eventName = Array.prototype.slice.call(arguments, 0, 1),
            eventArgs = Array.prototype.slice.call(arguments, 1);

        console.log('GraphView collection event',
                    '\n', 'event name:', eventName,
                    '\n', 'event args:', eventArgs,
                    '\n', 'collection', this.collection,
                    '\n', 'this', this);
      }, this);
    },

    render: function() {
      var html = this.template();
      this.$el.html(html);

      FuzzySelect2.attach(this.$filterField(), {
        width: '90%',
        multiple: true,
        collection: app.jobsCollection
      }).on('change', this.filter);

      return this;
    },

    postModifyRelatedTo: function() {
      this.$('[data-toggle="tooltip"]').tooltip();
    },

    removeRelatedTo: function(parentId) {
      var s2Data, s2Final, $el, oldModels, lockedForTarget, isTarget;

      if (!parentId) { return; }

      $el = this.$filterField();
      s2Data = $el.select2('data');
      s2Final = _.reduce(s2Data, function(memo, v) {
        lockedForTarget = v.locked && v.lockedFor && (v.lockedFor === parentId);
        isTarget = (v.id === parentId);

        if (!(lockedForTarget || isTarget)) {
          memo[v.id] = v;
        }

        return memo;
      }, {});

      $el.select2('data', _.values(s2Final));

      oldModels = this.collection.filter(function(model) {
        return !s2Final[model.id];
      });

      if ((this.collection.length - oldModels.length) < 1) {
        this.collection.reset();
      } else {
        this.collection.remove(oldModels, {silent: true});
      }
      this.showGraph();
    },

    setRelatedTo: function(targetName, related) {
      if (!related) { return; }

      var s2Data = MapRelatedTo(targetName, related);

      this.collection.reset(s2Data);
    },

    setTarget: function(id) {
      var related;

      related = app.jobsGraphCollection.getWithRelatedNodes(id);
      return this.setRelatedTo(id, related);
    },

    addTarget: function(id) {
    },

    updateRelatedTo: function(targetName, related) {
      var $el, dataMap1, dataMap2, data;
      if (!related) { return; }

      $el = this.$filterField();
      dataMap1 = KeyBy($el.select2('data'), 'text');
      dataMap2 = KeyBy(MapRelatedTo(targetName, related), 'text');
      data = _.chain(dataMap1).merge(dataMap2).values().value();

      this.collection.reset(data);
    },

    filterSelectionsAdd: function() {
      var args = Array.prototype.slice.call(arguments);
      return this.filterSelections.apply(this, args);
    },

    filterSelectionsRemove: function() {
      var args = Array.prototype.slice.call(arguments);
      return this.filterSelections.apply(this, args);
    },

    filterSelectionsReset: function(collection, options) {
      var data = collection.toJSON(),
          $el  = this.$filterField();

      $el.select2('data', data);
      this.postModifyRelatedTo(data);

      if (this.collection.isEmpty()) {
        //this.showGraph();
        //Graph.showScoped(null, app.jobsGraphCollection);
      }
    },

    showGraph: function() {
      var scope = this.collection.toJSON();

      if (_.isEmpty(scope)) { scope = null; }
      else {
        scope = _.reduce(scope, function(memo, v) {
          memo[v.id] = v;
          return memo;
        }, {});
      }

      Graph.showScoped(scope, app.jobsGraphCollection);
    },

    showTree: function() {
      var scope = this.collection.toJSON(),
          collection = app.jobsGraphCollection,
          data, nodes;

      if (_.isEmpty(scope)) {
        data = app.jobsGraphCollection.toJSON();
      } else {
        data = _.filter(scope, function(s) { return !s.locked; });
      }

      nodes = _.merge.apply(null, _.map(data, function(c) {
        return collection.get(c.id).getRelatedAsTree();
      }));

      //Tree.showScoped(nodes);
    },

    filterSelections: function(model) {
      var related,
          view = this,
          args = Array.prototype.slice.call(arguments);

      if (!!this.collection) {
        related = app.jobsCollection.getRelatedTo(model.id);
        this.updateRelatedTo(model.get('text'), related);
      }

      this.showGraph();
      //Graph.showScoped(related, app.jobsGraphCollection, model.id);
    },

    filter: function(e) {
      var added   = e.added,
          removed = e.removed,
          $el;

      if (!!added) { this.collection.add(added); }

      if (!!removed) { this.removeRelatedTo(removed.id); }
    },

    '$filterField': function() {
      return this.$('#graph-filter');
    },

    closed: function(e) {
      FuzzySelect2.unattach(this.$filterField());
    },

    resumed: function(e) {
      e && e.preventDefault();

      Graph.start();
    },

    paused: function(e) {
      e && e.preventDefault();

      Graph.stop();
    }
  });

  return GraphView;
});
