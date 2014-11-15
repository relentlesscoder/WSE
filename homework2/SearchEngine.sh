#!/bin/bash

NEWLINE=$'\n'
DIRECTORY='bin'

if [ ! -d ${DIRECTORY} ]; then
  # Control will enter here if $DIRECTORY exists.
  mkdir ./${DIRECTORY} 
  cp -r ./lib ./${DIRECTORY}
  export CLASSPATH=./bin:./bin/lib/*
  javac -d ./bin  ./src/edu/nyu/cs/cs2580/*/*.java ./src/edu/nyu/cs/cs2580/*.java -nowarn
  echo "Compiled"
fi

export CLASSPATH=$CLASSPATH:./${DIRECTORY}:./${DIRECTORY}/lib/*
java -Xmx512m edu.nyu.cs.cs2580.SearchEngine $*