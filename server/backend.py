#!/usr/bin/env python

from google.appengine.api import users
from google.appengine.ext import ndb

import time
import math

earth_radius = 6371000
distance_threshold = 100
identical_lift_distance_threshold = 10


# Database models.

# A user who can be waiting at a lift.
class UserInfo(ndb.Model):
    last_lift = ndb.StringProperty(indexed=False)
    last_lift_wait = ndb.IntegerProperty(indexed=False)
    last_update = ndb.IntegerProperty(indexed=False)

# Wait time information about a lift.
class LiftWaitInfo(ndb.Model):
    last_wait = ndb.IntegerProperty(indexed=False)
    last_update = ndb.IntegerProperty(indexed=False)

# List of lifts with geographical information.
class LiftList(ndb.Model):
    lift_list = ndb.JsonProperty(indexed=False)


# Static helpers.

# Get the distance between two points.
def get_distance(lat1, lng1, lat2, lng2):
    [lat1r, lng1r, lat2r, lng2r] = [math.radians(x) for x in [lat1, lng1, lat2, lng2]]
    return earth_radius * math.acos(math.sin(lat1r) * math.sin(lat2r) + math.cos(lat1r) * math.cos(lat2r) * math.cos(lng1r - lng2r))

# Given a location and list of lifts, returns a tuple (name, distance) for the nearest lift, or
# (None, infinity) if there are no lifts.
def get_nearest(lat, lng, acc, lift_list):
    (best_name, best_distance) = (None, float("inf"))
    for name in lift_list:
        (llat, llng) = lift_list[name]
        distance = get_distance(lat, lng, llat, llng)
        if distance < best_distance:
            (best_name, best_distance) = (name, distance)
    return (best_name, best_distance)


# Database helpers.

# Given the location, returns the name of the current lift, or None if not at a lift.
def get_current_lift_name(lat, lng, acc):
    (best_name, best_distance) = get_nearest(lat, lng, acc, get_lift_list_item().lift_list)
    if best_distance < distance_threshold:
        return best_name
    return None

# Updates a particular lift's wait info with the given wait and update time (creating the wait info
# if necessary).
def update_lift(name, wait, update_time):
    lift = ndb.Key(LiftWaitInfo, name).get()
    if lift == None:
        lift = LiftWaitInfo(id=name)
    lift.last_wait = wait
    lift.last_update = update_time
    lift.put()

# Gets the list of geographical lift info (creating it if necessary).
def get_lift_list_item():
    items = LiftList.query().fetch(1)
    if items:
        item = items[0]
    else:
        item = LiftList()
        item.lift_list = {}
    return item

# Adds geographical information about a lift, unless that lift already exists.
def maybe_add_lift(name, lat, lng):
    lift_list_item = get_lift_list_item()

    if name in lift_list_item.lift_list:
        return False

    (best_name, best_distance) = get_nearest(lat, lng, 0, lift_list_item.lift_list)
    if best_distance < identical_lift_distance_threshold:
        return False

    lift_list_item.lift_list.update({name : (lat, lng)})
    lift_list_item.put()
    return True


# Backend methods.

# Returns the list of wait information (string) about all lifts
def get_lift_wait_info_list():
    lifters = LiftWaitInfo.query().fetch(100)
    lift_strings = []
    for lift in lifters:
        # TODO: Make better.
        if not lift.last_wait:
            lift_strings.append(lift.key.id() + ": " + "no data yet")
            continue
        if lift.last_update:
            ago_string = " (" + str(((int) (time.time()) - lift.last_update) / 60) + " min ago)"
        else:
            ago_string = ""
        lift_strings.append(lift.key.id() + ": " + str(lift.last_wait / 60) + " min " + str(lift.last_wait % 60) + " sec" + ago_string)

    return lift_strings

# Returns the list of geographical information (name, lat, lng)) about all lifts
def get_lift_geo_info_list():
    lift_list = get_lift_list_item().lift_list
    tuples = []
    for name in lift_list:
        (lat, lng) = lift_list[name]
        tuples.append((name, lat, lng))
    return tuples

# Posts a location of a particular user. Returns the user's current lift.
def set_user_location(token, lat, lng, accuracy, time):
    # Get the user, or create him if we haven't seen him.
    key = ndb.Key(UserInfo, token)
    user = key.get()
    if user == None:
        user = UserInfo(id=token)

    # Figure out his nearest lift, and update as appropriate.
    current_lift_name = get_current_lift_name(lat, lng, accuracy)
    if current_lift_name == user.last_lift:
        if user.last_lift:
            user.last_lift_wait = user.last_lift_wait + time - user.last_update
        user.last_update = time
    else:
        if user.last_lift:
            print("last lift: " + user.last_lift)
            update_lift(user.last_lift, user.last_lift_wait, user.last_update)
        user.last_lift = current_lift_name
        user.last_lift_wait = 0
        user.last_update = time

    # Save this user.
    user.put()

    return current_lift_name

# Adds a new lift. Returns some kind of status message.
def maybe_add_new_lift(name, lat, lng):
    if maybe_add_lift(name, lat, lng):
        return "Added " + name + " at (" + str(lat) + ", " + str(lng) + ")"
    return "Didn't add, it's already there"
