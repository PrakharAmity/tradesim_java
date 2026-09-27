#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"

# 1. Build initial jar if not present
if [ ! -f target/tradesim.jar ]; then
    mvn -q -DskipTests package 1>&2
fi

# 2. Launch server in background
PORT=3000 java -XX:+UseSerialGC -Xms16m -Xmx64m -XX:TieredStopAtLevel=1 -XX:CICompilerCount=1 -Xss256k -jar target/tradesim.jar &
SERVER_PID=$!

cleanup() {
    kill -TERM "$SERVER_PID" 2>/dev/null || true
    exit 0
}
trap cleanup SIGTERM SIGINT EXIT

# 3. Watch for changes in src/ or target/tradesim.jar to cleanly restart server
get_snapshot() {
    find src/main target -name "*.java" -o -name "tradesim.jar" 2>/dev/null | xargs stat -c "%Y %n" 2>/dev/null | sort
}
LAST_SNAPSHOT=$(get_snapshot)

while true; do
    sleep 2
    CURRENT_SNAPSHOT=$(get_snapshot)
    if [ "$CURRENT_SNAPSHOT" != "$LAST_SNAPSHOT" ]; then
        # If java files changed directly, rebuild
        if find src/main -name "*.java" -newer target/tradesim.jar 2>/dev/null | grep -q .; then
            echo "[TradeSim] Recompiling updated classes..."
            mvn -q -DskipTests package 1>&2 || true
        fi
        
        echo "[TradeSim] Reloading server with updated JAR..."
        kill -TERM "$SERVER_PID" 2>/dev/null || true
        wait "$SERVER_PID" 2>/dev/null || true
        PORT=3000 java -XX:+UseSerialGC -Xms16m -Xmx64m -XX:TieredStopAtLevel=1 -XX:CICompilerCount=1 -Xss256k -jar target/tradesim.jar &
        SERVER_PID=$!
        echo "[TradeSim] Server reloaded (PID: $SERVER_PID)."
        LAST_SNAPSHOT=$(get_snapshot)
    fi
done