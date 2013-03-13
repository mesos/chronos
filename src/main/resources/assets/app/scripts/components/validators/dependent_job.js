/**
 * Dependent Job Model Validator
 *
 */
define([
  'backbone',
  'underscore',
  'models/base_job',
  'models/dependent_job',
  'components/validator'
],
function(Backbone, _, BaseJob, BaseJobValidator) {
  return {
    parents: function(parents) {
      this.should("have parents", function() {
        return parents && (parents.length > 0);
      });

      this.should("have valid parents", function() {
        return this.withCollection(function(collection) {
          return _.every(parents, function(parentName) {
            return collection.where({name: parentName}).length > 0;
          });
        });
      });
    }
  };
});

