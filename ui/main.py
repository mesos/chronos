import os
import sys
import json
import requests
import re
from string import strip
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
            print r.status_code
            print r.headers
            print r.text
        return render_jobs()

    @app.route('/kill', methods=["POST"])
    def kill_job():
        job_name = request.form['name']
        print "Killed job %s" % job_name
        url = "http://%s/scheduler/task/kill/%s" % (CHRONOS_HOST, job_name)
        r = requests.delete(url)
        if (r.status_code != 204):
            print "Did not receive HTTP 204 back from killing %s" % job_name
            print r.status_code
            print r.headers
            print r.text
        return render_jobs()

    @app.route('/run', methods=["POST"])
    def run_job():
        job_name = request.form['name']
        print "Ran job %s" % job_name
        url = "http://%s/scheduler/job/%s" % (CHRONOS_HOST, job_name)
        r = requests.put(url)
        if (r.status_code != 204):
            print "Did not receive HTTP 204 back from manually starting %s" % job_name
            print r.status_code
            print r.headers
            print r.text
        return render_jobs()

    @app.route('/create', methods=["POST"])
    def create_job():
        job_hash = {}
        job_hash["async"] = False
        job_hash["epsilon"] = "PT30M"
        job_hash["executor"] = ""
        job_hash["disabled"] = True if str(request.form["newStatusInput"]) == "disabled" else False
        job_hash["command"] = str(request.form["newCommandInput"])
        job_hash["name"] = str(request.form["newNameInput"])
        job_hash["owner"] = str(request.form["newOwnerInput"])

        if 'newParentsInput' in request.form:
            # This is a dependent job
            job_hash["parents"] = map(strip, str(request.form["newParentsInput"]).split(","))
            url = "http://%s/scheduler/dependency" % CHRONOS_HOST
        else:
            # This is a scheduled job
            job_hash["schedule"] = "R%s/%sT%sZ/P%s" % (str(request.form["newRepeatsInput"]),
                                                       str(request.form["newDateInput"]),
                                                       str(request.form["newTimeInput"]),
                                                       str(request.form["newPeriodInput"]))
            url = "http://%s/scheduler/iso8601" % CHRONOS_HOST

        headers = {"Content-Type": "application/json"}
        r = requests.post(url, data=json.dumps(job_hash), headers=headers)
        if (r.status_code != 204):
            print "Job hash is: %s" % str(job_hash)
            print "Json'd data is %s" % json.dumps(job_hash)
            print r.status_code
            print r.headers
            print r.text
            print "Did not receive HTTP 204 back from creating %s" % job_hash["name"]
        print "Created job %s" % job_hash["name"]
        return render_jobs()

    @app.route('/edit', methods=["POST"])
    def edit_job():
        print "in the edit job method"
        job_hash = {}
        job_hash["async"] = False
        job_hash["epsilon"] = "PT30M"
        job_hash["executor"] = ""
        job_hash["disabled"] = True if str(request.form["statusInput"]) == "disabled" else False
        job_hash["command"] = str(request.form["commandInput"])
        job_hash["name"] = str(request.form["nameInput"])
        job_hash["owner"] = str(request.form["ownerInput"])

        if 'parentsInput' in request.form:
            # This is a dependent job
            job_hash["parents"] = map(strip, str(request.form["parentsInput"]).split(","))
            url = "http://%s/scheduler/dependency" % CHRONOS_HOST
        else:
            # This is a scheduled job
            job_hash["schedule"] = "R%s/%sT%sZ/P%s" % (str(request.form["repeatsInput"]),
                                                       str(request.form["dateInput"]),
                                                       str(request.form["timeInput"]),
                                                       str(request.form["periodInput"]))
            url = "http://%s/scheduler/iso8601" % CHRONOS_HOST

        headers = {"Content-Type": "application/json"}
        r = requests.put(url, data=json.dumps(job_hash), headers=headers)
        if (r.status_code != 204):
            print "Job hash is: %s" % str(job_hash)
            print "Json'd data is %s" % json.dumps(job_hash)
            print r.status_code
            print r.headers
            print r.text
            print "Did not receive HTTP 204 back from creating %s" % job_hash["name"]
        print "Edited job %s" % job_hash["name"]
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
