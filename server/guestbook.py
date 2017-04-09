#!/usr/bin/env python

import os

from google.appengine.ext import ndb

import time
import jinja2
import webapp2

JINJA_ENVIRONMENT = jinja2.Environment(
    loader=jinja2.FileSystemLoader(os.path.dirname(__file__)),
    extensions=['jinja2.ext.autoescape'],
    autoescape=True)

class CyclePath(ndb.Model):
    point_list = ndb.JsonProperty(indexed=False)

def get_api_key():
    f = open('api_key.txt', 'r')
    return f.read()


class Cycler(webapp2.RequestHandler):
    def get(self):
        token = self.request.get('token', '')
        if not token:
            self.response.write("You need to provide a token")
            return
        native = self.request.get('native', '')

        path = ndb.Key(CyclePath, token).get()

        if native:
            if path == None:
                self.response.write("")
                return
            self.response.write([[lat, lng, stamp] for (lat, lng, _, stamp) in path.point_list])
            return
        else:
            if path == None:
                self.response.write("Don't have any points yet, try again later")
                return
            template = JINJA_ENVIRONMENT.get_template('cycle.html')
            self.response.write(template.render(
                {'list' : path.point_list, 'API_KEY' : get_api_key()}))
            return

class CycleSubmit(webapp2.RequestHandler):
    def get(self):
        token = self.request.get('token', '')
        lat = float(self.request.get('lat', '0.0'))
        lng = float(self.request.get('lng', '0.0'))
        acc = float(self.request.get('acc', '0.0'))
        # tim is UTC seconds
        tim = int(self.request.get('tim', '0'))

        if lat == 0.0 and lng == 0.0:
            return

        path = ndb.Key(CyclePath, token).get()
        if path == None:
            path = CyclePath(id=token)
            path.point_list = []
        # we assume the list is already sorted (in increasing order of time), so to keep it that
        # way we just need to look backwards through the list until we find something not bigger
        # than the new value (and then insert the new value after that element)
        index = len(path.point_list) - 1
        while index >= 0:
            if len(path.point_list[index]) < 4 or path.point_list[index][3] <= tim:
                break
            index = index - 1
        path.point_list.insert(index + 1, (lat, lng, time.ctime(tim + 11 * 60 * 60), tim))
        path.put()

app = webapp2.WSGIApplication([
    ('/cycler', Cycler),
    ('/cycle_submit', CycleSubmit),
], debug=True)
