# Over-The-Air Deployment (OTA) Service

### Motivation

To enable Over-the-Air deployment of iOS apps to an Apple device you need a web server providing...
* an HTML file with an absolute (itms-services) link to...
* a PLIST file pointing to an absolute URL of the...
* IPA file to be deployed.

In enterprise scenarios like the following the need of absolute URLs causes many issues.
Assume you have a central CI and release build of iOS Apps. During the build besides the IPA (iOS App deployable) file also the OTA HTML page and PLIST file is generated.
OTA deployement will work from the CI server (e.g. Hudson). But as soon as the artifacts (IPA, HTML and PLIST) are deployed to a central repository (like Nexus in the Maven case) the absolute URLs will still point to the CI server which is not a stable location. The correct approach would be the html file in the central repository would point the the corresponding PLIST and IPA in the central repository.

That's why we implemented the OTA Service.
* Our Xcode-Maven-Plugin build generates a (build) html file (see "Build HTML Template" section below) which redirects to the OTA (HTML) Service.
* The OTA HTML Service on-the-fly generates a page containing an itms-services link to the OTA (PLIST) Service.
* The OTA PLIST Service on-the-fly generates the plist file containing the URL to the IPA file.
* OTA Service also generates QRCodes on-the-fly which enables direct installation of apps or opening the install page without the need to enter the URL.

### Configuration in Tomcat

* The ios-service.war can simply be deployed to the Tomcat/webapps folder
* If `<Host [...] copyXML="true">` is configured in the server.xml the default context config of the Application is copied to <br>`<Tomcat>/conf/Catalina/localhost/ota-service.xml`
* **Since iOS 7.1 OTA service has to be used with https!** The the server hosting OTA Service has to have https enabled **and** has to have a certificate accepted by iOS. If you use a self signed certificate the appropriate (CA) certificate has to be installed on the device.

**Parameters in `ota-service.xml`:**
* `htmlTemplatePath`: The absolute path to your custom HTML template (the template **must not be named "template.html"!**)
* `applicationBaseUrl`: The base URL of the ota-service application. If specified always this base URL is used instead of deriving it from the request. This is useful to enforce enforce https, even if the ota-service is called with http. Example "https://myhost:8443/ota-service".
* `debug`: if "true" the template is reloaded for each request (helpful for testing).
* Any additional custom parameters can be used inside the template.

**HTML Template**
The default `template.html` included in the OTA Service is located in `modules/ota-library/src/main/resources`.
Jquery has to be downloaded to the OTA server in order to comply with the same-origin policy (http://en.wikipedia.org/wiki/Same-origin_policy).
Download from e.g. http://code.jquery.com/jquery-latest.pack.js and save it in <webapps>/ota.libraries/jquery-latest.pack.js.

You can use the following built-in properties in your custom HTML (Velocity) template:
* `$title`: The title of the App.
* `$bundleIdentifier`: The BundleIdentifier of the App.
* `$bundleVersion`: The BundleVersion of the App.
* `$ipaUrl`: The URL to the IPA file.
* `$plistUrl`: The URL to the PLIST Service. The itms-services link for OTA deployment should use this URL.<br>
  E.g. `<a href='itms-services:///?action=download-manifest&url=$plistUrl'>Install Over-the-air</a>`
* `$plistUrl?action=qrcode`: URL to the (dynamically generated) "direct install" QRCode. Add this as "src" of an `<img>` tag to display the QRCode.
* `$htmlQrcodeUrl`: URL to the QRCode pointing to the install page itself. Add this as "src" of an `<img>` tag to display the QRCode.
* `$<yourCustomParameter>`: Any other custom parameters defined in the ota-service.xml can be used as well

### Build HTML Template
To use an OTA Service for deployment of your Apps you have to provide the `.ipa` file via http(s) and place an `.html` file next to the IPA file with the same name (only different extension).<br>
E.g. `http://server1:1080/Store/MyApp.ipa` and `http://server1:1080/Store/MyApp.html`.
The default `buildTemplate.html` file in `modules/ota-library/src/main/resources` shows how a template could look like:
* Important is the iFrame refering to the OTA Service (example src: https://server2:8443/ota-service/HTML?title=MyApp&bundleIdentifier=org.company.mybundleidentifier&bundleVersion=1.0.0)
  * `title`: The title of the App
  * `bundleIdentifier`: The BundleIdentifier of the App.
  * `bundleVersion`: The BundleVersion of the App.
  * `<yourCustomParameter>`: You can provide any additional parameters which can be used in the OTA HTML Template.
* And optional URL parameter is `removeOuterFrame` which can be set to "true" or "false". This parameter specifies if the Window shall be reloaded with the iFrame content only or if the iFrame shall be kept together with this outer HTML page.
* To enable https for the server providing the IPA file the `Referer` has to be added as URL parameter. In the default template you can see how to do this using javascript.

### License ###

This project is copyrighted by [SAP AG](http://www.sap.com/) and made available under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html). Please also confer to the text files "LICENSE" and "NOTICE" included with the project sources.
