#!/bin/bash

PORT=$1
CORPUS=$2
VALID=0

if [ $1 -eq 25806 ]; then
	VALID=$((VALID+1))
fi
if [ -e $2 ]; then
	VALID=$((VALID+1))
fi
if [ ${VALID} -ne 2 ]; then
	echo "Arguments for this program are: 25806 [PATH-TO-CORPUS]"
	exit 1
fi
 
rm -rf ./bin
mkdir ./bin
cp -r ./lib ./bin
cp ./src/log4j2.xml ./bin

export CLASSPATH=./bin:./bin/lib/log4j-api.jar:./bin/lib/log4j-core.jar

javac -d ./bin  ./src/edu/nyu/cs/cs2580/*.java
java edu.nyu.cs.cs2580.SearchEngine ${PORT} ${CORPUS}

rm -rf ./bin

NEWLINE=$'\n'
echo "${NEWLINE}done:)"

