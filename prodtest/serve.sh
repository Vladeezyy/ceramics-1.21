#!/bin/sh
# Runs the NeoForge server in the current dir until "Done", a crash or the end of the process (max 10 min), then stops
# it. Output: server.out
rm -f server.out console.in
mkfifo console.in
sh run.sh nogui < console.in > server.out 2>&1 &
pid=$!
exec 3> console.in
i=0
until grep -qE 'Done \(|Crash report|Failed to start|Exception in server tick|Error: Could not' server.out 2>/dev/null || ! kill -0 $pid 2>/dev/null || [ $i -gt 300 ]; do sleep 2; i=$((i+1)); done
sleep 3
echo stop >&3
i=0; while kill -0 $pid 2>/dev/null && [ $i -lt 30 ]; do sleep 1; i=$((i+1)); done
kill $pid 2>/dev/null
exec 3>&-
rm -f console.in
grep -E 'Done \(|Crash report|Failed to start|Error: Could not' server.out | head -3
grep -E '/(ERROR|FATAL)\]' server.out | sed -E 's/^\[[^]]*\] //' | cut -c1-250 | sort | uniq -c | sort -rn | head -20
