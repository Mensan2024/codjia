// SPDX-License-Identifier: GPL-3.0-or-later
// Voix off de CODJIA dans le navigateur : Piper (réseau neuronal VITS) exécuté par ONNX Runtime Web.
// Tout tourne sur l'appareil ; les fichiers viennent du site CODJIA lui-même (dossier voix/),
// téléchargés une seule fois puis gardés dans le cache du navigateur.
(function () {
  'use strict';
  var BASE = new URL('.', document.currentScript ? document.currentScript.src : location.href).href;
  var CACHE = 'codjia-voix-v1';
  var VOICES = {
    fr: { file: 'fr_FR-siwis-medium', espeak: 'fr' },
    en: { file: 'en_GB-alba-medium', espeak: 'en-gb-x-rp' }
  };
  var sessions = {}, configs = {}, phonemizerPromise = null, ortPromise = null;

  function loadScript(src) {
    return new Promise(function (resolve, reject) {
      var s = document.createElement('script');
      s.src = src; s.onload = resolve;
      s.onerror = function () { reject(new Error('Chargement impossible : ' + src)); };
      document.head.appendChild(s);
    });
  }

  // Télécharge un fichier en signalant la progression ; garde une copie dans le cache du navigateur.
  async function fetchCached(url, onBytes) {
    var cache = null;
    try { cache = await caches.open(CACHE); } catch (e) { cache = null; }
    if (cache) {
      var hit = await cache.match(url);
      if (hit) { var b = await hit.arrayBuffer(); onBytes && onBytes(b.byteLength, b.byteLength); return b; }
    }
    var res = await fetch(url);
    if (!res.ok) throw new Error('Téléchargement de la voix impossible (' + res.status + ')');
    var total = Number(res.headers.get('content-length')) || 0;
    var reader = res.body && res.body.getReader ? res.body.getReader() : null;
    var buf;
    if (!reader) { buf = await res.arrayBuffer(); }
    else {
      var parts = [], got = 0;
      for (;;) {
        var r = await reader.read();
        if (r.done) break;
        parts.push(r.value); got += r.value.length;
        onBytes && onBytes(got, total);
      }
      var all = new Uint8Array(got), off = 0;
      for (var i = 0; i < parts.length; i++) { all.set(parts[i], off); off += parts[i].length; }
      buf = all.buffer;
    }
    if (cache) { try { await cache.put(url, new Response(buf.slice(0))); } catch (e) { /* quota : on continue sans cache */ } }
    return buf;
  }

  function ort() {
    if (!ortPromise) {
      ortPromise = loadScript(BASE + 'ort/ort.wasm.min.js').then(function () {
        var o = window.ort;
        o.env.wasm.wasmPaths = BASE + 'ort/';
        o.env.wasm.numThreads = 1;   // pas d'isolation cross-origin sur GitHub Pages
        return o;
      });
    }
    return ortPromise;
  }

  function phonemizer() {
    if (!phonemizerPromise) {
      phonemizerPromise = loadScript(BASE + 'piper_phonemize.js').then(function () {
        return { create: window.createPiperPhonemize };
      });
    }
    return phonemizerPromise;
  }

  // Phonèmes (identifiants Piper) d'un texte, via espeak-ng. Le module WebAssembly est gardé entre deux appels.
  var phMod = null, phOut = null;
  async function phonemeModule() {
    if (phMod) return phMod;
    var ph = await phonemizer();
    phMod = await ph.create({
      print: function (line) { try { var j = JSON.parse(line); if (j.phoneme_ids) phOut = j; } catch (e) { /* journal */ } },
      printErr: function () {},
      locateFile: function (f) { return BASE + f; }
    });
    return phMod;
  }
  async function phonemeIds(text, espeakVoice) {
    for (var attempt = 0; attempt < 2; attempt++) {
      try {
        var mod = await phonemeModule();
        phOut = null;
        mod.callMain(['-l', espeakVoice, '--input', JSON.stringify([{ text: text }]), '--espeak_data', '/espeak-ng-data']);
        if (phOut && phOut.phoneme_ids && phOut.phoneme_ids.length) return phOut.phoneme_ids;
      } catch (e) { /* module dans un état inattendu : on le recrée une fois */ }
      phMod = null;
    }
    throw new Error('Phonémisation impossible');
  }

  async function load(lang, onProgress) {
    var v = VOICES[lang] || VOICES.fr;
    if (sessions[v.file]) return v;
    var o = await ort();
    var cfg = JSON.parse(new TextDecoder().decode(await fetchCached(BASE + v.file + '.onnx.json')));
    var model = await fetchCached(BASE + v.file + '.onnx', function (got, total) { onProgress && onProgress(got, total || 63e6); });
    sessions[v.file] = await o.InferenceSession.create(model, { executionProviders: ['wasm'] });
    configs[v.file] = cfg;
    return v;
  }

  /** Synthétise `text` ; renvoie { samples: Float32Array (mono), sampleRate }. */
  async function synthesize(text, lang, rate, onProgress) {
    var v = await load(lang, onProgress);
    var o = await ort();
    var cfg = configs[v.file], inf = cfg.inference || {};
    var ids = await phonemeIds(text, (cfg.espeak && cfg.espeak.voice) || v.espeak);
    var input = new o.Tensor('int64', BigInt64Array.from(ids.map(BigInt)), [1, ids.length]);
    var lengths = new o.Tensor('int64', BigInt64Array.from([BigInt(ids.length)]), [1]);
    var scales = new o.Tensor('float32', Float32Array.from([
      inf.noise_scale != null ? inf.noise_scale : 0.667,
      (inf.length_scale != null ? inf.length_scale : 1) / Math.max(0.5, rate || 1),
      inf.noise_w != null ? inf.noise_w : 0.8
    ]), [3]);
    var res = await sessions[v.file].run({ input: input, input_lengths: lengths, scales: scales });
    var data = res.output.data;
    return { samples: data instanceof Float32Array ? data : Float32Array.from(data), sampleRate: cfg.audio.sample_rate };
  }

  /** true si la voix de `lang` est déjà téléchargée (aucun téléchargement à prévoir). */
  async function isCached(lang) {
    try {
      var v = VOICES[lang] || VOICES.fr;
      var cache = await caches.open(CACHE);
      return !!(await cache.match(BASE + v.file + '.onnx'));
    } catch (e) { return false; }
  }

  window.CodjiaVoice = { synthesize: synthesize, isCached: isCached, load: load, phonemeIds: phonemeIds, sizeMb: 63 };
})();
