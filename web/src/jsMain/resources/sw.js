// CODJIA : fonctionne hors ligne une fois ouvert, et se met à jour tout seul.
// - Coquille (page, code) : réseau d'abord, donc une nouvelle version publiée arrive au prochain chargement.
// - Polices et musiques : cache d'abord (elles ne changent pas d'une version à l'autre).
// - Voix (dossier voix/) : cache dédié, gardé d'une version à l'autre (fichiers lourds, téléchargés une fois).
// Aucune donnée n'est envoyée.
const VERSION = 'codjia-__BUILD__';
const VOICE = 'codjia-voix-v1';
const KEEP = [VERSION, VOICE, 'codjia-ma-musique-v1'];
const SHELL = ['./', 'index.html', 'codjia.js', 'manifest.webmanifest', 'icons/icon-192.png', 'icons/icon-512.png'];

self.addEventListener('install', (e) => {
  e.waitUntil(caches.open(VERSION).then((c) => c.addAll(SHELL)).then(() => self.skipWaiting()));
});

self.addEventListener('activate', (e) => {
  e.waitUntil(caches.keys().then((keys) => Promise.all(keys.filter((k) => !KEEP.includes(k)).map((k) => caches.delete(k))))
    .then(() => self.clients.claim()));
});

self.addEventListener('message', (e) => {
  if (e.data === 'version') e.source && e.source.postMessage({ version: VERSION });
});

self.addEventListener('fetch', (e) => {
  const url = new URL(e.request.url);
  if (e.request.method !== 'GET' || url.origin !== self.location.origin) return;
  // Les modèles de voix sont gérés par codjia-voice.js (avec progression) : on ne les double pas ici.
  if (url.pathname.endsWith('.onnx') || url.pathname.includes('/ma-musique/')) return;
  const voice = url.pathname.includes('/voix/');
  const assets = url.pathname.includes('/assets/');
  e.respondWith(
    caches.open(voice ? VOICE : VERSION).then(async (cache) => {
      if (assets || (voice && !url.pathname.endsWith('codjia-voice.js'))) {
        const hit = await cache.match(e.request);
        if (hit) return hit;
        const res = await fetch(e.request);
        if (res.ok) cache.put(e.request, res.clone());
        return res;
      }
      try {
        const res = await fetch(e.request, { cache: 'no-cache' });
        if (res.ok) cache.put(e.request, res.clone());
        return res;
      } catch (err) {
        return (await cache.match(e.request)) || (await cache.match('index.html'));
      }
    })
  );
});
