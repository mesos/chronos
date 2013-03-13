/**
 * Jobs Collection
 *
 */
define([
  'underscore',
  'collections/base_jobs',
  'components/pollable_collection'
], function(_,
            BaseJobsCollection,
            Pollable) {

  var JobsCollection;

  JobsCollection = BaseJobsCollection.extend(_.extend({}, Pollable, {
    url: '/scheduler/jobs'
    //url: '/stubs/jobs.json'
  }));

  return JobsCollection;
});
