#!/bin/sh
# Régénère les musiques et les encode en AAC pour l'application (assets) — reproductible.
set -e
cd "$(dirname "$0")/../.."
OUT=build/music
python3 tools/music/compose.py "$OUT"
DEST=engine-android/src/main/assets/music
mkdir -p "$DEST"
for w in "$OUT"/*.wav; do
  id=$(basename "$w" .wav)
  ffmpeg -v error -y -i "$w" -c:a aac -b:a 160k -movflags +faststart "$DEST/$id.m4a"
done
cp "$OUT/music.json" "$DEST/music.json"
ls -la "$DEST"
