import {observable, computed} from 'mobx';
import $ from 'jquery'

function nextHour(date) {
  date.setHours(date.getHours() + Math.floor(date.getMinutes() / 60))
  date.setMinutes(0)
  date.setSeconds(0)
  date.setMilliseconds(0)
  return date;
}

export default class JobForm {
  scheduled = true
  @observable submitError = ''
  @observable submitStatus = ''
  @observable fields = []
  defaultFields = [
    {
      name: 'name',
      label: 'Name',
      description: 'Unique name for this job',
      sync: this.syncNameField,
      value: '',
      defaultValue: '',
      error: '',
      valid: false,
      type: 'textinput',
      advanced: false,
    },
    {
      name: 'schedule',
      label: 'Schedule',
      description: 'ISO8601 job schedule',
      sync: this.syncScheduleField,
      error: '',
      value: '',
      defaultValue: 'R/' + nextHour(new Date()).toISOString() + '/PT24H',
      valid: true,
      type: 'textinput',
      advanced: false,
    },
    {
      name: 'parents',
      label: 'Parent Jobs',
      description: 'Job parents',
      sync: this.syncParentsField,
      error: '',
      value: '',
      defaultValue: '',
      valid: false,
      type: 'multiselect',
      advanced: false,
      getOptions: this.getParentsOptions,
    },
    {
      name: 'command',
      label: 'Command',
      description: 'Job command (i.e., `sh -c \'command\'`)',
      sync: this.syncNonEmptyStringField,
      value: '',
      defaultValue: '',
      error: '',
      valid: false,
      type: 'textinput',
      advanced: false,
    },
    {
      name: 'container.image',
      label: 'Container Image',
      description: 'Container image name',
      sync: this.syncContainerName,
      value: '',
      defaultValue: '',
      error: '',
      valid: true,
      type: 'textinput',
      advanced: true,
    },
    {
      name: 'container.network',
      label: 'Container Network',
      description: 'Container network type',
      sync: this.syncContainerNetwork,
      value: 'BRIDGE',
      defaultValue: 'BRIDGE',
      error: '',
      valid: true,
      type: 'select',
      advanced: true,
      getOptions: this.getContainerNetworkOptions,
    },
    {
      name: 'container.type',
      label: 'Container Type',
      description: 'Container type',
      sync: this.syncContainerType,
      value: 'DOCKER',
      defaultValue: 'DOCKER',
      error: '',
      valid: true,
      type: 'select',
      advanced: true,
      getOptions: this.getContainerTypeOptions,
    },
    {
      name: 'cpus',
      label: 'CPUs',
      description: 'CPU shares (ex: 0.1)',
      sync: this.syncNonzeroRealField,
      value: null,
      defaultValue: null,
      error: '',
      valid: true,
      type: 'numberinput',
      advanced: true,
    },
    {
      name: 'mem',
      label: 'Memory',
      description: 'Memory limit in GiB',
      sync: this.syncNonzeroRealField,
      value: null,
      defaultValue: null,
      error: '',
      valid: true,
      type: 'numberinput',
      advanced: true,
    },
    {
      name: 'disk',
      label: 'Disk',
      description: 'Disk space in GiB',
      sync: this.syncNonzeroRealField,
      value: null,
      defaultValue: null,
      error: '',
      valid: true,
      type: 'numberinput',
      advanced: true,
    },
    {
      name: 'concurrent',
      label: 'Concurrent',
      description: 'A job which may execute concurrently (i.e., multiple instances)',
      sync: this.syncBooleanField,
      value: false,
      defaultValue: false,
      error: '',
      valid: true,
      type: 'checkbox',
      advanced: true,
    },
    {
      name: 'owner',
      label: 'Owner',
      description: 'Comma-separated list of job owners (i.e., `peter@aol.com`)',
      sync: this.syncStringField,
      value: '',
      defaultValue: '',
      error: '',
      valid: true,
      type: 'textinput',
      advanced: true,
    },
    {
      name: 'ownerName',
      label: 'Owner Name',
      description: 'Own name',
      sync: this.syncStringField,
      value: '',
      defaultValue: '',
      error: '',
      valid: true,
      type: 'textinput',
      advanced: true,
    },
  ]

  getField(name) {
    for (let i = 0; i < this.fields.length; i++) {
      if (this.fields[i].name === name) {
        return this.fields[i]
      }
    }
  }

  @computed get submitDisabled() {
    var hasError = false
    this.fields.some(v => {
      if (!this.scheduled && v.name === 'schedule') {
        return false
      }
      if (this.scheduled && v.name === 'parents') {
        return false
      }
      if (v.name === 'command' &&
          this.getField('container.image').value &&
          this.getField('container.image').valid) {
        return false
      }
      if (!v.valid) {
        hasError = true
        return true
      }
    })
    return hasError ? 'disabled' : ''
  }

  getParentsOptions(jobSummaryStore) {
    var options = []
    jobSummaryStore.jobNames.forEach(j => {
      options.push({label: j, value: j})
    })
    return options
  }

  getContainerNetworkOptions(jobSummaryStore) {
    return [
      {label: 'BRIDGE', value: 'BRIDGE'},
      {label: 'HOST', value: 'HOST'},
    ]
    var options = []
    jobSummaryStore.jobNames.forEach(j => {
      options.push({label: j, value: j})
    })
    return options
  }

  getContainerTypeOptions(jobSummaryStore) {
    return [
      {label: 'DOCKER', value: 'DOCKER'},
    ]
    var options = []
    jobSummaryStore.jobNames.forEach(j => {
      options.push({label: j, value: j})
    })
    return options
  }

