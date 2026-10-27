#!/bin/sh
# Functional self-test (common/src/selftest/.../SelfTest.java) on a fresh dev server world of LOADER (neoforge or
# fabric, default neoforge), under the hang watchdog (tools/watchdog.sh, max 15 min): the server stops itself after
# the result line; prints the result and every error/warning that is not known vanilla noise.
cd "$(dirname "$0")/.." || exit 1
. tools/watchdog.sh
LOADER=${LOADER:-${1:-neoforge}}
kill_dev() { pkill -f "fml.modFolders=ceramics" 2>/dev/null; pkill -f "$(pwd)/$LOADER/.*(loom-cache|net\.fabricmc)" 2>/dev/null; pkill -f "$(pwd)/.gradle/loom-cache/projects/$LOADER" 2>/dev/null; }
LOG=build-selftest-$LOADER.log
kill_dev
rm -rf "$LOADER/run/world" "$LOG"
mkdir -p "$LOADER/run" && echo "eula=true" > "$LOADER/run/eula.txt"
run_watched "$LOG" 900 ./gradlew ":$LOADER:runSelftest" --console=plain
kill_dev
grep -E 'Ceramics selftest\] (PASS|FAILED|FAIL )|Minecraft Crash Report|Failed to start|BUILD FAILED' "$LOG" | sed -E 's/^\[[^]]*\] //'
echo "--- errors / warnings (vanilla dev noise filtered)"
grep -E '/(ERROR|FATAL|WARN)\]|Caused by|Exception' "$LOG" \
  | grep -vE 'Ceramics selftest|server.properties|uses unexpected schema|OFFLINE/INSECURE|online-mode|hackers to connect|no attempt to authenticate|LanServerPinger|UnixException|Ambiguity between arguments' \
  | sed -E 's/^\[[^]]*\] //' | cut -c1-300 | sort | uniq -c | sort -rn | head -40

# result: 0 if all checks passed
grep -q "Ceramics selftest\] PASS" "$LOG"
