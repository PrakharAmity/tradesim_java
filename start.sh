#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
exec java -XX:+UseSerialGC -Xms16m -Xmx64m -XX:TieredStopAtLevel=1 -XX:CICompilerCount=1 -Xss256k -jar target/tradesim.jar


