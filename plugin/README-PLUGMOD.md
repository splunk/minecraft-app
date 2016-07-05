# Overview

Run `mvn clean package` from the plugin directory to package the LogToSplunk plugmod.

The forge spigot plugin that this project builds can be found in `logtosplunk-plugin/target` and is called `logtosplunk-plugin-{version}.jar`

Currently the supported veresion of minecraft are 1.10 for spigot; possible that newer versions are compatible with the plugmod.

# Config

Place properties at `config/splunk.properties` (works for both plugin and mod); path is relative to directory where minecraft is executed from (generally where minecraft server jar is...).

The splunk host, port, and tokencan be configured with the properties: `splunk.craft.connection.host`, `splunk.craft.connection.port` and `splunk.craft.token`; for example:

```
splunk.craft.connection.host=127.0.0.1
splunk.craft.connection.port=8088
splunk.craft.token=1234-5678-1234-123456789012
splunk.craft.enable.consolelog=true
splunk.craft.server.name=default
```

Note that the values listed here are the defaults.

Further configuration of Splunk via the splunk UI is needed as well as installing the LogToSplunk app (not currently covered in this readme).
