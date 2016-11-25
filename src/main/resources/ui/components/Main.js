import React from 'react'
import JobSummaryView from './JobSummaryView'
import JobEditor from './JobEditor'
import {observer} from 'mobx-react'

@observer
export default class Main extends React.Component {
  render() {
    const jobSummaryStore = this.props.jobSummaryStore;
    return (
      <div className="container">
        <div className="panel panel-default">
          <div className="panel-heading">
            <JobEditor jobSummaryStore={jobSummaryStore} />
          </div>
          <div className="panel-body">
            <div className="row">
              <div className="col-md-1 bg-success">SUCCESS</div>
              <div className="col-md-1 bg-success">{jobSummaryStore.successCount}</div>
              <div className="col-md-1 bg-danger">FAILURE</div>
              <div className="col-md-1 bg-danger">{jobSummaryStore.failureCount}</div>
              <div className="col-md-1 bg-info">FRESH</div>
              <div className="col-md-1 bg-info">{jobSummaryStore.freshCount}</div>
              <div className="col-md-1 bg-primary">RUNNING</div>
              <div className="col-md-1 bg-primary">{jobSummaryStore.runningCount}</div>
              <div className="col-md-1 bg-info">QUEUED</div>
              <div className="col-md-1 bg-info">{jobSummaryStore.queuedCount}</div>
              <div className="col-md-1 bg-success">IDLE</div>
              <div className="col-md-1 bg-success">{jobSummaryStore.idleCount}</div>
            </div>
          </div>
          <div className="table-responsive">
            <table className="table table-striped table-hover table-condensed">
              <thead>
                <tr>
                  <th>JOB</th>
                  <th>NEXT RUN</th>
                  <th>STATUS</th>
                  <th>STATE</th>
                  <th className="text-right">ACTIONS</th>
                </tr>
              </thead>
              <tbody>
                {this.getVisibleJobs().map(job =>
                  (<JobSummaryView key={job.name} job={job} />)
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    )
  }

  getVisibleJobs() {
    return this.props.jobSummaryStore.jobSummarys
  }
}

Main.propTypes = {
  jobSummaryStore: React.PropTypes.object.isRequired,
}
