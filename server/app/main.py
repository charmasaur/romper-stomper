import datetime
import os
import time

from flask import abort, request, render_template, url_for, make_response
from flask_sqlalchemy import SQLAlchemy
import base64
import imghdr

from app.app import app, db


EXPIRY_TIME = datetime.timedelta(days=30)

class CyclePath(db.Model):
    __tablename__ = "cycle_paths"

    token = db.Column(db.String, primary_key=True)
    # JSON string
    point_list = db.Column(db.String)
    expiry_date = db.Column(db.DateTime)

def get_api_key():
    return os.getenv("MAPBOX_API_KEY", "")


@app.route('/cycler', methods=['GET'])
def cycler():
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

@app.route("/cycle_submit", methods=['GET'])
def cycle_submit():
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

@app.route("/remove_expired", methods=['GET'])
def remove_expired():
    date = datetime.datetime.today()
    query = CyclePath.query(
            CyclePath.expiry_date != None,
            CyclePath.expiry_date < date)

    ndb.delete_multi(query.iter(keys_only=True))
    self.response.write("Deleted " + str(query.count()) + " entries");
