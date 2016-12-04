import express from 'express'
import bodyParser from 'body-parser'
import path from 'path'

import { renderToString } from 'react-dom/server'

import React from 'react'

const app = express()
app.use('/assets', express.static(path.join(__dirname, './assets')))

const webpack = require('webpack')
const webpackDevMiddleware = require('webpack-dev-middleware')
const webpackHotMiddleware = require('webpack-hot-middleware')
const config = require('./webpack.config')
const compiler = webpack(config)
app.use(webpackDevMiddleware(compiler, {
  noInfo: true,
  publicPath: config.output.publicPath,
}))
app.use(webpackHotMiddleware(compiler))

app.get('/', function(req, res) {
  const page = `
  <!DOCTYPE html>
  <html lang="en">
    <head>
      <meta charset="utf-8" />
      <meta name="viewport" content="width=device-width, initial-scale=1" />
      <link rel="stylesheet" href="assets/fonts/RobotoMono-Bold.ttf" />
      <link rel="stylesheet" href="assets/css/bootstrap.min.css" />
      <link rel="stylesheet" href="assets/css/bootstrap-theme.css" />
      <link rel="stylesheet" href="assets/css/font-awesome.min.css" />
      <link rel="stylesheet" href="assets/css/react-select.min.css" />
      <link rel="stylesheet" href="assets/css/jsoneditor.min.css" />
      <title>CHRONOS</title>
    </head>
    <body>
      <div id="root" />
      <script type="text/javascript" src="assets/js/bundle.js" charset="utf-8"></script>
    </body>
  </html>
  `
  res.status(200).send(page)
})

let jobs = new Map() // jobs are stored here
jobs.set('derpjob', {'name':'derpjob','command':'echo derp','shell':true,'executor':'','executorFlags':'','retries':2,'owner':'','ownerName':'','description':'','successCount':1,'errorCount':0,'lastSuccess':'2016-11-17T00:54:09.777Z','lastError':'','cpus':0.1,'disk':256.0,'mem':128.0,'disabled':false,'softError':false,'dataProcessingJobType':false,'errorsSinceLastSuccess':0,'uris':[],'environmentVariables':[],'arguments':[],'highPriority':false,'runAsUser':'root','constraints':[],'schedule':'R/2016-11-18T00:53:55.000Z/PT24H','scheduleTimeZone':''})
jobs.set('derpjob2', {'name':'derpjob2','command':'echo derp','shell':true,'executor':'','executorFlags':'','retries':2,'owner':'','ownerName':'','description':'','successCount':1,'errorCount':0,'lastSuccess':'2016-10-17T00:54:09.777Z','lastError':'2016-11-17T00:54:09.777Z','cpus':0.1,'disk':256.0,'mem':128.0,'disabled':false,'softError':false,'dataProcessingJobType':false,'errorsSinceLastSuccess':0,'uris':[],'environmentVariables':[],'arguments':[],'highPriority':false,'runAsUser':'root','constraints':[],'schedule':'R/2016-11-18T00:53:55.000Z/PT24H','scheduleTimeZone':''})
jobs.set('derpjob3', {'name':'derpjob3','command':'echo derp','shell':true,'executor':'','executorFlags':'','retries':2,'owner':'','ownerName':'','description':'','successCount':1,'errorCount':0,'lastSuccess':'','lastError':'','cpus':0.1,'disk':256.0,'mem':128.0,'disabled':false,'softError':false,'dataProcessingJobType':false,'errorsSinceLastSuccess':0,'uris':[],'environmentVariables':[],'arguments':[],'highPriority':false,'runAsUser':'root','constraints':[],'schedule':'R/2016-11-18T00:53:55.000Z/PT24H','scheduleTimeZone':''})
jobs.set('derpdependency', {'name':'derpdependency','command':'echo heeerrrrrrp','shell':true,'executor':'','executorFlags':'','retries':2,'owner':'','ownerName':'','description':'','successCount':0,'errorCount':0,'lastSuccess':'','lastError':'','cpus':0.1,'disk':256.0,'mem':128.0,'disabled':false,'softError':false,'dataProcessingJobType':false,'errorsSinceLastSuccess':0,'uris':[],'environmentVariables':[],'arguments':[],'highPriority':false,'runAsUser':'root','constraints':[],'parents':['derpjob']})
jobs.set('derpjob4', {'name':'derpjob4','command':'echo derp','shell':true,'executor':'','executorFlags':'','retries':2,'owner':'','ownerName':'','description':'','successCount':1,'errorCount':0,'lastSuccess':'','lastError':'','cpus':0.1,'disk':256.0,'mem':128.0,'disabled':true,'softError':false,'dataProcessingJobType':false,'errorsSinceLastSuccess':0,'uris':[],'environmentVariables':[],'arguments':[],'highPriority':false,'runAsUser':'root','constraints':[],'schedule':'R/2016-11-18T00:53:55.000Z/PT24H','scheduleTimeZone':''})
jobs.set('ajob', {'name':'ajob','schedule':'','command':'echo derp','shell':true,'executor':'','executorFlags':'','retries':2,'owner':'','ownerName':'','description':'','successCount':1,'errorCount':0,'lastSuccess':'2016-11-17T00:54:09.777Z','lastError':'','cpus':0.1,'disk':256.0,'mem':128.0,'disabled':false,'softError':false,'dataProcessingJobType':false,'errorsSinceLastSuccess':0,'uris':[],'environmentVariables':[],'arguments':[],'highPriority':false,'runAsUser':'root','constraints':[],'scheduleTimeZone':''})
jobs.set('bjob', {'name':'bjob','schedule':'','command':'echo derp','shell':true,'executor':'','executorFlags':'','retries':2,'owner':'','ownerName':'','description':'','successCount':1,'errorCount':0,'lastSuccess':'2016-11-17T00:54:09.777Z','lastError':'','cpus':0.1,'disk':256.0,'mem':128.0,'disabled':false,'softError':false,'dataProcessingJobType':false,'errorsSinceLastSuccess':0,'uris':[],'environmentVariables':[],'arguments':[],'highPriority':false,'runAsUser':'root','constraints':[],'scheduleTimeZone':''})
jobs.set('cjob', {'name':'cjob','schedule':'','command':'echo derp','shell':true,'executor':'','executorFlags':'','retries':2,'owner':'','ownerName':'','description':'','successCount':1,'errorCount':0,'lastSuccess':'2016-11-17T00:54:09.777Z','lastError':'','cpus':0.1,'disk':256.0,'mem':128.0,'disabled':false,'softError':false,'dataProcessingJobType':false,'errorsSinceLastSuccess':0,'uris':[],'environmentVariables':[],'arguments':[],'highPriority':false,'runAsUser':'root','constraints':[],'scheduleTimeZone':''})
jobs.set('djob', {'name':'djob','schedule':'','command':'echo derp','shell':true,'executor':'','executorFlags':'','retries':2,'owner':'','ownerName':'','description':'','successCount':1,'errorCount':0,'lastSuccess':'2016-11-17T00:54:09.777Z','lastError':'','cpus':0.1,'disk':256.0,'mem':128.0,'disabled':false,'softError':false,'dataProcessingJobType':false,'errorsSinceLastSuccess':0,'uris':[],'environmentVariables':[],'arguments':[],'highPriority':false,'runAsUser':'root','constraints':[],'scheduleTimeZone':''})

