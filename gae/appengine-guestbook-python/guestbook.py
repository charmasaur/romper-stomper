#!/usr/bin/env python
# TOM IS TOM
# Copyright 2016 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# [START imports]
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

JINJA_ENVIRONMENT = jinja2.Environment(
    loader=jinja2.FileSystemLoader(os.path.dirname(__file__)),
    extensions=['jinja2.ext.autoescape'],
    autoescape=True)
# [END imports]

DEFAULT_GUESTBOOK_NAME = 'default_guestbook'


# We set a parent key on the 'Greetings' to ensure that they are all
# in the same entity group. Queries across the single entity group
# will be consistent. However, the write rate should be limited to
# ~1/second.

def guestbook_key(guestbook_name=DEFAULT_GUESTBOOK_NAME):
    """Constructs a Datastore key for a Guestbook entity.

    We use guestbook_name as the key.
    """
    return ndb.Key('Guestbook', guestbook_name)


# [START greeting]
class Author(ndb.Model):
    """Sub model for representing an author."""
    identity = ndb.StringProperty(indexed=False)
    email = ndb.StringProperty(indexed=False)


class Greeting(ndb.Model):
    """A main model for representing an individual Guestbook entry."""
    author = ndb.StructuredProperty(Author)
    content = ndb.StringProperty(indexed=False)
    date = ndb.DateTimeProperty(auto_now_add=True)
# [END greeting]

class Fellow(ndb.Model):
    last_lift = ndb.StringProperty(indexed=False)
    last_lift_wait = ndb.IntegerProperty(indexed=False)
    last_update = ndb.IntegerProperty(indexed=False)

class Lift(ndb.Model):
    last_wait = ndb.IntegerProperty(indexed=False)
    last_update = ndb.IntegerProperty(indexed=False)

# [START main_page]
class MainPage(webapp2.RequestHandler):

    def get(self):
        guestbook_name = self.request.get('guestbook_name',
                                          DEFAULT_GUESTBOOK_NAME)
        greetings_query = Greeting.query(
            ancestor=guestbook_key(guestbook_name)).order(-Greeting.date)
        greetings = greetings_query.fetch(10)

        user = users.get_current_user()
        if user:
            url = users.create_logout_url(self.request.uri)
            url_linktext = 'Logout'
        else:
            url = users.create_login_url(self.request.uri)
            url_linktext = 'Login'

        template_values = {
            'user': user,
            'greetings': greetings,
            'guestbook_name': urllib.quote_plus(guestbook_name),
            'url': url,
            'url_linktext': url_linktext,
        }

        template = JINJA_ENVIRONMENT.get_template('index.html')
        self.response.write(template.render(template_values))
# [END main_page]


# [START guestbook]
class Guestbook(webapp2.RequestHandler):

    def post(self):
        # We set the same parent key on the 'Greeting' to ensure each
        # Greeting is in the same entity group. Queries across the
        # single entity group will be consistent. However, the write
        # rate to a single entity group should be limited to
        # ~1/second.
        guestbook_name = self.request.get('guestbook_name',
                                          DEFAULT_GUESTBOOK_NAME)
        greeting = Greeting(parent=guestbook_key(guestbook_name))

        if users.get_current_user():
            greeting.author = Author(
                    identity=users.get_current_user().user_id(),
                    email=users.get_current_user().email())

        greeting.content = self.request.get('content')
        greeting.put()

        query_params = {'guestbook_name': guestbook_name}
        self.redirect('/?' + urllib.urlencode(query_params))
# [END guestbook]

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

# [START guestbook]
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
# [END guestbook]


# [START app]
app = webapp2.WSGIApplication([
    ('/', MainPage),
    ('/sign', Guestbook),
    ('/here', Here),
], debug=True)
# [END app]
