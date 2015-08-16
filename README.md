# summarize
A simple web service to provide quick summaries of ongoing and completed work

## Key Directories
The following directories are where the key data for this application is stored.  If you're deploying this
app as a [docker container](https://hub.docker.com/r/slugger/summarize/), then you may want to use volumes for one or more of these directories, depending
on if you're running the container as a production app or doing development.

`/var/lib/summarize`: Application (Derby) database is written under this directory.  For a production
container, you almost always want to specify this directory as a data volume so that the database lives
outside of the container and can be persisted between runs.  For development, you can always start with
an empty database by not using a data volume and just storing the db within the container.

`/var/log/summarize`: Application log files are stored in this directory.  Usually sufficies to leave
these logs within the container itself, but if there is a need to archive these logs then you may want
to use a data volume for this location.

## Key Directories in Docker Image
When deploying the app as a docker container, the following directories are where the docker image stores
and reads data from.

`/var/lib/jetty/webapps`: This is where the applications deployed to the Jetty instance
are stored.  For development, you may want to use a data volume from the host so you can replace the
`summarize.war` file and deploy updated versions quickly and easily within the container.  For a production
container, you just want to use the bundled app files in the image.

`/var/lib/jetty/static`: This is where all the static content of the web site is stored
and served from.  Again, if developing, you may want to use a data volume to allow quick & easy editing.
For production, just leave the content as is within the image.

`/usr/local/jetty/bin/stop.sh`: **Use this script to stop the docker container!!**  The Jetty image this
image is built from does not properly shutdown Jetty when using `docker stop ...` and the side effect of
that is the derby db is not properly shutdown.  During dev/testing, I never actually corrupted the db
when using `docker stop`, but it's a possibility.  Executing the `stop.sh` script will properly shutdown
Jetty, which then calls the derby db shutdown code to gracefully close the database.  You should use
something like this:

`docker exec -ti <container_id> /usr/local/jetty/bin/stop.sh`

The above command will shutdown Jetty, shutdown the derby db properly and then terminate your container.

## Docker Details
###Exposed Ports
The following ports are exposed by the docker image:

`8080`: Jetty is listening on this port.

`8000`: When enabled, the Java remote debugger will be listening on this port.  You can attach a remote
Eclipse debugger to this port for debugging purposes.

### Enabling Java Remote Debugger
To enable the Java remote debugging server within the Jetty instance, you must start the container with
the following environment variable set:

`DEBUG_SUMMARIZE=1`

When this env variable is set, Jetty will launch with the Java remote debug server enabled on port 8000.
Be sure to also publish port 8000 from the container to the host so you can connect to the debugger.

### Docker Jetty User
The Jetty server running in the docker container executes as user/group jetty **with uid/gid 999**.  All
the data directories mentioned above must be readable and writable by this user.

###Example Container Launch
`docker run -d -p 8080:8080 -p 8000:8000 -e DEBUG_SUMMARIZE=1 -v /data/summarize:/var/lib/summarize --name summarize slugger/summarize:latest`

**Note for non-docker users:** The war file alone does not contain the static content referenced by the web
pages in this app.  If you're building the war and deploying it in your own app server then you also need
to deploy the static content separately and make it available at the same context used by the docker image.
More details can be provided if necessary, but I expect anyone using this app will just deploy it using
docker.