app.use(bodyParser.json())

app.get('/v1/scheduler/jobs', function(req, res) {
  let arr = []
  jobs.forEach(j => arr.push(j))
  res.setHeader('Content-Type', 'application/json')
  res.status(200).send(JSON.stringify(arr))
})

app.get('/v1/scheduler/job/:jobName', function(req, res) {
  res.setHeader('Content-Type', 'application/json')
  res.status(200).send(JSON.stringify(jobs.get(req.params.jobName)))
})

app.get('/v1/scheduler/jobs/summary', function(req, res) {
  let arr = []
  jobs.forEach(j => {
    let status = 'fresh'
    if (!j.lastSuccess) {
      status = 'failure'
    }
    if (!j.lastError) {
      status = 'success'
    }
    if (j.lastError > j.lastSuccess) {
      status = 'failure'
    }
    let state = 'idle'
    if (Math.floor((Math.random() * 10) + 1) == 1) {
      state = Math.floor((Math.random() * 10) + 1) + ' running'
    } else if (Math.floor((Math.random() * 10) + 1) == 1) {
      state = 'queued'
    }
    let now = new Date()
    if (j.name === 'ajob') {
      now.setSeconds(now.getSeconds() + 30)
      j.schedule = 'R/' + now.toISOString() + '/PT24H'
    }
    if (j.name === 'bjob') {
      now.setHours(now.getHours() + 2)
      j.schedule = 'R/' + now.toISOString() + '/PT24H'
    }
    if (j.name === 'cjob') {
      now.setDate(now.getDate() + 3)
      j.schedule = 'R/' + now.toISOString() + '/PT24H'
    }
    if (j.name === 'djob') {
      now.setSeconds(now.getSeconds() + 300)
      j.schedule = 'R/' + now.toISOString() + '/PT24H'
    }
    let summary = {
      name: j.name,
      status: status,
      state: state,
      schedule: j.schedule,
      parents: j.parents,
      disabled: j.disabled,
    }
    arr.push(summary)
  })
  res.setHeader('Content-Type', 'application/json')
  res.status(200).send(JSON.stringify({'jobs':arr}))
})

app.post('/v1/scheduler/iso8601', function(req, res) {
  console.log('Got POST for scheduled job')
  console.log(req.body)
  var job = req.body
  res.setHeader('Content-Type', 'application/json')
  if (job instanceof Object) {
    jobs.set(job.name, job)
    console.log(`Updated jobs (${jobs.size})`)
    res.status(201).send(JSON.stringify({ success: true }))
  } else {
    res.status(200).send(JSON.stringify({ success: false, error: 'expected `jobs` to be array' }))
  }
})

app.post('/v1/scheduler/dependency', function(req, res) {
  console.log('Got POST for dependent job')
  console.log(req.body)
  var job = req.body
  res.setHeader('Content-Type', 'application/json')
  if (job instanceof Object) {
    jobs.set(job.name, job)
    console.log(`Updated jobs (${jobs.size})`)
    res.status(201).send(JSON.stringify({ success: true }))
  } else {
    res.status(200).send(JSON.stringify({ success: false, error: 'expected `jobs` to be array' }))
  }
})

// example of handling 404 pages
app.get('*', function(req, res) {
  res.status(404).send('Server.js > 404 - Page Not Found')
})

// global error catcher, need four arguments
app.use((err, req, res, next) => {
  console.error('Error on request %s %s', req.method, req.url)
  console.error(err.stack)
  res.status(500).send('Server error')
})

process.on('uncaughtException', evt => {
  console.log('uncaughtException: ', evt)
})

app.listen(3000, function(){
  console.log('Listening on port 3000')
})
