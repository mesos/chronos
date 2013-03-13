/**
 * New Job View
 *
 */
define(['views/job_detail_view'], function(JobDetailView) {

  var NewJobView;

  NewJobView = JobDetailView.extend({
    className: 'job-details-wrapper create-job'
  });

  return NewJobView;
});
