import React from 'react'
import {observer} from 'mobx-react'
import $ from 'jquery'
import 'bootstrap'

$(document).ready(function(){
  $('[data-toggle="tooltip"]').tooltip()
})

@observer
class JobSummaryView extends React.Component {
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
        <td data-container="body" data-toggle="tooltip" data-placement="top" title="Job is disabled"><s>{job.name}</s></td>
      )
    } else {
      return (
        <td>{job.name}</td>
      )
    }
  }
  render() {
    const job = this.props.job;
    return (
      <tr>
        {this.getNameTd(job)}
        <td className={job.nextExpected === "OVERDUE" ? "danger" : null} data-container="body" data-toggle="tooltip" data-placement="top" title={job.schedule}>{job.nextExpected}</td>
        <td className={this.getStatusClass(job)}>{job.status}</td>
        <td className={this.getStateClass(job)}>{job.state}</td>
        <td className="text-right">
          <div className="btn-group" role="group" aria-label="Left Align">
            <button
              type="button"
              onClick={this.runJob.bind(this)}
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
              disabled="disabled"
              title="Edit">
              <i className="fa fa-pencil-square-o" aria-hidden="true"></i>
            </button>
            <button
              type="button"
              className="btn btn-warning"
              aria-label="Stop"
              data-loading-text='<i class="fa fa-spinner fa-pulse fa-fw"></i>'
              onClick={this.stopJob.bind(this)}
              title="Stop">
              <i className="fa fa-stop" aria-hidden="true"></i>
            </button>
            <button
              type="button"
              className="btn btn-danger"
              aria-label="Delete"
              data-loading-text='<i class="fa fa-spinner fa-pulse fa-fw"></i>'
              onClick={this.deleteJob.bind(this)}
              title="Delete">
              <i className="fa fa-times" aria-hidden="true"></i>
            </button>
          </div>
        </td>
      </tr>
    );
  }

  getStatusClass(job) {
    if (job.status === "success") {
      return "success"
    }
    if (job.status === "failure") {
      return "warning"
    }
    return ""
  }

  getStateClass(job) {
    if (job.state === "running") {
      return "success"
    }
    if (job.state === "queued") {
      return "info"
    }
    return ""
  }

  doRequest(target, method, url, success, fail) {
    var btn = $(target).button('loading')
    $.ajax({
      type: method,
      url: url,
    }).done(function(resp) {
      setTimeout(function() {
        btn.button('reset')
        if (success) {
          success()
        }
      }, 500)
    }).fail(function(resp) {
      setTimeout(function() {
        btn.button('reset')
        if (fail) {
          fail(resp)
        }
      }, 500)
    })
  }

  runJob(event) {
    this.doRequest(
      event.currentTarget,
      "PUT",
      '/v1/scheduler/job/' + encodeURIComponent(this.props.job.name)
    )
  }

  stopJob(event) {
    this.doRequest(
      event.currentTarget,
      "PUT",
      '/v1/scheduler/task/kill/' + encodeURIComponent(this.props.job.name)
    )
  }

  deleteJob(event) {
    var _this = this;
    this.doRequest(
      event.currentTarget,
      "DELETE",
      '/v1/scheduler/job/' + encodeURIComponent(this.props.job.name),
      function(resp) {
        _this.props.job.destroy()
      }
    )
  }
}

JobSummaryView.propTypes = {
  job: React.PropTypes.object.isRequired
};

export default JobSummaryView
