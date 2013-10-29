# -*- coding: utf-8 -*-
"""
    flask_wtf
    ~~~~~~~~~

    Flask-WTF extension

    :copyright: (c) 2010 by Dan Jacob.
    :license: BSD, see LICENSE for more details.
"""
# flake8: noqa
from __future__ import absolute_import

from .form import Form
from .csrf import CsrfProtect
from .recaptcha import *

__version__ = '0.9.3'
