import React from 'react';
import { observer } from 'mobx-react';
import JobForm from '../models/JobForm'
import Select from 'react-select'

$(document).ready(function() {
  $('.collapse').on('shown.bs.collapse', function() {
    $(this).parent().find(".glyphicon-chevron-down").removeClass("glyphicon-chevron-down").addClass("glyphicon-chevron-up")
  }).on('hidden.bs.collapse', function() {
    $(this).parent().find(".glyphicon-chevron-up").removeClass("glyphicon-chevron-up").addClass("glyphicon-chevron-down")
  })
})

@observer
class TextInput extends React.Component {
  getGroupClassName(field) {
    if (field.error) {
      return "form-group has-error has-feedback"
    } else if (field.value) {
      return "form-group has-success has-feedback"
    }
    return "form-group"
  }
  getFieldIcon(field) {
    if (field.error) {
      return "glyphicon glyphicon-remove form-control-feedback"
    } else if (field.value) {
      return "glyphicon glyphicon-ok form-control-feedback"
    }
    return "form-control-feedback"
  }
  render() {
    const field = this.props.field
    return (
      <div className={this.getGroupClassName(field)}>
        <label className="control-label col-sm-3" htmlFor={"sf-"+field.name}>{field.label}</label>
        <div className="col-sm-9">
          <input
            type="text"
            className="form-control"
            name={field.name}
            id={"sf-"+field.name}
            defaultValue={field.defaultValue}
            aria-describedby={"sf-"+field.name+"-status"}
            onChange={(event) => field.sync(event, field)}
            />
          <span className={this.getFieldIcon(field)} aria-hidden="true"></span>
          <span id={"sf-"+field.name+"-status"} className="sr-only">{field.error}</span>
          {field.error ? <p>{field.error}</p> : null}
        </div>
      </div>
    )
  }
}

TextInput.propTypes = {
  field: React.PropTypes.object.isRequired,
}

@observer
class NumberInput extends React.Component {
  getGroupClassName(field) {
    if (field.error) {
      return "form-group has-error has-feedback"
    } else if (field.value) {
      return "form-group has-success has-feedback"
    }
    return "form-group"
  }
  getFieldIcon(field) {
    if (field.error) {
      return "glyphicon glyphicon-remove form-control-feedback"
    } else if (field.value) {
      return "glyphicon glyphicon-ok form-control-feedback"
    }
    return "form-control-feedback"
  }
  render() {
    const field = this.props.field
    return (
      <div className={this.getGroupClassName(field)}>
        <label className="control-label col-sm-3" htmlFor={"sf-"+field.name}>{field.label}</label>
        <div className="col-sm-9">
          <input
            type="number"
            className="form-control"
            name={field.name}
            id={"sf-"+field.name}
            defaultValue={field.defaultValue}
            aria-describedby={"sf-"+field.name+"-status"}
            onChange={(event) => field.sync(event, field)}
            />
          <span className={this.getFieldIcon(field)} aria-hidden="true"></span>
          <span id={"sf-"+field.name+"-status"} className="sr-only">{field.error}</span>
          {field.error ? <p>{field.error}</p> : null}
        </div>
      </div>
    )
  }
}

NumberInput.propTypes = {
  field: React.PropTypes.object.isRequired,
}

@observer
class MultiSelectInput extends React.Component {
  getOptions() {
    return this.props.field.getOptions(this.props.jobSummaryStore)
  }
  render() {
    const field = this.props.field
    return (
      <div className="form-group">
        <label className="control-label col-sm-3" htmlFor={"sf-"+field.name}>{field.label}</label>
        <div className="col-sm-9">
          <Select
            multi
            simpleValue
            options={this.getOptions()}
            name={field.name}
            id={"sf-"+field.name}
            aria-describedby={"sf-"+field.name+"-status"}
            onChange={(value) => field.sync(value, field)}
            value={field.value}
            />
          <span id={"sf-"+field.name+"-status"} className="sr-only">{field.error}</span>
          {field.error ? <p>{field.error}</p> : null}
        </div>
      </div>
    )
  }
}

MultiSelectInput.propTypes = {
  field: React.PropTypes.object.isRequired,
  jobSummaryStore: React.PropTypes.object.isRequired,
}

@observer
class SelectInput extends React.Component {
  getOptions() {
    return this.props.field.getOptions(this.props.jobSummaryStore)
  }
  render() {
    const field = this.props.field
    return (
      <div className="form-group">
        <label className="control-label col-sm-3" htmlFor={"sf-"+field.name}>{field.label}</label>
        <div className="col-sm-9">
          <Select
            simpleValue
            options={this.getOptions()}
            name={field.name}
            id={"sf-"+field.name}
            aria-describedby={"sf-"+field.name+"-status"}
            onChange={(value) => field.sync(value, field)}
            value={field.value}
            />
          <span id={"sf-"+field.name+"-status"} className="sr-only">{field.error}</span>
          {field.error ? <p>{field.error}</p> : null}
        </div>
      </div>
    )
  }
}

SelectInput.propTypes = {
  field: React.PropTypes.object.isRequired,
  jobSummaryStore: React.PropTypes.object.isRequired,
}

