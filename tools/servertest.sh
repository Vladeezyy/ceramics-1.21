#!/bin/sh
# Starts the dev server on a fresh world, waits until it is up or has failed (max 10 min), stops it, prints a summary.
# Extra gradle arguments are passed on (e.g. -Pwith_mods).
cd "$(dirname "$0")/.."
pkill -f 'fml.modFolders=ceramics.*server' 2>/dev/null
rm -rf run/world build-server.log
mkdir -p run && echo "eula=true" > run/eula.txt
./gradlew ${RUN:-runServer} --console=plain "$@" > build-server.log 2>&1 &
i=0
until grep -qE 'Done \(|Minecraft Crash Report|Failed to start|Registry loading errors|BUILD FAILED|FAILED TO BIND' build-server.log 2>/dev/null || [ $i -gt 300 ]; do sleep 2; i=$((i+1)); done
sleep 3
pkill -f 'fml.modFolders=ceramics' 2>/dev/null; sleep 2; pkill -9 -f 'fml.modFolders=ceramics' 2>/dev/null
wait 2>/dev/null
[ $i -gt 300 ] && echo "TIMEOUT (no Done / crash in 10 min)"
grep -E 'Done \(|Minecraft Crash Report|Failed to start|Registry loading errors|FAILED TO BIND|BUILD FAILED' build-server.log | head -3
echo "--- errors / warnings from the mod or data"
grep -E '/(ERROR|FATAL|WARN)\]|Caused by|Exception' build-server.log | grep -viE 'Ambiguity between arguments|Unable to resolve|Configuration file .* is not correct' | sed -E 's/^\[[^]]*\] //' | cut -c1-300 | sort | uniq -c | sort -rn | head -40
