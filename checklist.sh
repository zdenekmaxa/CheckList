#!/bin/sh

# CheckList application start script

# TDAQ release should be set up in order to have access to
# the default TDAQ Java runtime environment under $TDAQ_JAVA_HOME


# should be pointing to the correct production DAQ/HLT release
TDAQ_RELEASE_SETUP="/det/tdaq/scripts/setup_TDAQ.sh"


if [ "x$TDAQ_JAVA_HOME" == "x" ]
then
    echo "TDAQ release does not seem to be set up."
    echo "CheckList needs access to Java via TDAQ_JAVA_HOME env variable."
    echo "Setting up TDAQ release: $TDAQ_RELEASE_SETUP"
    if [ -r $TDAQ_RELEASE_SETUP ]
    then
        source $TDAQ_RELEASE_SETUP
    else
        echo "$TDAQ_RELEASE_SETUP does not exist."
        echo "Try setting up TDAQ release yourself, exit."
        exit 1
    fi
fi

echo "Java is available under: $TDAQ_JAVA_HOME"
CHECKLIST_HOME=`dirname $0`
echo "Running CheckList application from: $CHECKLIST_HOME"
exec $TDAQ_JAVA_HOME/bin/java -jar $CHECKLIST_HOME/checklist.jar $*

