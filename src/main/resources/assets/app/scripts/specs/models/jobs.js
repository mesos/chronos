define([
  'underscore',
  'mocha',
  'chai',
  'models/base_job',
  'models/scheduled_job',
  'models/dependent_job',
  'collections/jobs'
],
function(_,
         _mocha,
         Chai,
         BaseJob,
         ScheduledJob,
         DependentJob,
         JobsCollection) {

  var expect = Chai.expect;

  describe('Base jobs', function() {
    it('should not be saveable', function() {
      var job;

      job = new BaseJob();
      expect(_.bind(job.save, job)).to.throw(Error);
    });
  });

  var BaseJobValidity = function(JobClass) {
    return function() {
      it('should be invalid without a name', function() {
        var job = new JobClass();
        expect(job.validate()).to.be.falsy;
      });

      it('should not be the default', function() {
        var defaultAttributes, job;
        defaultAttributes = BaseJob.prototype.defaults.call();
        job = new JobClass({name: defaultAttributes.name});

        var vvv;
        expect(job.validate()).to.be.falsy;
      });

      it('should be globally unique', function() {
        var jobsCollection, jobs, names;

        jobsCollection = new JobsCollection();
        names = [
          'job1',
          'job2',
          'job3',
          'job1'
        ];

        jobs = _.map(names, function(name) {
          return new JobClass({name: name});
        });

        jobsCollection.add(jobs);

        expect(jobsCollection.length).to.be.eql(3);
      });
    }
  };

  describe('Dependent jobs', function() {
    describe('should follow base valitity rules',
             BaseJobValidity(DependentJob));
  });

  describe('Scheduled jobs', function() {
    describe('should follow base valitity rules',
             BaseJobValidity(ScheduledJob));
  });
});
