#!/bin/bash

URL=$1
JUDGEMENT=$2

VALID=0

if [ $# -lt 2 ]; then
  echo 1>&2 "$0: not enough arguments"
  exit 2
elif [ $# -gt 2 ]; then
  echo 1>&2 "$0: too many arguments"
fi

if [ -f ${JUDGEMENT} ]; then
	VALID=$((VALID+1))
fi

if [ ${VALID} -ne 1 ]; then
	echo "Arguments for this program are: [URL] [PATH-TO-JUDGMENT]"
	exit 1
fi

export CLASSPATH=./bin:./bin/lib/log4j-api.jar:./bin/lib/log4j-core.jar

curl ${URL} | java edu.nyu.cs.cs2580.Evaluator ${JUDGEMENT}
