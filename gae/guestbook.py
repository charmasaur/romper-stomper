#!/usr/bin/env python

import os
import urllib

from google.appengine.api import users
from google.appengine.ext import ndb

import time
import math
import jinja2
import webapp2

lifts = [
    ("Blue Calf", -36.3845236, 148.3715626),
    ("Blue Cow", -36.382989, 148.379258),
    ("Carpark", -36.377766, 148.375920),
    ("Freedom", -36.385398, 148.378838),
    ("Early Starter", -36.383761, 148.394574),
    ("Summit", -36.381758, 148.397523),
    ("Terminal", -36.383041, 148.397376),
    ("Ridge", -36.378971, 148.407400),
    ("Brumby", -36.385608, 148.399890),
    ("Pleasant Valley", -36.387208, 148.399514),
    ("North Perisher", -36.392348, 148.409069),
    ("Interceptor", -36.394566, 148.409089),
    ("Sun Valley", -36.406811, 148.395866),
    ("Happy Valley", -36.408446, 148.398216),
    ("Mt Perisher Chairs", -36.410348, 148.400238),
    ("International", -36.413069, 148.395704),
    ("Eyre", -36.415602, 148.391805),
    ]

earth_radius = 6371000
distance_threshold = 100


class Fellow(ndb.Model):
    last_lift = ndb.StringProperty(indexed=False)
    last_lift_wait = ndb.IntegerProperty(indexed=False)
    last_update = ndb.IntegerProperty(indexed=False)

class Lift(ndb.Model):
    last_wait = ndb.IntegerProperty(indexed=False)
    last_update = ndb.IntegerProperty(indexed=False)


def get_distance(lat1, lng1, lat2, lng2):
    [lat1r, lng1r, lat2r, lng2r] = [math.radians(x) for x in [lat1, lng1, lat2, lng2]]
    return earth_radius * math.acos(math.sin(lat1r) * math.sin(lat2r) + math.cos(lat1r) * math.cos(lat2r) * math.cos(lng1r - lng2r))

def is_distance_within(lat1, lng1, lat2, lng2):
    return get_distance(lat1, lng1, lat2, lng2) < distance_threshold

def get_nearest(lat, lng, acc):
    for (name, llat, llng) in lifts:
        if is_distance_within(lat, lng, llat, llng):
            return name
    return None

def update_lift(name, wait, update_time):
    lift = ndb.Key(Lift, name).get()
    if lift == None:
        lift = Lift(id=name)
    lift.last_wait = wait
    lift.last_update = update_time
    lift.put()

class Here(webapp2.RequestHandler):

    def get(self):
        token = self.request.get('token', '')
        lat = float(self.request.get('lat', '0.0'))
        lng = float(self.request.get('lng', '0.0'))
        acc = float(self.request.get('acc', '0.0'))
        tim = int(self.request.get('tim', '0'))

        if lat == 0.0 and lng == 0.0:
            if token == '':
                delimiter = '<br>'
            else:
                delimiter = "|"
            nearest = None
        else:
            delimiter = "|"
            # Get the fellow, or create him if we haven't seen him.
            key = ndb.Key(Fellow, token)
            fellow = key.get()
            if fellow == None:
                fellow = Fellow(id=token)

            # Figure out his nearest lift, and update as appropriate.
            nearest = get_nearest(lat, lng, acc)
            if nearest == fellow.last_lift:
                if fellow.last_lift:
                    fellow.last_lift_wait = fellow.last_lift_wait + tim - fellow.last_update
                fellow.last_update = tim
            else:
                if fellow.last_lift:
                    print("last lift: " + fellow.last_lift)
                    update_lift(fellow.last_lift, fellow.last_lift_wait, fellow.last_update) 
                fellow.last_lift = nearest
                fellow.last_lift_wait = 0
                fellow.last_update = tim

            # Save this fellow.
            fellow.put()

        lifters = Lift.query().fetch(100)
        if nearest:
            self.response.write("You are at: " + nearest + "|")
        for lift in lifters:
            if lift.last_update:
                ago_string = " (" + str(((int) (time.time()) - lift.last_update) / 60) + " min ago)"
            else:
                ago_string = ""
            self.response.write(lift.key.id() + ": " + str(lift.last_wait / 60) + " min " + str(lift.last_wait % 60) + " sec" + ago_string + delimiter)


app = webapp2.WSGIApplication([
    ('/here', Here),
], debug=True)
