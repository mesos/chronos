import os
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
    def hello_world():
        return render_template('index.html'), 200


    @app.route('/create')
    def create_job():
        return render_template('create.html'), 200

    @app.route('/view')
    # View a page that lists all jobs.
    def view_jobs():
        return render_template('index.html'), 200

    @app.route('/details')
    # View the details for one particular job, including statistics
    def view_details():
        return render_template('')

    return app

if __name__ == '__main__':
    app = create_app()
    port = int(os.environ.get("PORT", 4399))
    app.run(host='0.0.0.0', port=port)
