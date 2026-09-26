#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
mvn -q -DskipTests package 1>&2
exec java -jar target/tradesim.jar
