#LogToSplunkPlugin Environment Setup
Information on how to get things up and running for development.  There may be some dependencies that
need to be downloaded to get this to work, if you run into any, email alszeb@gmail.com with the
info more make a merge request with the updated README-DEV.

## Intellij
These steps have been tested in Intellij.  It looks like most of the guides on forge talk about
running it in Eclipse, although there are guides for Intellij as well. 

To get started on Intellij, clone this project, checkout a forge-enabled branch and import
as a gradle project.  In the forge section below, a bunch of gradle tasks are run to get the environment set up.
Somewhere in there we *might* be supposed to run 'genIntellijRuns' but I'm not sure I needed to.

I add the below snippet to the bottom of my build.gradle to get it to download the test dependencies for me.

```
idea{
    module{
        downloadSources = true
        downloadJavadoc = true
    }
}
```

## Gradle
If you don't have Gradle installed, you can instead use the Gradle wrapper by running
the gradlew in place of the gradle command (the arguments are the same).

## Testing
Mockito, JUnit and JMockit need to be on the classpath (dependencies are in build.gradle). 

## Forge
The forge dev environment is constructed with gradle.  

I ran these steps on a cleanly cloned repository (I had already installed the latest
versions of Minecraft forge, so its possible these steps wont work out-of-the-box. 
Email alszeb@gmail.com with missing steps). Minecraft itself may also need to be installed
before forge.

Run the following steps (some steps may take a little while):
``` 
gradle setupDecompWorkspace
```
At this point, I think you should have access to any code necessary to write the plugin.

To get a minecraft instance up and running so you can see your changes in action:

```
gradle runServer
```

- Close the attemped server instance and set value to true in *eclipse/eula.text*

```
gradle runServer
```

- Allow the server to fully start, then set online-mode=false in *eclipse/server.properties*
- Close the server instance.

```
gradle runServer
gradle runClient
```

Enter the server via the client instance and test!


## Unit Tests

Unit testing the deobfuscated code requires a lot of mocking to work. Some of the Forge classes can't loaded at the same time as our test dependencies so they also must be mocked out.
