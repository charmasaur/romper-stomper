#!/usr/bin/env python

import os
import urllib

import jinja2
import webapp2

import backend

JINJA_ENVIRONMENT = jinja2.Environment(
    loader=jinja2.FileSystemLoader(os.path.dirname(__file__)),
    extensions=['jinja2.ext.autoescape'],
    autoescape=True)

def get_api_key():
    f = open('api_key.txt', 'r')
    return f.read()


class Here(webapp2.RequestHandler):
    def get(self):
        token = self.request.get('token', '')
        lat = float(self.request.get('lat', '0.0'))
        lng = float(self.request.get('lng', '0.0'))
        acc = float(self.request.get('acc', '0.0'))
        tim = int(self.request.get('tim', '0'))

        if not lat == 0.0 or not lng == 0.0:
            # We're being notified of a position from a mobile client.
            current_lift = backend.set_user_location(token, lat, lng, acc, tim)
            if current_lift:
                self.response.write("You are at: " + current_lift + "|")

        wait_infos = backend.get_lift_wait_info_list()
        # Now send a list of lifts to ...
        if token == '':
            # ... a web client
            delimiter = '<br>'
        else:
            # ... a mobile client
            delimiter = '|'
        for wait_info in wait_infos:
            self.response.write(wait_info + delimiter)


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

        result = backend.maybe_add_new_lift(name, lat, lng)
        self.response.write(result)


class ListLifts(webapp2.RequestHandler):
    def get(self):
        template = JINJA_ENVIRONMENT.get_template('list.html')
        lifts = backend.get_lift_geo_info_list()
        for (name, lat, lng) in lifts:
            self.response.write(name + ": (" + str(lat) + ", " + str(lng) + ")<br>")
        self.response.write(template.render({'list' : lifts, 'API_KEY' : get_api_key()}))


app = webapp2.WSGIApplication([
    ('/here', Here),
    ('/add', Add),
    ('/add_internal', AddInternal),
    ('/list_lifts', ListLifts),
], debug=True)
