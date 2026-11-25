#!/usr/bin/env bash
# SPDX-License-Identifier: GPL-3.0-or-later
# Prépare la voix off de l'application web dans <dossier du site>/voix :
#   - moteur ONNX Runtime Web (WebAssembly, MIT) ;
#   - phonémiseur Piper / espeak-ng (WebAssembly), réduit au français et à l'anglais ;
#   - voix Piper « siwis » (français, CC BY 4.0) et « alba » (anglais, CC BY 4.0).
# Tout est servi par le site lui-même : le navigateur ne contacte aucun autre serveur.
# Les versions et empreintes sont figées ; toute différence arrête la construction.
set -euo pipefail
OUT="${1:?usage: prepare_web_voice.sh <dossier du site>}/voix"
HERE="$(cd "$(dirname "$0")" && pwd)"
ORT_VERSION=1.31.1
PIPER_WASM_VERSION=1.0.0
MODELS="https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models"
declare -A SHA=(
  [fr_FR-siwis-medium.onnx]=b015076c2bedcff22fbedd5da6ffb49b7d0abb2e3bdc51e1a7906e0000e411ed
  [fr_FR-siwis-medium.onnx.json]=39479916c2db192b5ac9764daddd0c744d83e023ad890c6976c0633ae4df8959
  [en_GB-alba-medium.onnx]=c904d007a8047ab13628b021351b983d0a2627c0d7a81c64a6fe9ad661adb1cf
  [en_GB-alba-medium.onnx.json]=aa965a2f02ecced632c2694e1fc72bbff6d65f265fab567ca945918c73dd89f4
)
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
mkdir -p "$OUT/ort"

# 1. Paquets npm (npm vérifie leur intégrité).
(cd "$TMP" && npm pack --silent "onnxruntime-web@$ORT_VERSION" "@diffusionstudio/piper-wasm@$PIPER_WASM_VERSION" >/dev/null)
mkdir -p "$TMP/ort" "$TMP/piper"
tar xzf "$TMP"/onnxruntime-web-*.tgz -C "$TMP/ort"
tar xzf "$TMP"/diffusionstudio-piper-wasm-*.tgz -C "$TMP/piper"
for f in ort.wasm.min.js ort-wasm-simd-threaded.mjs ort-wasm-simd-threaded.wasm; do cp "$TMP/ort/package/dist/$f" "$OUT/ort/"; done
cp "$TMP/ort/package/README.md" "$OUT/ort/README.md"   # licence MIT (Microsoft) indiquée dans le paquet
python3 -I "$HERE/repack_espeak.py" "$TMP/piper/package/build" "$OUT"

# 2. Voix Piper (fichiers ONNX), avec contrôle d'empreinte.
for voice in fr_FR-siwis-medium en_GB-alba-medium; do
  curl -fsSL --retry 3 -o "$TMP/$voice.tar.bz2" "$MODELS/vits-piper-$voice.tar.bz2"
  tar xjf "$TMP/$voice.tar.bz2" -C "$TMP" "vits-piper-$voice/$voice.onnx" "vits-piper-$voice/$voice.onnx.json" "vits-piper-$voice/MODEL_CARD"
  for f in "$voice.onnx" "$voice.onnx.json"; do
    echo "${SHA[$f]}  $TMP/vits-piper-$voice/$f" | sha256sum -c --quiet -
    cp "$TMP/vits-piper-$voice/$f" "$OUT/"
  done
  cp "$TMP/vits-piper-$voice/MODEL_CARD" "$OUT/$voice.MODEL_CARD.md"
done
cat > "$OUT/LISEZMOI.txt" <<'TXT'
Voix off de CODJIA (navigateur)
- Voix française « siwis » : Piper (rhasspy), données SIWIS, licence CC BY 4.0.
- Voix anglaise « alba » : Piper (rhasspy), données Alba (Université d'Édimbourg), licence CC BY 4.0.
- Phonémiseur : piper-phonemize + espeak-ng (GPL-3.0) compilés en WebAssembly (build MIT de diffusion-studio).
- Moteur : ONNX Runtime Web (MIT, Microsoft).
TXT
du -sh "$OUT"
