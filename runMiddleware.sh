#!/bin/bash
echo Starting Middleware

cd ~
rm -rf local/jdk1.8.0_66
rm -rf local/ASL_Project
cp -r jdk1.8.0_66 local
cp -r ASL_Project local

export JAVA_HOME=~/local/jdk1.8.0_66
export PATH=$PATH:$JAVA_HOME/bin

cd local/ASL_Project
ant -f asl_project.xml -Djdk.home.1.8=$JAVA_HOME run.middleware

echo runMiddleware.sh has finished. Press any key to close exit and close screen session.
read
