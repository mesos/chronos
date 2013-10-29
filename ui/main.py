import os
import sys
import json
import requests
from models.jobs import DependentJob, ScheduledJob
from collections import defaultdict
from flask import Flask, render_template, send_from_directory
from flask_wtf import Form
from wtforms import TextField, HiddenField, ValidationError, RadioField, BooleanField, SubmitField
from wtforms.validators import Required
def create_app():
    app = Flask(__name__)
    app.config.update(DEBUG = True,)

    @app.route('/favicon.ico')
    def favicon():
        return send_from_directory(os.path.join(app.root_path, 'static'), 'ico/favicon.ico')

    @app.errorhandler(404)
    def page_not_found(e):
        return render_template('404.html'), 404

    @app.route('/')
    def view_jobs():
        entries = gen_table_entries()
        total_jobs=len(entries)
        failed_jobs=len(entries.filter(lambda entry: entry.lastStatus == "failed"))
        return render_template('index.html', 
                               total_jobs=total_jobs,
                               failed_jobs=failed_jobs,
                               entries=entries), 200

    @app.route('/create')
    def create_job():
        return render_template('create.html'), 200

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
            parents = j["parents"]
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
