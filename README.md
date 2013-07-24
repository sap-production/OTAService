# Over-The-Air Deployment (OTA) Service

### Motivation

To enable over-the-air deployment of iOS apps to an Apple device you need
* A web server providing...
* an HTML file with an absolute (itms-services) link to...
* a PLIST file pointing to an absolute URL of the...
* IPA file to be deployed.

In enterprise scenarios like the following the need of absolute URLs causes many issues.
Assume you have a central CI and release build of iOS Apps. During the build besides the IPA (iOS App deployable) file also the OTA HTML page and PLIST file is generated.
OTA deployement will work from the CI server (e.g. Hudson). But as soon as the artifacts (IPA, HTML and PLIST) are deployed to a central repository (like Nexus in the Maven case) the absolute URLs will still point to the CI server which is not a stable location. The correct approach would be the html file in the central repository would point the the corresponding PLIST and IPA in the central repository.

That's why we implemented the OTA Service.
* Our build generates an (build) html file which redirects to the OTA (HTML) Service
* The OTA HTML Service on-the-fly generates a page containing an itms-services link to the OTA (PLIST) Service.
* The OTA PLIST Service on-the-fly generates the plist file containing the URL to the IPA file

The "build html" file has to be located in parallel to the IPA file and the and the html file has to have the same name as the IPA file, except for the ending which has to be "-ota.htm".
This way the over-the-air deployment works on the CI server as well as from the central repository as well as from any other Web Server (e.g an internal app store).

### Configuration in Tomcat

* The ios-service.war can simply be deployed to the Tomcat/webapps folder
* If &lt;Host [...] copyXML="true"&gt; is configured in the server.xml the default context config of the Application is copied to <br>&lt;Tomcat&gt;/conf/Catalina/localhost/ota-service.xml

**Parameters in ota-service.xml:**
* htmlTemplatePath: The absolute path to your custom HTML template.
* You can define any other custom parameters

**HTML Template**
You can use the following properties in your HTML (Velocity) template:
* $title: The title of the App.
* $bundleIdentifier: The BundleIdentifier of the App.
* $ipaUrl: The URL to the IPA file.
* $plistUrl: The URL to the PLIST Service. The itms-services link for OTA deployment should use this URL.<br>
  E.g. &lt;a href='itms-services:///?action=download-manifest&url=$plistUrl'&gt;Install Over-the-air&lt;/a&gt;
* $&lt;yourCustomParameter&gt;: Any other custom parameters defined in the ota-service.xml can be used as well

### License ###

This project is copyrighted by [SAP AG](http://www.sap.com/) and made available under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html). Please also confer to the text files "LICENSE" and "NOTICE" included with the project sources.
