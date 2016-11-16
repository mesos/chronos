import React from 'react'
import Header from './Header'
import Main from './Main'
import Footer from './Footer'
import {render} from 'react-dom'
import {JobSummaryStore} from '../stores/JobSummaryStore'

var observableJobSummaryStore = new JobSummaryStore()

class Root extends React.Component {
  render () {
    const jobSummaryStore = observableJobSummaryStore
    return (
      <div>
        <Header title='CHRONOS' />
        <Main jobSummaryStore={jobSummaryStore} />
        <Footer />
      </div>
    )
  }
}

render(<Root/>, document.getElementById('root'));
