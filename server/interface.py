# TODO: Where should the divide be? Is it legit that every time we decide to
# send a new bit of information (say we want to send a wait time plus an
# uncertainty), we have to update both the frontend and backend (and maybe even
# the client too)? I think the client should be really dumb. Like really dumb.
# So that sort of thing shouldn't require a client change. I also think the
# frontend should just be about the actual transport of data -- making sure
# requests are well-formed, extracting parameters, calling appropriate backend
# methods (the frontend knows that a "Here" request sometimes means "here's a
# location", and sometimes means "hey, give me lifts"; the backend shouldn't
# care about that).
# So this suggests that the backend should be providing the information to show,
# and the frontend just converts that to a form appropriate for sending to the
# various clients.

# Returns the list of wait information about all lifts
def get_lift_wait_info_list():
    return None

# Returns the list of geographical information about all lifts
def get_lift_geo_info_list():
    return None

# Posts a location of a particular user
def set_user_location(token, lat, lng, accuracy, time):
    return None

# Adds a new lift. Returns some kind of status message.
def maybe_add_new_lift(name, lat, lng):
    return None
