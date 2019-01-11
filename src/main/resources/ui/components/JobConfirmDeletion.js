import React from 'react'
import {observer} from 'mobx-react'
import 'bootstrap'

@observer
class JobConfirmDeletion extends React.Component {
  constructor(props){
    super(props)
  }

  deleteJob(){
    this.props.doRequest(
      this.props.jobToDelete.event.currentTarget,
      'DELETE',
      'v1/scheduler/job/' + encodeURIComponent(this.props.jobToDelete.job.name)
    );
    $('#job-confirm-deletion-modal').modal('hide');
    this.props.callback(this.props.jobToDelete.job.name);
  }

  render() {
    console.log(this.props.jobToDelete)
    return (
      <div className="modal fade" id='job-confirm-deletion-modal' tabIndex="-1" role="dialog"
           aria-labelledby="mymodallabel">
        <div className="modal-dialog custom-modal" role="document">
          <div className="modal-content">
            <div className="modal-header">
              <button type="button" className="close" data-dismiss="modal" aria-label="close"><span
                aria-hidden="true">&times;</span></button>
              <h4 className="modal-title">Confirm delete</h4>
            </div>

            <div className="modal-body">
              <p>Do you really want to delete the
                job {this.props.jobToDelete.job ? this.props.jobToDelete.job.name : ""} ?</p>
            </div>
            <div className="modal-footer">
              <button type="button" className="btn btn-danger">Cancel</button>
              <button type="button"
                      className="btn btn-success btn-secondary" onClick={() => this.deleteJob()}>
                Confirm
              </button>
            </div>
          </div>
        </div>
      </div>
    )
  }
}

export default JobConfirmDeletion
