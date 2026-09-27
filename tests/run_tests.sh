#!/usr/bin/env bash
set -e
cd "$(dirname "$0")/.."

# Recompile modified .java files and update tradesim.jar
mvn -q -DskipTests package 1>&2

# Execute the TestRunner with the freshly compiled jar
exec java -cp target/tradesim.jar com.tradesim.TestRunner
