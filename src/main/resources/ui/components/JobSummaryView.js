import React from 'react'
import {observer} from 'mobx-react'
import $ from 'jquery'
import 'bootstrap'
import {JsonStore} from '../stores/JsonStore'
import JsonEditor from './JsonEditor'
import JobDetails from './JobDetails';

$(document).ready(function () {
  $('[data-toggle="tooltip"]').tooltip()
})

@observer
class JobSummaryView extends React.Component {
  jsonStore = new JsonStore()

  disabledWrap(job, value) {
    if (job.disabled) {
      return (
        <s>{value}</s>
      )
    } else {
      return (
        value
      )
    }
  }

  getNameTd(job) {
    if (job.disabled) {
      return (
        <td data-container="body" data-toggle="tooltip" data-placement="top" title="Job is disabled"><s>{job.name}</s>
        </td>
      )
    } else {
      return (
        <td>
          <span onClick={() => {
            this.showJobDetails(job);
          }} className="jobName">
            {job.name}
          </span>
        </td>
      )
    }
  }

  renderJob(job) {
    return (
      <tr key={job.name}>
        {this.getNameTd(job)}
        <td className={job.nextExpected === 'OVERDUE' ? 'danger' : null} data-container="body" data-toggle="tooltip"
            data-placement="top" title={job.schedule}>{job.nextExpected}</td>
        <td className={this.getStatusClass(job)}>{job.status}</td>
        <td className={this.getStateClass(job)}>{job.state}</td>
        <td className="text-right">
          <div className="btn-group" role="group" aria-label="Left Align">
            <button
              type="button"
              onClick={(event) => this.runJob(event, job)}
              className="btn btn-success btn-secondary"
              aria-label="Run"
              data-loading-text='<i class="fa fa-spinner fa-pulse fa-fw"></i>'
              autoComplete="off"
              title="Run">
              <i className="fa fa-play" aria-hidden="true"></i>
            </button>
            <button
              type="button"
              className="btn btn-info"
              aria-label="Edit"
              onClick={() => this.editJob(job)}
              title="Edit">
              <i className="fa fa-pencil-square-o" aria-hidden="true"></i>
            </button>
            <button
              type="button"
              className="btn btn-warning"
              aria-label="Stop"
              data-loading-text='<i class="fa fa-spinner fa-pulse fa-fw"></i>'
              onClick={(event) => this.stopJob(event, job)}
              title="Stop">
              <i className="fa fa-stop" aria-hidden="true"></i>
            </button>
            <button
              type="button"
              className="btn btn-danger"
              aria-label="Delete"
              data-loading-text='<i class="fa fa-spinner fa-pulse fa-fw"></i>'
              onClick={(event) => this.deleteJob(this, job)}
              title="Delete">
              <i className="fa fa-times" aria-hidden="true"></i>
            </button>
          </div>
        </td>
      </tr>
    )
  }

  render() {
    const jobs = this.props.jobs
    return (
      <div>
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
            {jobs.map(job => this.renderJob(job))}
            </tbody>
          </table>
        </div>
        <JsonEditor jsonStore={this.jsonStore}/>
        <JobDetails jsonStore={this.jsonStore}/>
      </div>
    )
  }

  getStatusClass(job) {
    if (job.status === 'success') {
      return 'success'
    }
    if (job.status === 'failure') {
      return 'warning'
    }
    return ''
  }

  getStateClass(job) {
    if (job.state.match(/\d+ running/)) {
      return 'success'
    }
    if (job.state === 'queued') {
      return 'info'
    }
    return ''
  }

  doRequest(target, method, url, success, fail) {
    var btn = $(target).button('loading')
    $.ajax({
      type: method,
      url: url,
    }).done(function (resp) {
      setTimeout(function () {
        btn.button('reset')
        if (success) {
          success()
        }
      }, 500)
    }).fail(function (resp) {
      setTimeout(function () {
        btn.button('reset')
        if (fail) {
          fail(resp)
        }
      }, 500)
    })
  }

  runJob(event, job) {
    this.doRequest(
      event.currentTarget,
      'PUT',
      'v1/scheduler/job/' + encodeURIComponent(job.name)
    )
  }

  stopJob(event, job) {
    this.doRequest(
      event.currentTarget,
      'DELETE',
      'v1/scheduler/task/kill/' + encodeURIComponent(job.name)
    )
  }

  deleteJob(event, job) {
    let _job = job
    this.doRequest(
      event.currentTarget,
      'DELETE',
      'v1/scheduler/job/' + encodeURIComponent(job.name),
      function (resp) {
        _job.destroy()
      }
    )
  }

  editJob(job) {
    this.jsonStore.loadJob(job.name)
    $('#json-modal').modal('show')
  }

  showJobDetails(job) {
    this.jsonStore.loadJob(job.name, false)
    $('#job-details-modal').modal('show')
  }
}

JobSummaryView.propTypes = {
  jobs: React.PropTypes.object.isRequired
}

export default JobSummaryView
