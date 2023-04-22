from flask import Flask
from flask_sqlalchemy import SQLAlchemy

app = Flask(__name__, static_url_path='')
app.config.from_object("app.config.Config")
db = SQLAlchemy(app)
