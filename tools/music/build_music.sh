#!/bin/sh
# Régénère le catalogue musical (compositions originales) et l'encode en AAC pour l'application — reproductible.
set -e
cd "$(dirname "$0")/../.."
OUT=build/music
rm -rf "$OUT"
python3 tools/music/compose_energy.py "$OUT"
DEST=engine-android/src/main/assets/music
rm -f "$DEST"/*.m4a
mkdir -p "$DEST"
for w in "$OUT"/*.wav; do
  id=$(basename "$w" .wav)
  ffmpeg -v error -y -i "$w" -c:a aac -b:a 192k -movflags +faststart "$DEST/$id.m4a"
done
cp "$OUT/music.json" "$DEST/music.json"
ls -la "$DEST"