@observer
class Input extends React.Component {
  render() {
    const field = this.props.field
    if (field.type === "textinput") {
      return (
        <TextInput
          field={field}
          />
      )
    }
    if (field.type === "multiselect") {
      return (
        <MultiSelectInput
          field={field}
          jobSummaryStore={this.props.jobSummaryStore}
          />
      )
    }
    if (field.type === "select") {
      return (
        <SelectInput
          field={field}
          jobSummaryStore={this.props.jobSummaryStore}
          />
      )
    }
    if (field.type === "numberinput") {
      return (
        <NumberInput
          field={field}
          />
      )
    }
  }
}

Input.propTypes = {
  field: React.PropTypes.object.isRequired,
  jobSummaryStore: React.PropTypes.object.isRequired,
}

@observer
class ModalComponent extends React.Component {
  alertField() {
    const jobForm = this.props.jobForm
    if (jobForm.submitError) {
      return (
        <div className="alert alert-warning alert-dismissible" role="alert">
          <button type="button" className="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">&times;</span></button>
          <strong>Error submitting job</strong>
          <p>{jobForm.submitStatus}</p>
          <p>{jobForm.submitError}</p>
        </div>
      )
    }
  }
  typeName() {
    return this.props.jobForm.scheduled ? "scheduled" : "dependent"
  }
  formattedTypeName() {
    return this.props.jobForm.scheduled ? "Scheduled" : "Dependent"
  }
  render() {
    const jobForm = this.props.jobForm
    var normalFields = []
    var advancedFields = []
    this.props.jobForm.allFields.forEach(f => {
      if (f.name === "schedule" && !jobForm.scheduled) return
      if (f.name === "scheduleTimeZone" && !jobForm.scheduled) return
      if (f.name === "parents" && jobForm.scheduled) return
      if (f.advanced) {
        advancedFields.push(<Input key={f.name} field={f} jobSummaryStore={this.props.jobSummaryStore} />)
      } else {
        normalFields.push(<Input key={f.name} field={f} jobSummaryStore={this.props.jobSummaryStore} />)
      }
    })
    return (
      <div className="modal fade" id={this.typeName() + "-job-editor-modal"} tabIndex="-1" role="dialog" aria-labelledby="mymodallabel">
        <div className="modal-dialog" role="document">
          <div className="modal-content">
            <div className="modal-header">
              <button type="button" className="close" data-dismiss="modal" aria-label="close"><span aria-hidden="true">&times;</span></button>
              <h4 className="modal-title" id={this.typeName() + "-job-editor-modal-label"}>New {this.formattedTypeName()} Job</h4>
            </div>
            <form className="form-horizontal" id={this.typeName() + "-job-editor-form"} onSubmit={jobForm.onSubmit.bind(jobForm)}>
              <div className="modal-body">
                {this.alertField()}
                {normalFields}
                <div className="panel-group">
                  <div className="panel panel-default">
                    <div className="panel-heading">
                      <h4 className="panel-title">
                        <a role="button" data-toggle="collapse" href={"#collapse" + this.typeName()} aria-expanded="true" aria-controls={"collapse" + this.typeName()} style={{textDecoration: 'none'}}>
                          <small>Advanced Options <span className="glyphicon glyphicon-chevron-down"></span></small>
                        </a>
                      </h4>
                    </div>
                    <div id={"collapse" + this.typeName()} className="panel-collapse collapse">
                      <div className="panel-body">
                        {advancedFields}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              <div className="modal-footer">
                <button type="button" className="btn btn-default btn-danger" onClick={jobForm.reset.bind(jobForm)}>Reset</button>
                <button type="button" className="btn btn-default" data-dismiss="modal">Close</button>
                <button type="button" className="btn btn-primary" onClick={jobForm.onSubmit.bind(jobForm)} disabled={this.props.jobForm.submitDisabled} data-loading-text='sending... <i class="fa fa-spinner fa-pulse fa-fw"></i>'>Add Job</button>
              </div>
            </form>
          </div>
        </div>
      </div>
    )
  }
}

var scheduledJobForm = new JobForm(true)
var dependentJobForm = new JobForm(false)

@observer
class JobEditor extends React.Component {
  render() {
    return (
      <div className="panel-heading">
        <div className="btn-group text-right">
          <button type="button" className="btn btn-primary dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
            <i className="fa fa-plus" aria-hidden="true"></i> ADD JOB
          </button>
          <ul className="dropdown-menu">
            <li><a href="#" data-toggle="modal" data-target="#scheduled-job-editor-modal"><i className="fa fa-calendar" aria-hidden="true"></i> scheduled</a></li>
            <li><a href="#" data-toggle="modal" data-target="#dependent-job-editor-modal"><i className="fa fa-share-alt" aria-hidden="true"></i> dependent</a></li>
          </ul>
        </div>
        <ModalComponent jobSummaryStore={this.props.jobSummaryStore} jobForm={scheduledJobForm} />
        <ModalComponent jobSummaryStore={this.props.jobSummaryStore} jobForm={dependentJobForm} />
      </div>
    )
  }
}

export default observer(JobEditor);
