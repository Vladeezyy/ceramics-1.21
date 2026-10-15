#!/bin/sh
# Runs the client scene (src/selftest/.../ClientScene.java, max 15 min): screenshots in run/screenshots/scene_*.png,
# then prints the checks and the errors of the run. LANG_CODE=ru_ru (or another language) sets the game language.
cd "$(dirname "$0")/.."
rm -rf run/saves/ceramics_scene run/screenshots/scene_*.png build-scene.log
mkdir -p run
if [ -n "$LANG_CODE" ]; then
  touch run/options.txt
  grep -v '^lang:' run/options.txt > run/options.tmp; echo "lang:$LANG_CODE" >> run/options.tmp; mv run/options.tmp run/options.txt
fi
perl -e 'alarm 900; exec @ARGV' ./gradlew runScene --console=plain "$@" > build-scene.log 2>&1
echo "exit $?"
grep -E '\[scene\] (CHECK|DONE)' build-scene.log | sed -E 's/^.*\[scene\] //'
ls run/screenshots/ 2>/dev/null | grep scene_
echo "--- errors"
grep -E '/(ERROR|FATAL)\]|Caused by|Exception' build-scene.log | grep -v 'NoSuchFileException: server.properties' | sed -E 's/^\[[^]]*\] //' | cut -c1-300 | sort | uniq -c | sort -rn | head -30
