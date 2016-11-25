import {observable, computed, autorun} from 'mobx';
import JobSummaryModel from '../models/JobSummaryModel'

export class JobSummaryStore {
  @observable jobSummarys = new Array;
  @observable isLoading = true;

  constructor() {
    this.loadJobSummarys();
  }

  @computed get successCount() {
    return this.jobSummarys.reduce(
      (sum, job) => sum + (job.status === "success" ? 1 : 0),
      0
    )
  }

  @computed get failureCount() {
    return this.jobSummarys.reduce(
      (sum, job) => sum + (job.status === "failure" ? 1 : 0),
      0
    )
  }

  @computed get freshCount() {
    return this.jobSummarys.reduce(
      (sum, job) => sum + (job.status === "fresh" ? 1 : 0),
      0
    )
  }

  @computed get queuedCount() {
    return this.jobSummarys.reduce(
      (sum, job) => sum + (job.state === "queued" ? 1 : 0),
      0
    )
  }

  @computed get idleCount() {
    return this.jobSummarys.reduce(
      (sum, job) => sum + (job.state === "idle" ? 1 : 0),
      0
    )
  }

  @computed get runningCount() {
    return this.jobSummarys.reduce(
      (sum, job) => sum + (job.state === "running" ? 1 : 0),
      0
    )
  }

  @computed get jobNames() {
    var names = []
    this.jobSummarys.forEach(j => {
      names.push(j.name)
    })
    return names
  }

  /**
  * Update a jobSummary with information from the server. Guarantees a jobSummary
  * only exists once. Might either construct a new jobSummary, update an existing one,
  * or remove a jobSummary if it has been deleted on the server.
  */
  updateJobSummaryFromServer(json) {
    var jobSummary = this.jobSummarys.find(jobSummary => jobSummary.name === json.name);
    if (!jobSummary) {
      jobSummary = JobSummaryModel.fromJS(this, json);
      this.jobSummarys.push(jobSummary);
    }
    jobSummary.updateFromJson(json);
  }

  /**
  * Fetches all jobSummary's from the server
  */
  loadJobSummarys() {
    this.isLoading = true;
    var otherThis = this;
    $.getJSON('/v1/scheduler/jobs/summary').done(function(resp) {
      var serverJobNames = new Set();
      resp.jobs.forEach(json => {
        serverJobNames.add(json.name)
        otherThis.updateJobSummaryFromServer(json)
      });

      // Check for jobs which exist here, but not on the server
      otherThis.jobSummarys.filter(function(j) {
        return !serverJobNames.has(j.name)
       }).forEach(j => j.destroy())

      otherThis.isLoading = false;
    }).fail(function() {
      otherThis.isLoading = false;
    });
    setTimeout(function() {
      otherThis.loadJobSummarys();
    }, 2000);
  }

  /**
  * A jobSummary was somehow deleted, clean it from the client memory
  */
  removeJobSummary(jobSummary) {
    this.jobSummarys.splice(this.jobSummarys.indexOf(jobSummary), 1);
  }
}
