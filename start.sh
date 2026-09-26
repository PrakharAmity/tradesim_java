#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
exec java -jar target/tradesim.jar

