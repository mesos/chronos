# coding: utf-8
"""
    flask_wtf.csrf
    ~~~~~~~~~~~~~~

    CSRF protection for Flask.

    :copyright: (c) 2013 by Hsiaoming Yang.
"""

import os
import hmac
import hashlib
import time
from flask import current_app, session, request, abort
from ._compat import to_bytes
try:
    from urlparse import urlparse
except ImportError:
    # python 3
    from urllib.parse import urlparse


__all__ = ('generate_csrf', 'validate_csrf', 'CsrfProtect')


def generate_csrf(secret_key=None, time_limit=None):
    """Generate csrf token code.

    :param secret_key: A secret key for mixing in the token,
                       default is Flask.secret_key.
    :param time_limit: Token valid in the time limit,
                       default is 3600s.
    """
    if not secret_key:
        secret_key = current_app.config.get(
            'WTF_CSRF_SECRET_KEY', current_app.secret_key
        )

    if not secret_key:
        raise Exception('Must provide secret_key to use csrf.')

    if time_limit is None:
        time_limit = current_app.config.get('WTF_CSRF_TIME_LIMIT', 3600)

    if 'csrf_token' not in session:
        session['csrf_token'] = hashlib.sha1(os.urandom(64)).hexdigest()

    if time_limit:
        expires = time.time() + time_limit
        csrf_build = '%s%s' % (session['csrf_token'], expires)
    else:
        expires = ''
        csrf_build = session['csrf_token']

    hmac_csrf = hmac.new(
        to_bytes(secret_key),
        to_bytes(csrf_build),
        digestmod=hashlib.sha1
    ).hexdigest()
    return '%s##%s' % (expires, hmac_csrf)


def validate_csrf(data, secret_key=None, time_limit=None):
    """Check if the given data is a valid csrf token.

    :param data: The csrf token value to be checked.
    :param secret_key: A secret key for mixing in the token,
                       default is Flask.secret_key.
    :param time_limit: Check if the csrf token is expired.
                       default is True.
    """
    if not data or '##' not in data:
        return False

    expires, hmac_csrf = data.split('##', 1)
    try:
        expires = float(expires)
    except:
        return False

    if time_limit is None:
        time_limit = current_app.config.get('WTF_CSRF_TIME_LIMIT', 3600)

    if time_limit:
        now = time.time()
        if now > expires:
            return False

    if not secret_key:
        secret_key = current_app.config.get(
            'WTF_CSRF_SECRET_KEY', current_app.secret_key
        )

    if 'csrf_token' not in session:
        return False

    csrf_build = '%s%s' % (session['csrf_token'], expires)
    hmac_compare = hmac.new(
        to_bytes(secret_key),
        to_bytes(csrf_build),
        digestmod=hashlib.sha1
    ).hexdigest()
    return hmac_compare == hmac_csrf


class CsrfProtect(object):
    """Enable csrf protect for Flask.

    Register it with::

        app = Flask(__name__)
        CsrfProtect(app)

    And in the templates, add the token input::

        <input type="hidden" name="csrf_token" value="{{ csrf_token() }}"/>

    If you need to send the token via AJAX, and there is no form::

        <meta name="csrf_token" content="{{ csrf_token() }}" />

    You can grab the csrf token with JavaScript, and send the token together.
    """

    def __init__(self, app=None):
        self._exempt_views = set()

        if app:
            self.init_app(app)

    def init_app(self, app):
        app.jinja_env.globals['csrf_token'] = generate_csrf
        strict = app.config.get('WTF_CSRF_SSL_STRICT', True)
        csrf_enabled = app.config.get('WTF_CSRF_ENABLED', True)

        @app.before_request
        def _csrf_protect():
            # many things come from django.middleware.csrf
            if not csrf_enabled:
                return

            if request.method in ('GET', 'HEAD', 'OPTIONS', 'TRACE'):
                return

            if self._exempt_views:
                if not request.endpoint:
                    return

                view = app.view_functions.get(request.endpoint)
                if not view:
                    return

                dest = '%s.%s' % (view.__module__, view.__name__)
                if dest in self._exempt_views:
                    return

            csrf_token = None
            if request.method in ('POST', 'PUT', 'PATCH'):
                csrf_token = request.form.get('csrf_token')
            if not csrf_token:
                # You can get csrf token from header
                # The header name is the same as Django
                csrf_token = request.headers.get('X-CSRFToken')
            if not csrf_token:
                # The header name is the same as Rails
                csrf_token = request.headers.get('X-CSRF-Token')
            if not validate_csrf(csrf_token):
                reason = 'CSRF token missing or incorrect.'
                return self._error_response(reason)

            if request.is_secure and strict:
                if not request.referrer:
                    reason = 'Referrer checking failed - no Referrer.'
                    return self._error_response(reason)

                good_referrer = 'https://%s/' % request.host
                if not same_origin(request.referrer, good_referrer):
                    reason = 'Referrer checking failed - origin not match.'
                    return self._error_response(reason)

            request.csrf_valid = True  # mark this request is csrf valid

    def exempt(self, view):
        """A decorator that can exclude a view from csrf protection.

        Remember to put the decorator above the `route`::

            csrf = CsrfProtect(app)

            @csrf.exempt
            @app.route('/some-view', methods=['POST'])
            def some_view():
                return
        """
        view_location = '%s.%s' % (view.__module__, view.__name__)
        self._exempt_views.add(view_location)
        return view

    def _error_response(self, reason):
        return abort(400, reason)

    def error_handler(self, view):
        """A decorator that set the error response handler.

        It accepts one parameter `reason`::

            @csrf.error_handler
            def csrf_error(reason):
                return render_template('error.html', reason=reason)

        By default, it will return a 400 response.
        """
        self._error_response = view
        return view


def same_origin(current_uri, compare_uri):
    parsed_uri = urlparse(current_uri)
    parsed_compare = urlparse(compare_uri)

    if parsed_uri.scheme != parsed_compare.scheme:
        return False

    if parsed_uri.hostname != parsed_compare.hostname:
        return False

    if parsed_uri.port != parsed_compare.port:
        return False
    return True
