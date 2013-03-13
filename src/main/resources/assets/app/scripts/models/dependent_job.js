/**
 * Dependent Job Model
 *
 */
define([
  'backbone',
  'underscore',
  'models/base_job',
  'validations/base_job',
  'components/functor'
],
function(Backbone,
         _,
         BaseJobModel,
         BaseJobValidations,
         Functor) {

  var DependentJobModel;

  function getColl(t) {
    if (!!(t && t.collection)) {
      return t.collection;
    } else {
      return app.jobsCollection;
    }
  }

  DependentJobModel = BaseJobModel.extend({
    url: function(action) {
      return BaseJobModel.prototype.url.call(this, action) ||
        '/scheduler/dependency';
    },

    getWhitelist: function() {
      return (['parents']).concat(BaseJobModel.getWhitelist());
    },

    validate: _.extend({}, BaseJobValidations, {
      parents: {
        custom: 'validateParents'
      }
    }),

    hasSchedule: Functor(false),

    validateParents: function(attributeName, attributeValue) {
      var collection = getColl(this),
          valid, circular, empty;

      if (!collection) { return 'Invalid, please refresh.'; }
      valid = _.all(attributeValue, function(parent) {
        return collection.get(parent);
      });

      circular = _.any(attributeValue, function(parent) {
        return this.id === parent;
      }, this);

      empty = _.isEmpty(attributeValue);

      if (!valid) {
        return 'Parents should be specified by name.';
      }

      if (circular) {
        return 'A job may not depend on itself.';
      }

      if (empty) {
        return 'A dependent job must have parents.';
      }
    }
  });

  return DependentJobModel;
});
