#!/usr/bin/env python

import os
import urllib

from google.appengine.api import users
from google.appengine.ext import ndb

import time
import math
import jinja2
import webapp2

JINJA_ENVIRONMENT = jinja2.Environment(
    loader=jinja2.FileSystemLoader(os.path.dirname(__file__)),
    extensions=['jinja2.ext.autoescape'],
    autoescape=True)

class Here(webapp2.RequestHandler):

    def get(self):
        token = self.request.get('token', '')
        lat = float(self.request.get('lat', '0.0'))
        lng = float(self.request.get('lng', '0.0'))
        acc = float(self.request.get('acc', '0.0'))
        tim = int(self.request.get('tim', '0'))

        if lat == 0.0 and lng == 0.0:
            # This is a request for a list of lifts ...
            if token == '':
                # ... from a web client
                # TODO: get list of lifts from the backend and send them in the response
            else:
                # ... from a mobile client
                # TODO: get a list of lifts and send them in the response
            current_lift_name = None
        else:
            # We're being notified of a position from a mobile client.
            # TODO: send (lat, lng, acc, tim, token) to backend for updating
            # TODO: get a list of lifts and send them in the response


class Add(webapp2.RequestHandler):
    def get(self):
        template = JINJA_ENVIRONMENT.get_template('add.html')
        self.response.write(template.render({'API_KEY' : get_api_key()}))


class AddInternal(webapp2.RequestHandler):
    def get(self):
        name = self.request.get('name', '')
        try:
            lat = float(self.request.get('lat', '0.0'))
            lng = float(self.request.get('lng', '0.0'))
        except ValueError:
            self.response.write("Improper request")
            return

        if name == '' or lat == 0.0 or lng == 0.0:
            self.response.write("Improper request")
            return

        # TODO: send (lat, lng, name) to backend for updating, and get response and forward it

class ListLifts(webapp2.RequestHandler):
    def get(self):
        template = JINJA_ENVIRONMENT.get_template('list.html')
        # TODO: get list of lifts from the backend, and send them


app = webapp2.WSGIApplication([
    ('/here', Here),
    ('/add', Add),
    ('/add_internal', AddInternal),
    ('/list_lifts', ListLifts),
], debug=True)
