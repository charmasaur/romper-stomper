# Server for romper

The Dockerfile encapsulates the main part of the server. It assumes access to environent variables
`MAPBOX_API_KEY` (the Mapbox API key, obviously), `DATABASE_URL` (URL for an external SQL database),
and `PORT` (port on which the server will run).
