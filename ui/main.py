import os
import sys
import json
import requests
import re
from models.jobs import DependentJob, ScheduledJob
from collections import defaultdict
from flask import Flask, render_template, send_from_directory, request
def create_app():
    app = Flask(__name__)
    app.config.update(DEBUG = True,)

    def render_jobs():
        entries = gen_table_entries()
        total_jobs=len(entries)
        failed_jobs=len(filter(lambda entry: entry.lastStatus == "Failed", entries))
        return render_template('index.html',
                               chronos_host=CHRONOS_HOST,
                               total_jobs=total_jobs,
                               failed_jobs=failed_jobs,
                               entries=entries), 200


    @app.route('/favicon.ico')
    def favicon():
        return send_from_directory(os.path.join(app.root_path, 'static'), 'ico/favicon.ico')

    @app.errorhandler(404)
    def page_not_found(e):
        return render_template('404.html'), 404

    @app.route('/')
    def view_jobs():
        return render_jobs()

    @app.route('/delete', methods=["POST"])
    def delete_job():
        job_name = request.form['name']
        print "Deleted job %s" % job_name
        url = "http://%s/scheduler/job/%s" % (CHRONOS_HOST, job_name)
        r = requests.delete(url)
        if (r.status_code != 204):
            print "Did not receive HTTP 204 back from deleting %s" % job_name
        return render_jobs()

    @app.route('/kill', methods=["POST"])
    def kill_job():
        job_name = request.form['name']
        print "Killed job %s" % job_name
        url = "http://%s/scheduler/task/kill/%s" % (CHRONOS_HOST, job_name)
        r = requests.delete(url)
        if (r.status_code != 204):
            print "Did not receive HTTP 204 back from killing %s" % job_name
        return render_jobs()

    @app.route('/run', methods=["POST"])
    def run_job():
        job_name = request.form['name']
        print "Ran job %s" % job_name
        url = "http://%s/scheduler/job/%s" % (CHRONOS_HOST, job_name)
        r = requests.put(url)
        if (r.status_code != 204):
            print "Did not receive HTTP 204 back from manually starting %s" % job_name
        return render_jobs()

    @app.route('/create', methods=["POST"])
    def create_job():
        print 'In this method!'
        job_hash = request.form
        print "Created job %s" % job_hash
        return render_jobs()

    @app.route('/edit', methods=["POST"])
    def edit_job():
        job_hash = request.form
        print "Edited job %s" % job_hash
        return render_jobs()

    return app

def get_job_details():
    base_path = "http://" + CHRONOS_HOST + "/scheduler/jobs"
    job_details = {} # name mapping to job object
    resp = requests.get(base_path)
    data = json.loads(resp.content)
    for j in data:
        name = j["name"]
        owner = j["owner"]
        command = j["command"]
        retries = j["retries"]
        lastSuccess = j["lastSuccess"]
        lastError = j["lastError"]
        successCount = j["successCount"]
        errorCount = j["errorCount"]
        disabled = j["disabled"]

        if 'parents' in j:
            # This is a dependent job.
            parents = map(str, j["parents"])
            job = DependentJob(name, owner, command, retries, lastSuccess, lastError,
                               successCount, errorCount, disabled, parents=parents)
            job_details[name] = job
        elif 'schedule' in j:
            # This is a scheduled job.
            schedule = j["schedule"]
            job = ScheduledJob(name, owner, command, retries, lastSuccess, lastError,
                               successCount, errorCount, disabled, schedule=schedule)
            job_details[name] = job
        else:
            raise AssertionError("Job did not fit into dependent or scheduled categories")
            pass

    return job_details

def gen_table_entries():
    job_details = get_job_details()
    job_stats = get_job_stats()
    for (job_name, job) in job_details.iteritems():
        job.stats = job_stats[job_name]
    return job_details.values()

def get_job_stats():
    base_path = "http://" + CHRONOS_HOST + "/scheduler/stats/"
    all_stats = defaultdict(dict)
    for rank in ["99thPercentile", 
                 "95thPercentile",
                 "75thPercentile",
                 "median"]:
        path = base_path + rank
        resp = requests.get(path)
        data = json.loads(resp.content)
        for nametime in data:
            name = nametime["jobNameLabel"]
            time = nametime["time"]
            all_stats[name][rank] = time

    return all_stats


if __name__ == '__main__':
    CHRONOS_HOST = sys.argv[1]
    app = create_app()
    port = int(os.environ.get("PORT", 4399))
    app.run(host='0.0.0.0', port=port)
