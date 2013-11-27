/**
 * Details Collection
 */
define([
  'underscore',
  'collections/base_jobs',
  'components/functor'
], function(_,
            JobsCollection,
            Functor) {

  'use strict';

  var DetailsCollection = JobsCollection.extend({

    initialize: function() {
      this.listenTo(this, {
        add: this.serialize,
        remove: this.serialize
      });
    },

    deserialize: function(path) {
      var modelNames = _.uniq(path.split('/')),
          collection = this,
          modelNamesMap,
          models,
          untrackedModels,
          name;

      modelNamesMap = _.object(modelNames,
                               _.times(modelNames.length, Functor(true)));

      models = app.jobsCollection.filter(function(job) {
        if (!!(name = job.get('name'))) { return !!modelNamesMap[name]; }
        return false;
      });

      untrackedModels = _.filter(models, function(model) {
        var name = model.get('name'),
            other = collection.where({name: name});

        return !(!name || !!other.length);
      });

      this.add(untrackedModels);
    },

    serialize: function(model, collection, options) {
      var base = ['jobs'],
          isFake = options && options.create,
          names,
          path;

      if (isFake || model.isNew()) {
        return false;
      }

      if (this.length) {
        names = _.chain(this.where({persisted: true})).map(function(job) {
          return job.get('name');
        }).reject(function(name) {
          return name === '-';
        }).value();
        path = base.concat(names).join('/');
      } else {
        path = '/';
      }

      // TODO: hack to fix jobs/:jobName////////////// infinite redirect
      if (path.slice(-1) === '-') {
        return false;
      }

      app.router.navigate(path);
    }
  });

  return DetailsCollection;
});
