# coding: utf-8

import werkzeug.datastructures

from jinja2 import Markup
from flask import request, session, current_app
from wtforms.fields import HiddenField
from wtforms.widgets import HiddenInput
from wtforms.validators import ValidationError
from wtforms.ext.csrf.form import SecureForm
from ._compat import text_type, string_types
from .csrf import generate_csrf, validate_csrf

try:
    from .i18n import translations
except:
    translations = None


class _Auto():
    '''Placeholder for unspecified variables that should be set to defaults.

    Used when None is a valid option and should not be replaced by a default.
    '''
    pass


def _is_hidden(field):
    """Detect if the field is hidden."""
    if isinstance(field, HiddenField):
        return True
    if isinstance(field.widget, HiddenInput):
        return True
    return False


class Form(SecureForm):
    """
    Flask-specific subclass of WTForms **SecureForm** class.

    If formdata is not specified, this will use flask.request.form.
    Explicitly pass formdata = None to prevent this.

    :param csrf_context: a session or dict-like object to use when making
                         CSRF tokens. Default: flask.session.

    :param secret_key: a secret key for building CSRF tokens. If this isn't
                       specified, the form will take the first of these
                       that is defined:

                       * SECRET_KEY attribute on this class
                       * WTF_CSRF_SECRET_KEY config of flask app
                       * SECRET_KEY config of flask app
                       * session secret key

    :param csrf_enabled: whether to use CSRF protection. If False, all
                         csrf behavior is suppressed.
                         Default: WTF_CSRF_ENABLED config value
    """
    SECRET_KEY = None
    TIME_LIMIT = 3600

    def __init__(self, formdata=_Auto, obj=None, prefix='', csrf_context=None,
                 secret_key=None, csrf_enabled=None, *args, **kwargs):

        if csrf_enabled is None:
            csrf_enabled = current_app.config.get('WTF_CSRF_ENABLED', True)

        self.csrf_enabled = csrf_enabled

        if formdata is _Auto:
            if self.is_submitted():
                formdata = request.form
                if request.files:
                    formdata = formdata.copy()
                    formdata.update(request.files)
                elif request.json:
                    formdata = werkzeug.datastructures.MultiDict(request.json)
            else:
                formdata = None

        if self.csrf_enabled:
            if csrf_context is None:
                csrf_context = session
            if secret_key is None:
                # It wasn't passed in, check if the class has a SECRET_KEY
                secret_key = getattr(self, "SECRET_KEY", None)

            self.SECRET_KEY = secret_key
        else:
            csrf_context = {}
            self.SECRET_KEY = ''
        super(Form, self).__init__(formdata, obj, prefix,
                                   csrf_context=csrf_context,
                                   *args, **kwargs)

    def generate_csrf_token(self, csrf_context=None):
        if not self.csrf_enabled:
            return None
        return generate_csrf(self.SECRET_KEY, self.TIME_LIMIT)

    def validate_csrf_token(self, field):
        if not self.csrf_enabled:
            return True
        if hasattr(request, 'csrf_valid') and request.csrf_valid:
            # this is validated by CsrfProtect
            return True
        if not validate_csrf(field.data, self.SECRET_KEY, self.TIME_LIMIT):
            raise ValidationError(field.gettext('CSRF token missing'))

    def validate_csrf_data(self, data):
        """Check if the csrf data is valid.

        .. versionadded: 0.9.0

        :param data: the csrf string to be validated.
        """
        return validate_csrf(data, self.SECRET_KEY, self.TIME_LIMIT)

    def is_submitted(self):
        """
        Checks if form has been submitted. The default case is if the HTTP
        method is **PUT** or **POST**.
        """

        return request and request.method in ("PUT", "POST")

    def hidden_tag(self, *fields):
        """
        Wraps hidden fields in a hidden DIV tag, in order to keep XHTML
        compliance.

        .. versionadded:: 0.3

        :param fields: list of hidden field names. If not provided will render
                       all hidden fields, including the CSRF field.
        """

        if not fields:
            fields = [f for f in self if _is_hidden(f)]

        rv = [u'<div style="display:none;">']
        for field in fields:
            if isinstance(field, string_types):
                field = getattr(self, field)
            rv.append(text_type(field))
        rv.append(u"</div>")

        return Markup(u"".join(rv))

    def validate_on_submit(self):
        """
        Checks if form has been submitted and if so runs validate. This is
        a shortcut, equivalent to ``form.is_submitted() and form.validate()``
        """
        return self.is_submitted() and self.validate()

    def _get_translations(self):
        if not current_app.config.get('WTF_I18N_ENABLED', True):
            return None
        return translations
