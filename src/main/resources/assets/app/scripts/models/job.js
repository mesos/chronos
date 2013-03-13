/**
 * Generic Job Model
 *
 */
define(['backbone', 'underscore', 'models/base_job'],
       function(Backbone, _, BaseJobModel) {

  var JobModel;

  JobModel = BaseJobModel.extend({
    isNew: function() {
      return true;
    },

    isValid: function() { return false; },

    getWhitelist: function() {
      return ([]).concat(BaseJobModel.getWhitelist());
    },

    hasSchedule: function() {
      return true;
    }
  });

  return JobModel;
});
