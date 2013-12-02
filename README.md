# Minecraft App

#### Version 0.8 Beta

The Minecraft App lets you visualize the minecraft world from the guts side. Wondering how many blocks have been dug up by your buddies? Not a problem. Wondering who's found the most diamonds? Yep, got it covered. Have you been planting enough wheat? Carrots? Pototoes? The Minecraft App will let you know.

### Getting Started
This section provides information about installing and using the Minecraft App. 

#### Requirements

* Operating System: Windows, Linux, or Mac OS X.
* Web browsers: Latest versions of Chrome, Safari, or Firefox, Internet Explorer 9 or later. 
* Craftbukkit: The open source minecraft server from [Craftbukkit](http://bukkit.org/)
* LogToSplunk Plugin: The log to splunk plugin that allows input of more detailed minecraft data to splunk from [CraftBukkit](http://dev.bukkit.org/bukkit-plugins/logtosplunk/)
* The Splunk Web Framework: The Web Framework is included in Splunk 6 and is available for Splunk 5 from the 
[Splunk Developer Portal](http://dev.splunk.com/view/webframework-standalone/SP-CAAAEMA).
* Minecraft Overviewer (Optional): The Google Maps based minecraft word renderer from [Overviewer](http://overviewer.org)

#### Installing the Minecraft App 
The Minecraft App is built as a Splunk App on the Splunk Web Framework and must be installed on top of it. 

##### Installing from Splunk Web
If you downloaded the Minecraft App from [Splunk Apps](http://apps.splunk.com), you can install the app within Splunk Web. 

* For more, see [Where to get more apps and add-ons](http://docs.splunk.com/Documentation/Splunk/latest/Admin/Wheretogetmoreapps).

##### Installing from a ZIP Source File

1. [Download and unzip the Minecraft App](https://github.com/splunk/minecraft-app/archive/develop.zip) 
or clone the repository from [GitHub](https://github.com/splunk/minecraft-app.git). 
2. Copy the entire `/minecraft-app` subdirectory into `$SPLUNK_HOME/etc/apps/`. 
3. Restart Splunk.
4. In Splunk Web, navigate to the Minecraft App (*http://localhost:8000/dj/minecraft-app*).

##### Input Configuration

1. Confgiure a TCP input as noted in the [Documentation](http://docs.splunk.com/Documentation/Splunk/6.0/Data/Monitornetworkports) for whatever port you like.
2. Manually set the source type to "minecraft_log"
3. Ensure firewalls and NAT are properly configured if applicable


#### Installing the LogToSplunk Plugin

1. Downlod the jar from [Craftbukkit](http://dev.bukkit.org/bukkit-plugins/logtosplunk/)
2. Copy the LogToSplunk jar into your craftbukkit server's `/plugins` folder.
3. Create a `/LogToSplunk` directory in the `/plugins` folder
4. Create a and edit `/config.yml` text file in the `/LogToSplunk` directory created in 3
   * The text file should contain one line in the format of "splunks: \<Splunk Server\>:\<TCP Input Port\>" (ie. "splunks: localhost:8888"). Multiple servers can be added as comma separated values between the server:port entries. A port must be specified.

#### Configuring The Livemap

1. Download and configure Overview as described int the [Overviewer Docs](http://docs.overviewer.org/en/latest/)
2. Serve the overviewer via a webserver like [Apache](http://httpd.apache.org) or [IIS](http://www.iis.net). Many operating systems have a web service built in that just needs to be enabled.
3. Create an initial render from overviewer with at least one of the "Normal","Lighting", and "Night" options
4. Copy the overviewer.css,overviewer.js, and overviewerConfig.js scripts from the base render directory to `$SPLUNK_HOME/etc/apps/minecraft-app/django/minecraft-app/static/minecraft-app/` on your splunk server.
5. Edit `$SPLUNK_HOME/etc/apps/minecraft-app/django/minecraft-app/static/minecraft-app/overviewerConfig.js` and modify the path variable of each tileset object to include your webserver path. For example, change `"path": "world-normal"` to `"path": "http://webserver:81/world-normal"`. The external hostname must be used in order for the map to be visible to clients. Using "localhost" as the webserver will not work as the minecraft app does not reserve the map, it simply redirects to it.

NOTE: The minecraft-app does not refresh overviewer renders automatically. This will need to be scheduled by another service (ie. cron or task scheduler).


#### Known Issues

1) LogToSplunk does not maintain connecton across Splunk restarts. The minecraft server must be restarted if Splunk is restarted
2) Time calculations and active players may be mis-reported if player disconnects are not logged properly (ie. due to a server crash). Orphaned sessions may be estimated by running sessions from connection to the subsequent server start.
3) The live map may appear to "shift" as the minecraft world expands and overviewer resets it's origin in future renders. This can be corrected by recopying and modifying the overviewerConfig.js script with the same steps as the installation.



## Documentation and resources

When you need to know more:

* For CraftBukkit documentation, see [Craftbukkit](http://bukkit.org/)

* For Overviewer documentation, see [Overviewer](http://overviewer.org)

* For all things developer with Splunk, your main resource is the [Splunk Developer Portal](http://dev.splunk.com).

* For component reference documentation, see the [Splunk Web Framework Reference](http://docs.splunk.com/Documentation/WebFramework).

* For more about Splunk in general, see [Splunk>Docs](http://docs.splunk.com/Documentation/Splunk).


### How to contribute

If you would like to contribute to the Minecraft App, go here for more information:

* [Minecraft App Github](https://github.com/splunk/minecraft-app)

Please feel free to open issues and provide feedback through GitHub Issues.

## License
The Minecraft_App is licensed under the Apache License 2.0. Details can be found in the LICENSE file.



