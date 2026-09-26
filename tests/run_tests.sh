#!/usr/bin/env bash
set -e
cd "$(dirname "$0")/.."
# If the jar isn't built yet, build it once
if [ ! -f target/tradesim.jar ]; then
    mvn -q -DskipTests package 1>&2
fi
# Execute the built-in JSON TestRunner directly with Java
exec java -cp target/tradesim.jar com.tradesim.TestRunner
