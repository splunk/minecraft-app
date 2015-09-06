# Overview

Run `mvn clean package -Pinclude-forge` from the root directory to package the LogToSplunk plugmod.

The forge mod / spigot plugin that this project builds can be found in `logtosplunk-plugin/target` and is called `logtosplunk-plugin-{version}.jar`

Currently the supported veresion of minecraft are 1.8.7 for spigot and 1.8-1504 for forge; possible that newer versions are compatible with the plugmod.

# Splunk Logging Library

This library is currently hacked into the build because I don't have access to the splunk maven remote repo that stores this artifact. Currently the correctly-named jar needs to live in the `lib` directory where it will be automatically installed by maven during the build. This way of including the jar needs to be treated as a regular dependency when the aftifact becomes public. Email a Splunk developer to handle any NDAs etc. to get the logging library jar.

# Config

Place properties at `config/splunk_mod.properties` (works for both plugin and mod); path is relative to directory where minecraft is executed from (generally where minecraft server jar is...).

The splunk host and port can be configured with the properties: `mod.splunk.connection.host` and `mod.splunk.connection.port`; for example:

```
mod.splunk.connection.host=127.0.0.1
mod.splunk.connection.port=8888
```

Note that the values listed here are the defaults.

Further configuration of Splunk via the splunk UI is needed as well as installing the LogToSplunk app (not currently covered in this readme).
