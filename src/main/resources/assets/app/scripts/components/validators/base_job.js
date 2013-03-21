/**
 * Base Job Model Validator
 *
 */
define([
  'backbone',
  'underscore',
  'models/base_job'
],
function(Backbone, _, BaseJob) {
  return {
    name: function(name) {
      this.should("have non-default name", function() {
        var defaults = BaseJob.prototype.defaults.call(null);
        return defaults.name !== name;
      });

      this.should("have unique name", function() {
        return this.withCollection(function(collection) {
          return !collection.some(function(model) {
            return model.get('name') === name;
          });
        });
      });
    }
  };
});
