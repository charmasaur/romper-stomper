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

class LiftList(ndb.Model):
    lift_list = ndb.JsonProperty(indexed=False)


def get_distance(lat1, lng1, lat2, lng2):
    [lat1r, lng1r, lat2r, lng2r] = [math.radians(x) for x in [lat1, lng1, lat2, lng2]]
    return earth_radius * math.acos(math.sin(lat1r) * math.sin(lat2r) + math.cos(lat1r) * math.cos(lat2r) * math.cos(lng1r - lng2r))

# Returns a tuple (name, distance) for the nearest lift, or (None, infinity) if there are no lifts.
def get_nearest(lat, lng, acc):
    (best_name, best_distance) = (None, float("inf"))
    for (name, llat, llng) in lifts:
        distance = get_distance(lat, lng, llat, llng)
        if distance < best_distance:
            (best_name, best_distance) = (name, distance)
    return (best_name, best_distance)

# Returns the name of the current lift, or None if not at a lift.
def get_current_lift_name(lat, lng, acc):
    (best_name, best_distance) = get_nearest(lat, lng, acc)
    if best_distance < distance_threshold:
        return best_name
    return None

def update_lift(name, wait, update_time):
    lift = ndb.Key(Lift, name).get()
    if lift == None:
        lift = Lift(id=name)
    lift.last_wait = wait
    lift.last_update = update_time
    lift.put()

def get_lift_list_item():
    items = LiftList.query().fetch(1)
    if items:
        item = items[0]
    else:
        item = LiftList()
        item.lift_list = {}
    return item

def maybe_add_lift(name, lat, lng):
    lift_list_item = get_lift_list_item()

    if name in lift_list_item.lift_list:
        return False
    # TODO: See if there are any lifts with the same location too.


    lift_list_item.lift_list.update({name : (lat, lng)})
    lift_list_item.put()
    return True


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
            current_lift_name = None
        else:
            delimiter = "|"
            # Get the fellow, or create him if we haven't seen him.
            key = ndb.Key(Fellow, token)
            fellow = key.get()
            if fellow == None:
                fellow = Fellow(id=token)

            # Figure out his nearest lift, and update as appropriate.
            current_lift_name = get_current_lift_name(lat, lng, acc)
            if current_lift_name == fellow.last_lift:
                if fellow.last_lift:
                    fellow.last_lift_wait = fellow.last_lift_wait + tim - fellow.last_update
                fellow.last_update = tim
            else:
                if fellow.last_lift:
                    print("last lift: " + fellow.last_lift)
                    update_lift(fellow.last_lift, fellow.last_lift_wait, fellow.last_update) 
                fellow.last_lift = current_lift_name
                fellow.last_lift_wait = 0
                fellow.last_update = tim

            # Save this fellow.
            fellow.put()

        lifters = Lift.query().fetch(100)
        if current_lift_name:
            self.response.write("You are at: " + current_lift_name + "|")
        for lift in lifters:
            # TODO: Make better.
            if not lift.last_wait:
                self.response.write(lift.key.id() + ": " + "no data yet" + delimiter)
                continue
            if lift.last_update:
                ago_string = " (" + str(((int) (time.time()) - lift.last_update) / 60) + " min ago)"
            else:
                ago_string = ""
            self.response.write(lift.key.id() + ": " + str(lift.last_wait / 60) + " min " + str(lift.last_wait % 60) + " sec" + ago_string + delimiter)


class Add(webapp2.RequestHandler):
    def get(self):
        template = JINJA_ENVIRONMENT.get_template('add.html')

        f = open('api_key.txt', 'r')
        api_key = f.read()
        self.response.write(template.render({'API_KEY' : api_key}))


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

        if maybe_add_lift(name, lat, lng):
            self.response.write("Added " + name + " at (" + str(lat) + ", " + str(lng) + ")")
        else:
            self.response.write("Didn't add, it's already there")


app = webapp2.WSGIApplication([
    ('/here', Here),
    ('/add', Add),
    ('/add_internal', AddInternal),
], debug=True)
