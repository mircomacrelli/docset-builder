## What is this?

This is a small program that I use when I want to generate a docset for Dash.

## How to build

Use the command `./gradlew clean shadowJar` and take the jar file in `build/libs/`.

## How to create a new docset

Launch the program passing a configuration file and a destination directory. Example:

```shell script
java -jar docset-builder.jar spring-framework.yml ~/Documents
```

In the sources an example configuration file can be found in the directory `docsets`.
