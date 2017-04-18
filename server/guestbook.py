#!/usr/bin/env python

import os

from google.appengine.ext import ndb

import time
import jinja2
import webapp2
import datetime

JINJA_ENVIRONMENT = jinja2.Environment(
    loader=jinja2.FileSystemLoader(os.path.dirname(__file__)),
    extensions=['jinja2.ext.autoescape'],
    autoescape=True)

EXPIRY_TIME = datetime.timedelta(days=30)

class CyclePath(ndb.Model):
    point_list = ndb.JsonProperty(indexed=False)
    expiry_date = ndb.DateTimeProperty(indexed=True)

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

class Dump(webapp2.RequestHandler):
    def get(self):
        token = self.request.get('token', '')
        if not token:
            self.response.write("You need to provide a token")
            return

        path = ndb.Key(CyclePath, token).get()

        if path == None:
            self.response.write("Don't have any points yet, try again later")
            return
        self.response.write(path.point_list)

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
            path.expiry_date = datetime.datetime.today() + EXPIRY_TIME
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

class RemoveExpired(webapp2.RequestHandler):
    def get(self):
        date = datetime.datetime.today()
        query = CyclePath.query(
                CyclePath.expiry_date != None,
                CyclePath.expiry_date < date)

        ndb.delete_multi(query.iter(keys_only=True))
        self.response.write("Deleted " + str(query.count()) + " entries");

class List(webapp2.RequestHandler):
    def get(self):
        query = CyclePath.query().order(-CyclePath.expiry_date)

        response = ""
        for result in query.iter():
            response += ("<a href=\"http://localhost:8080/cycler?token=" + result.key.id() + "\">"
                    + result.key.id() + " " + str(result.expiry_date) + "</a><br>")
        self.response.write(response)

class DumpAll(webapp2.RequestHandler):
    def get(self):
        query = CyclePath.query()

        self.response.write([str(result.point_list) for result in query.fetch(5)])

app = webapp2.WSGIApplication([
    ('/cycler', Cycler),
    ('/dump', Dump),
    ('/cycle_submit', CycleSubmit),
    ('/remove_expired', RemoveExpired),
    ('/list', List),
    ('/dump_all', DumpAll),
], debug=True)
