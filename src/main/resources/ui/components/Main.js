import React from 'react'
import JobSummaryView from './JobSummaryView'
import JobEditor from './JobEditor'
import {observer} from 'mobx-react'

@observer
export default class Main extends React.Component {
  derp(event) {
    console.log(event)
  }

  constructor(props) {
    super(props);
    this.state = {
      filterString: '',
    };
    this.handleFilterChange = this.handleFilterChange.bind(this);
  }

  handleFilterChange(event) {
    this.setState({filterString: event.target.value});
  }

  render() {
    const jobSummaryStore = this.props.jobSummaryStore
    return (
      <div className="container">
        <div className="panel panel-default">
          <div className="container-fluid panel-heading">
            <div className="pull-left">
              <JobEditor jobSummaryStore={jobSummaryStore}/>
            </div>
            <div className="pull-right">
              <label>
                Filter Jobs:
                <input
                  type="text"
                  value={ this.state.filterString }
                  onChange={this.handleFilterChange}/>
              </label>
            </div>
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
          <JobSummaryView jobs={this.getVisibleJobs().filter(e => e.name.includes(this.state.filterString))}/>
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
