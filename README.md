# meural-control

This project provides ability to push content to a Meural display from an external URL.  It provides a webserver and services with swagger documented endpoints that can be leveraged to control the meural and push arbitrary content.  The user/pw of the netgear account need to be provided so we can fetch the meural information and control it locally.

It leverages Meural's API which their web-interface and mobile app run on. Documentation can be found <a href="https://documenter.getpostman.com/view/1657302/RVnWjKUL#intro/">here.</a><br>

There is also an http server that runs directly on the Meural device, which is used to post content directly to.  This project currently uses
`/remote/postcard/`<br>

## Content Sources
Presently google photos is the sole supported content source (other than passing an arbitrary URL to the web server's API endpoint).
Each of the sources will iterate through its content and display a new item from the source each hour.

### Google Photos
To setup google photos integration, navigate to <a href='https://developers.google.com/photos/library/guides/get-started-java'>here</a>
and follow instructions to create a `credentials.json` file.  This file should be placed within the `src/main/resources` directory.
Once the server starts up, there will be a URL printed to the console logs which needs to be navigated to in a browser.  It will
prompt you to log into your Google account and request the appropriate scopes for the integration.  Once this successfully completes, 
a token will be stored on the server so log-in is not necessary again (unless the token is revoked manually).

Within the application.properteis file, there is a required property, `gPhotos-albumTitle`, which defines the Google Photos album
to push content from.  

## application.properties
An application.properties file is necessary to be placed into `src/main/java/resources`.  It should contain a few properties:

server.port:[<i>port for this application to run on. Example:8081</i>]<br>
logbackserver=[<i>Optional IP/port for a logback server to get events.  If not defined, modify `logback.xml` to log to stdout or a file.  Example: 192.168.0.7:5671</i><br>
meural-api=https://api.meural.com/v0/ <br>
meural-account=[<i>your netgear acct email<i>]<br>
meural-password=[<i>your netgear acct password</i>]<br>
gPhotos-albumTitle=[<i>Album Name Example: Art</i>]<br>

## Swagger
Swagger UI is available at `http://127.0.0.1:8081/swagger-ui/index.html`.  It describes the webservice endpoints and allows usage directly in the browser.