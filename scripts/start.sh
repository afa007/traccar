#!/usr/bin/env bash

set -x

export PATH=$JAVA_HOME/bin:$PATH

LOG_PATH=/var/log/traccar

# BatchLauncher
BATCH_HOME=$(cd $(dirname $0);cd ..;pwd)

# BatchLauncher/lib
LIB_ROOT=$BATCH_HOME/lib

# BatchLauncher/conf
CONFIG_ROOT=$BATCH_HOME/conf

CLASSPATH=$CLASSPATH:$CONFIG_ROOT:$LIB_ROOT/*

JAVA_OPTS="-Xms4096m -Xmx4096m -XX:NewSize=512m -server -XX:+DisableExplicitGC -Dlog.path=$LOG_PATH -verbose:gc -XX:+PrintGCDateStamps -XX:+PrintGCDetails -Xloggc:$LOG_PATH/gc.log"

java -classpath $CLASSPATH $JAVA_OPTS org.traccar.Main

RETVAL=$?

exit $RETVAL