import React from 'react'
import {observer} from 'mobx-react'
import brace from 'brace'
import AceEditor from 'react-ace'

import 'brace/mode/javascript'
import 'brace/theme/monokai'

@observer
export default class JsonEditor extends React.Component {
  setAceCallback = false
  componentDidUpdate() {
    var _this = this
    if (!this.setAceCallback && this.refs.ace) {
      this.setAceCallback = true
      this.editor = this.refs.ace.editor
      this.editor.getSession().on('changeAnnotation', function() {
        var annotations = _this.editor.getSession().getAnnotations()
        var canSave = true
        if (annotations && annotations.length > 0) {
          annotations.forEach(function(anno){
            if (anno.type === 'error' || anno.type === 'warning') {
              canSave = false
            }
          })
        }
        if (canSave) {
          $('#json-editor-submit-button').prop('disabled', false)
        } else {
          $('#json-editor-submit-button').prop('disabled', true)
        }
      })
    }
  }
  getValue() {
    const jsonStore = this.props.jsonStore
    if (jsonStore.job) {
      return JSON.stringify(jsonStore.job.json, null, '  ')
    }
    return ''
  }
  saveJson() {
    if (this.refs.ace) {
      let _this = this
      this.editor = this.refs.ace.editor
      let session = this.editor.getSession()
      let btn = $('#json-editor-submit-button')
      const job = this.props.jsonStore.job
      btn.button('loading')
      let url = ''
      if ('schedule' in job.json) {
        url = 'v1/scheduler/iso8601'
      } else {
        url = 'v1/scheduler/dependency'
      }
      $.ajax({
        type: 'POST',
        url: url,
        data: session.getValue(),
        dataType: 'json',
        contentType: 'application/json; charset=utf-8',
      }).done(function(resp) {
        setTimeout(function() {
          $('#json-modal').modal('hide')
          btn.button('reset')
          jsonStore.submitError = ""
          jsonStore.submitStatus = ""
        }, 500)
      }).fail(function(resp) {
        setTimeout(function() {
          btn.button('reset')
          const jsonStore = _this.props.jsonStore
          jsonStore.submitError = resp.responseText
          jsonStore.submitStatus = resp.status + ': ' + resp.statusText
        }, 500)
      })
    }
  }
  setCurrentValue(value) {
    this.props.jsonStore.value = value
  }
  getEditor() {
    const jsonStore = this.props.jsonStore
    if (jsonStore.isLoading || !jsonStore.job) {
      this.setAceCallback = false
      return (
        <div>
          <p>Loading...</p>
        </div>
      )
    } else {
      return (
        <AceEditor
          ref="ace"
          mode="javascript"
          theme="monokai"
          onChange={(value) => this.setCurrentValue(value)}
          value={this.props.jsonStore.value}
          defaultValue={this.getValue()}
          name="json-editor-unique-id"
          editorProps={{
            $blockScrolling: true
          }}
          setOptions={{
            tabSize: 2,
            showGutter: true,
          }}
          />
      )
    }
  }
  alertField() {
    const jsonStore = this.props.jsonStore
    if (jsonStore.submitError) {
      return (
        <div id="json-editor-alert" className="alert alert-warning alert-dismissible" role="alert">
          <button type="button" className="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">&times;</span></button>
          <strong>Error submitting job</strong>
          <p>{jsonStore.submitStatus}</p>
          <p>{jsonStore.submitError}</p>
        </div>
      )
    }
  }
  render() {
    const jsonStore = this.props.jsonStore
    return (
      <div className="modal fade" id="json-modal" role="dialog" aria-labelledby="json-modal-label">
        <div className="modal-dialog" role="document">
          <div className="modal-content">
            <div className="modal-header">
              <button type="button" className="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
              <h4 className="modal-title" id="json-modal-label">Edit Job</h4>
            </div>
            <div className="modal-body">
              {this.alertField()}
              <div className="container">
                {this.getEditor()}
              </div>
            </div>
            <div className="modal-footer">
              <button type="button" className="btn btn-default" data-dismiss="modal">Close</button>
              <button
                type="button"
                id="json-editor-submit-button"
                className="btn btn-primary" onClick={() => this.saveJson()}
                data-loading-text='<i class="fa fa-spinner fa-pulse fa-fw"></i>'>
                Save changes
              </button>
            </div>
          </div>
        </div>
      </div>
    )
  }
}

JsonEditor.propTypes = {
  jsonStore: React.PropTypes.object.isRequired
}
