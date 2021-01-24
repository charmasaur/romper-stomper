import datetime
import json
import os
import time

from flask import abort, request, render_template
from flask_sqlalchemy import SQLAlchemy

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
    token = request.args.get('token')
    if not token:
        return "You need to provide a token"
    native = request.args.get('native')

    path = CyclePath.query.filter_by(token=token).first()
    if path:
        point_list = json.loads(path.point_list)
    else:
        point_list = None

    if native:
        if point_list == None:
            return ""
        return str([[lat, lng, stamp] for (lat, lng, _, stamp) in point_list])
    else:
        if point_list == None:
            return "Don't have any points yet, try again later"
        return render_template(
            "cycle.html",
            point_list=point_list,
            API_KEY=get_api_key())


@app.route("/cycle_submit", methods=['GET'])
def cycle_submit():
    token = request.args.get('token', '')
    lat = float(request.args.get('lat', '0.0'))
    lng = float(request.args.get('lng', '0.0'))
    acc = float(request.args.get('acc', '0.0'))
    # tim is UTC seconds
    tim = int(request.args.get('tim', '0'))

    if lat == 0.0 and lng == 0.0:
        return

    path = CyclePath.query.filter_by(token=token).first()
    if path == None:
        path = CyclePath(
            token=token,
            expiry_date=datetime.datetime.today() + EXPIRY_TIME,
        )
        db.session.add(path)
        point_list = []
    else:
        point_list = json.loads(path.point_list)

    # we assume the list is already sorted (in increasing order of time), so to keep it that
    # way we just need to look backwards through the list until we find something not bigger
    # than the new value (and then insert the new value after that element)
    index = len(point_list) - 1
    while index >= 0:
        if len(point_list[index]) < 4 or point_list[index][3] <= tim:
            break
        index = index - 1
    point_list.insert(index + 1, (lat, lng, time.ctime(tim + 11 * 60 * 60), tim))

    path.point_list = json.dumps(point_list)
    db.session.commit()
    return "done"

@app.route("/remove_expired", methods=['GET'])
def remove_expired():
    date = datetime.datetime.today()
    query = CyclePath.query(
            CyclePath.expiry_date != None,
            CyclePath.expiry_date < date)

    ndb.delete_multi(query.iter(keys_only=True))
    self.response.write("Deleted " + str(query.count()) + " entries");

@app.route("/dump", methods=['GET'])
def dump():
    return "<br>".join([str((path.token, path.point_list, path.expiry_date))
                     for path in CyclePath.query.all()])
