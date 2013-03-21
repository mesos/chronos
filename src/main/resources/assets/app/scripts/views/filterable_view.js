define([
  'jquery',
  'backbone',
  'underscore',
  'collections/selections',
  'components/fuzzy_select2',
  'hbs!templates/filterable_view'
],
function($,
         Backbone,
         _,
         Selections,
         FuzzySelect2,
         FilterableTpl) {

  var FilterableView;

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

  FilterableView = Backbone.View.extend({
    initialize: function() {
      _.bindAll(this, 'filter');

      if (!this.collection) {
        this.collection = new Selections();
      }

      this.listenTo(this.collection, {
        add: this.filterSelectionsAdd,
        remove: this.filterSelectionsRemove,
        reset: this.filterSelectionsReset
      });
    },

    template: FilterableTpl,

    render: function() {
      var data = this.collection.toJSON();

      this.$el.html(this.template());

      FuzzySelect2.attach(this.$filterField(), {
        width: '90%',
        multiple: true,
        collection: app.jobsCollection
      }).on('change', this.filter);

      if (!_.isEmpty(data)) {
        this.$filterField().select2('data', data);
      }

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

      this.trigger('selections:updated', this.collection);
    },

    setRelatedTo: function(targetName, related) {
      if (!related) { return; }

      var s2Data = MapRelatedTo(targetName, related);

      this.collection.reset(s2Data);
    },

    toMap: function() {
      var json = this.collection.toJSON();

      return _(json).reduce(function(memo, v) {
        memo[v.id] = v;
        return memo;
      }, {});
    },

    setTarget: function(id) {
      var related;

      related = app.jobsGraphCollection.getWithRelatedNodes(id);
      return this.setRelatedTo(id, related);
    },

    updateRelatedTo: function(targetName, related) {
      var $el, dataMap1, dataMap2, data, s2Data;
      if (!related) { return; }

      $el = this.$filterField();
      s2Data = $el.select2('data');
      dataMap1 = KeyBy(s2Data, 'text');
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
    },

    filterSelections: function(model) {
      var related,
          related2,
          view = this,
          args = Array.prototype.slice.call(arguments);

      if (!!this.collection) {
        related = app.jobsCollection.getRelatedTo(model.id);
        related2 = app.jobsGraphCollection.getWithRelatedNodes(model.id);
        this.updateRelatedTo(model.get('text'), related2);
      }

      this.trigger('selections:updated', this.collection);
    },

    filter: function(e) {
      e || (e = {});
      var added   = e.added,
          removed = e.removed,
          $el;

      if (!!added) { this.collection.add(added); }

      if (!!removed) { this.removeRelatedTo(removed.id); }
    },

    '$filterField': function() {
      return this.$el.find('.graph-filter');
    },

    closed: function(e) {
      FuzzySelect2.unattach(this.$filterField());
    }
  });

  return FilterableView;
});