  syncNameField(event, field) {
    let value = event.target.value
    if (!value) {
      field.valid = false
      field.error = 'Name must not be empty'
    } else if (!value.match(/^[\w.-]+$/)) {
      field.valid = false
      field.error = 'Name must match regex /^[\w.-]+$/'
    } else {
      field.valid = true
      field.error = ''
    }
    field.value = value
  }

  syncScheduleField(event, field) {
    let value = event.target.value
    if (!value) {
      field.valid = false
      field.error = 'Schedule must not be empty'
    } else if (!value.match(/^R\d*\/([\+-]?\d{4}(?!\d{2}\b))((-?)((0[1-9]|1[0-2])(\3([12]\d|0[1-9]|3[01]))?|W([0-4]\d|5[0-2])(-?[1-7])?|(00[1-9]|0[1-9]\d|[12]\d{2}|3([0-5]\d|6[1-6])))([T\s]((([01]\d|2[0-3])((:?)[0-5]\d)?|24\:?00)([\.,]\d+(?!:))?)?(\17[0-5]\d([\.,]\d+)?)?([zZ]|([\+-])([01]\d|2[0-3]):?([0-5]\d)?)?)?)?\/P(?=\w*\d)(?:\d+Y|Y)?(?:\d+M|M)?(?:\d+W|W)?(?:\d+D|D)?(?:T(?:\d+H|H)?(?:\d+M|M)?(?:\d+(?:\­.\d{1,2})?S|S)?)?$/)) {
      field.valid = false
      field.error = 'Schedule must match ISO8601 format'
    } else {
      field.valid = true
      field.error = ''
    }
    field.value = value
  }

  syncNonEmptyStringField(event, field) {
    let value = event.target.value
    if (!value) {
      field.valid = false
      field.error = field.label + ' must not be empty'
    } else {
      field.valid = true
      field.error = ''
    }
    field.value = value
  }

  syncStringField(event, field) {
    let value = event.target.value
    field.valid = true
    field.error = ''
    field.value = value
  }

  syncParentsField(value, field) {
    if (!value) {
      field.valid = false
      field.error = field.label + ' must not be empty'
    } else {
      field.valid = true
      field.error = ''
    }
    field.value = value
  }

  syncNonzeroRealField(event, field) {
    let value = event.target.value
    if (isNaN(value)) {
      field.valid = false
      field.error = field.label + ' must contain real number'
    } else if (value && value <= 0) {
      field.valid = false
      field.error = field.label + ' must be >=0'
    } else {
      field.valid = true
      field.error = ''
    }
    field.value = value
  }

  syncContainerName(event, field) {
    let value = event.target.value
    if (value) {
      if (!value.match(/^[a-z0-9\/:]+$/)) {
        field.valid = false
        field.error = field.label + ' must match regex /^[a-z0-9\/:]+$/'
      } else {
        field.valid = true
        field.error = ''
      }
    }
    field.value = value
  }

  syncContainerNetwork(value, field) {
    field.valid = true
    field.value = value
  }

  syncContainerType(value, field) {
    field.valid = true
    field.value = value
  }

  syncBooleanField(event, field) {
    let checked = event.target.checked
    field.valid = true
    field.value = checked
  }

  reset() {
    this.fields = $.extend(true, [], this.defaultFields)
    this.submitError = ''
    this.submitStatus = ''
    if ($('#scheduled-job-editor-form')[0]) {
      $('#scheduled-job-editor-form')[0].reset()
    }
    if ($('#dependent-job-editor-form')[0]) {
      $('#dependent-job-editor-form')[0].reset()
    }
  }

  constructor(scheduled) {
    this.scheduled = scheduled
    this.reset()
  }

  onSubmit(event) {
    var _this = this
    var btn = $(event.currentTarget).button('loading')
    var url = ''
    if (_this.scheduled) {
      url = '/v1/scheduler/iso8601'
    } else {
      url = '/v1/scheduler/dependency'
    }
    $.ajax({
      type: 'POST',
      url: url,
      data: JSON.stringify(_this.toJS()),
      dataType: 'json',
      contentType: 'application/json; charset=utf-8',
    }).done(function(resp) {
      setTimeout(function() {
        btn.button('reset')
        $('#scheduled-job-editor-modal').modal('hide')
        $('#dependent-job-editor-modal').modal('hide')
        _this.reset()
      }, 500)
    }).fail(function(resp) {
      setTimeout(function() {
        btn.button('reset')
        _this.submitError = resp.responseText
        _this.submitStatus = resp.status + ': ' + resp.statusText
      }, 500)
    })
  }

  toJS() {
    let obj = Object.create(null)
    this.fields.forEach(v => {
      if (this.scheduled && v.name === 'parents') {
        return
      } else if (!this.scheduled && v.name === 'schedule') {
        return
      }
      if (v.value || v.defaultValue) {
        if (!v.value) {
          v.value = v.defaultValue
        }
        if (v.name === 'parents') {
          obj[v.name] = v.value.split(',')
        } else {
          if (v.name.split('.').length > 1) {
            // handle nested values
            var splat = v.name.split('.').reverse()
            if (splat[splat.length - 1] == 'container') {
              // check that there's actually an image name defined
              if (!this.getField('container.image').value) {
                // skip if there's no image
                return
              }
            }
            var unflatten = function(obj, remaining) {
              var n = remaining.pop()
              if (remaining.length > 0) {
                if (!(n in obj)) {
                  obj[n] = Object.create(null)
                }
                unflatten(obj[n], remaining)
              } else {
                // this is the final value, store it
                obj[n] = v.value
              }
            }
            unflatten(obj, splat)
          } else {
            obj[v.name] = v.value
          }
        }
      }
    })
    return obj
  }
}
