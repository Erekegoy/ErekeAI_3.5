#!/bin/sh

##############################################################################
# Gradle start up script for POSIX
##############################################################################

DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

APP_HOME=$( cd "$( dirname "$0" )" && pwd )

warn () {
    echo "$*"
}

die () {
    echo
    echo "$*"
    echo
    exit 1
}

if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

exec "$JAVACMD" "-Dorg.gradle.appname=gradlew" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
