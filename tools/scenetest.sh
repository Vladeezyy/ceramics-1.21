#!/bin/sh
# Runs the client scene (common/src/selftest/.../ClientScene.java) on LOADER (neoforge or fabric, default neoforge)
# under the hang watchdog (tools/watchdog.sh, max 15 min): screenshots in <loader>/run/screenshots/scene_*.png, then
# prints the checks and the errors of the run. LANG_CODE=ru_ru (or another language) sets the game language.
cd "$(dirname "$0")/.." || exit 1
. tools/watchdog.sh
LOADER=${LOADER:-${1:-neoforge}}
kill_dev() { pkill -f "fml.modFolders=ceramics" 2>/dev/null; pkill -f "$(pwd)/$LOADER/.*(loom-cache|net\.fabricmc)" 2>/dev/null; }
RUN=$LOADER/run
LOG=build-scene-$LOADER.log
rm -rf "$RUN/saves/ceramics_scene" "$RUN"/screenshots/scene_*.png "$LOG"
mkdir -p "$RUN"
if [ ! -f "$RUN/options.txt" ]; then
  printf 'onboardAccessibility:false\ntutorialStep:none\npauseOnLostFocus:false\n' > "$RUN/options.txt"
fi
if [ -n "$LANG_CODE" ]; then
  grep -v '^lang:' "$RUN/options.txt" > "$RUN/options.tmp"; echo "lang:$LANG_CODE" >> "$RUN/options.tmp"; mv "$RUN/options.tmp" "$RUN/options.txt"
fi
run_watched "$LOG" 900 ./gradlew ":$LOADER:runScene" --console=plain
kill_dev
grep -E '\[scene\] (CHECK|DONE)' "$LOG" | sed -E 's/^.*\[scene\] //'
ls "$RUN/screenshots/" 2>/dev/null | grep scene_
echo "--- errors"
grep -E '/(ERROR|FATAL)\]|Caused by|Exception' "$LOG" | grep -vE 'NoSuchFileException: server.properties|authlib|sessionserver|minecraftservices|certificates|profile key' | sed -E 's/^\[[^]]*\] //' | cut -c1-300 | sort | uniq -c | sort -rn | head -30
