#!/usr/bin/env bash

set -e

if [ $# -lt 2 ]; then
	echo "Usage: $0 full.class.name [fs-arguments] mountpoint" >&2
	exit 1
fi

if ! which gradle &> /dev/null; then
  echo 'gradle not found in $PATH. Please install gradle.' >&2
  exit 1
fi

#check if dir exists else create
n=1
mountPoint=${!n}
if [ ! -d "$mountPoint" ]; then
	mkdir -p "$mountPoint"
fi

#echo the number, second argument
n=2
# echo "number" ${!n}
number=${!n}
#file
n=3 
# echo "file" ${!n}
filename=${!n}

absoluteMountpoint="$(cd "$mountPoint" && pwd)"
set -- "${@:1:$(expr "$#" - 1)}" "$absoluteMountpoint"

cd "$(dirname "$BASH_SOURCE")/.."
GRADLE_USER_HOME=gradle gradle uberJar
java -cp build/libs/\* "$mountpoint $number $filename"