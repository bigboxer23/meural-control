# meural-control

This project provides ability to push content to a Meural display from an external URL.  It provides a webserver and services with swagger documented endpoints that can be leveraged to control the meural and push arbitrary content.  The user/pw of the netgear account need to be provided so we can fetch the meural information and control it locally.

It leverages Meural's API which their web-interface and mobile app run on. Documentation can be found <a href="https://documenter.getpostman.com/view/1657302/RVnWjKUL#intro/">here.</a><br>

There is also an http server that runs directly on the Meural device, which is used to post content directly to.  This project currently uses
`/remote/postcard/` & `/remote/control_check/sleep` commands<br>

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
host=[hostname/IP where to publish to when `mvn package` is run]

## Swagger
Swagger UI is available at `http://127.0.0.1:8081/swagger-ui/index.html`.  It describes the webservice endpoints and allows usage directly in the browser.

## Installation
1) Run `install.sh`
   1) This is intended to be installed onto a raspberry pi or some other local server.  `install.sh` will install it to a
   host of your choosing.  The script needs to have the appropriate host variable set for where installation is desired.
3) Google photos authentication
   1) When the app initially launches, it will print a URL to the console which needs to be navigated to and proper authorizations
   given.  The browser this URL is launched in needs to be on the same machine as the meural-control is installed on.  If that 
   isn't possible (for headless installations for instance), it is possible to run the OAuth flow on a non-headless server and
   authenticate, and transfer the generated `tokens/StoredCredentials` to the appropriate directory on the remote headless machine.
   `install.sh` contains a commented line that does exactly this.
5) Running on launch of server (assuming raspberry pi)
   1) Two lines need to be added to the `/etc/rc.local` file before `exit 0`
      1) ```
         cd /home/pi/com/bigboxer23/meural-control/1.0
         nohup java -jar /home/pi/com/bigboxer23/meural-control/1.0/meural-control-1.0.jar &
7) Other
   1) Java needs to be available and installed (java 8+)
      1) On raspberry pi run `sudo apt install default-jdk`
