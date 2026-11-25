#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Réduit les données espeak-ng du phonémiseur Piper (WebAssembly) au français et à l'anglais.

Le paquet @diffusionstudio/piper-wasm embarque les données de toutes les langues (18 Mo) dans
piper_phonemize.data ; on ne garde que ce dont CODJIA a besoin (~1 Mo) et on réécrit la table des
fichiers dans piper_phonemize.js.

Usage : repack_espeak.py <dossier build du paquet> <dossier de sortie>
"""
import json
import pathlib
import re
import shutil
import sys

KEEP_EXACT = {"phondata", "phondata-manifest", "phonindex", "phontab", "intonations", "fr_dict", "en_dict"}
KEEP_PREFIX = ("lang/roa/fr", "lang/gmw/en")


def keep(name: str) -> bool:
    rel = name.removeprefix("/espeak-ng-data/")
    return rel in KEEP_EXACT or rel.startswith(KEEP_PREFIX)


def main(src: str, dst: str) -> None:
    src_p, dst_p = pathlib.Path(src), pathlib.Path(dst)
    dst_p.mkdir(parents=True, exist_ok=True)
    js = (src_p / "piper_phonemize.js").read_text(encoding="utf-8")
    data = (src_p / "piper_phonemize.data").read_bytes()
    m = re.search(r"loadPackage\((\{\"files\":.*?\"remote_package_size\":\d+\})\)", js, re.S)
    if not m:
        sys.exit("Table des fichiers introuvable dans piper_phonemize.js")
    meta = json.loads(m.group(1))
    files, out, pos = [], bytearray(), 0
    for f in meta["files"]:
        if not keep(f["filename"]):
            continue
        chunk = data[f["start"]:f["end"]]
        out += chunk
        nf = dict(f)
        nf["start"], nf["end"] = pos, pos + len(chunk)
        pos += len(chunk)
        files.append(nf)
    names = {f["filename"].removeprefix("/espeak-ng-data/") for f in files}
    missing = [n for n in KEEP_EXACT if n not in names]
    if missing or not any(n.startswith("lang/roa/fr") for n in names) or not any(n.startswith("lang/gmw/en") for n in names):
        sys.exit(f"Données espeak incomplètes : {missing}")
    new_meta = json.dumps({"files": files, "remote_package_size": len(out)}, separators=(",", ":"))
    js = js[:m.start(1)] + new_meta + js[m.end(1):]
    (dst_p / "piper_phonemize.js").write_text(js, encoding="utf-8")
    (dst_p / "piper_phonemize.data").write_bytes(bytes(out))
    shutil.copy(src_p / "piper_phonemize.wasm", dst_p / "piper_phonemize.wasm")
    print(f"espeak-ng : {len(files)} fichiers, {len(out) / 1e6:.2f} Mo (au lieu de {len(data) / 1e6:.1f} Mo)")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
