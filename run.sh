#!/bin/sh
mvn clean compile package -Dmaven.test.skip=true
nohup java -cp tracker-server-jar-with-dependencies.jar org.traccar.Main ./setup/traccar.xml &