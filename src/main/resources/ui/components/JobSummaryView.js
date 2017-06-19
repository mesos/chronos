import React from 'react'
import {observer} from 'mobx-react'
import $ from 'jquery'
import 'bootstrap'
import {JsonStore} from '../stores/JsonStore'
import JsonEditor from './JsonEditor'

$(document).ready(function () {
  $('[data-toggle="tooltip"]').tooltip()
})

@observer
class JobSummaryView extends React.Component {
  jsonStore = new JsonStore()

  constructor(props) {
    super(props);

    this.state = {
      jobs: this.props.jobs,
      currentFilter: null,
      reverseFilterOrder: null
    };
  }

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
        <td>{job.name}</td>
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
    const jobs = this.state.jobs
    return (
      <div className="jobSummaryView">
        <div className="table-responsive">
          <table className="table table-striped table-hover table-condensed">
            <thead>
            <tr>
              <th onClick={() => {this.filterColumn('name')}}
                  className={this.getFilterClassName('name')}>
                JOB
              </th>
              <th onClick={() => {this.filterColumn('schedule')}}
                  className={this.getFilterClassName('schedule')}>
                NEXT RUN
              </th>
              <th onClick={() => {this.filterColumn('status')}}
                  className={this.getFilterClassName('status')}>
                STATUS
              </th>
              <th onClick={() => {this.filterColumn('state')}}
                  className={this.getFilterClassName('state')}>
                STATE
              </th>
              <th className="text-right">ACTIONS</th>
            </tr>
            </thead>
            <tbody>
            {jobs.map(job => this.renderJob(job))}
            </tbody>
          </table>
        </div>
        <JsonEditor jsonStore={this.jsonStore}/>
      </div>
    )
  }

  getFilterClassName(column) {
    if (this.state.currentFilter === column && !this.state.reverseFilterOrder) {
      return "filter"
    } else if (this.state.currentFilter === column) {
      return "filterReverse"
    }
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

  filterColumn(column) {
    let reverseFilterOrder = false;
    let filteredJobs = this.state.jobs.sort((a, b) => {
      return a[column] < b[column] ? -1 : 1;
    });

    //sort in reverse order in case of second click on the same column
    if (this.state.currentFilter === column && !this.state.reverseFilterOrder) {
      filteredJobs = filteredJobs.reverse();
      reverseFilterOrder = !this.state.reverseFilterOrder;
    }

    this.setState({
      jobs: filteredJobs,
      currentFilter: column,
      reverseFilterOrder: reverseFilterOrder
    })
  }
}

JobSummaryView.propTypes = {
  jobs: React.PropTypes.object.isRequired
}

export default JobSummaryView
