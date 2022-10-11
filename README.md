# meural-control

This project provides ability to push content to a Meural display from an external URL.  It provides a webserver and services with swagger documented endpoints that can be leveraged to control the meural and push arbitrary content.  The user/pw of the netgear account need to be provided so we can fetch the meural information and control it locally.

It leverages Meural's API which their web-interface and mobile app run on. Documentation can be found <a href="https://documenter.getpostman.com/view/1657302/RVnWjKUL#intro/">here.</a><br>

There is also an http server that runs directly on the Meural device, which is used to post content directly to.  This project currently uses
`/remote/postcard/`<br>

### application.properties
An application.properties file is necessary to be placed into `src/main/java/resources`.  It should contain a few properties:

server.port:[<i>port for this application to run on. Example:8081</i>]<br>
logbackserver=[<i>Optional IP/port for a logback server to get events.  If not defined, modify `logback.xml` to log to stdout or a file.  Example: 192.168.0.7:5671</i><br>
meural-api=https://api.meural.com/v0/ <br>
meural-account=[<i>your netgear acct email<i>]<br>
meural-password=[<i>your netgear acct password</i>]<br>

### Swagger
Swagger UI is available at `http://127.0.0.1:8081/swagger-ui/index.html`.  It describes the webservice endpoints and allows usage directly in the browser.