// CODJIA : fonctionne hors ligne une fois ouvert. Coquille mise en cache à l'installation,
// polices et musiques mises en cache à la première utilisation. Aucune donnée envoyée.
const VERSION = 'codjia-__BUILD__';
const SHELL = ['./', 'index.html', 'codjia.js', 'manifest.webmanifest', 'icons/icon-192.png', 'icons/icon-512.png'];

self.addEventListener('install', (e) => {
  e.waitUntil(caches.open(VERSION).then((c) => c.addAll(SHELL)).then(() => self.skipWaiting()));
});

self.addEventListener('activate', (e) => {
  e.waitUntil(caches.keys().then((keys) => Promise.all(keys.filter((k) => k !== VERSION).map((k) => caches.delete(k))))
    .then(() => self.clients.claim()));
});

self.addEventListener('fetch', (e) => {
  const url = new URL(e.request.url);
  if (e.request.method !== 'GET' || url.origin !== self.location.origin) return;
  // Code et page : réseau d'abord (mises à jour), cache si hors ligne. Polices et musiques : cache d'abord.
  const assets = url.pathname.includes('/assets/');
  e.respondWith(
    caches.open(VERSION).then(async (cache) => {
      if (assets) {
        const hit = await cache.match(e.request);
        if (hit) return hit;
        const res = await fetch(e.request);
        if (res.ok) cache.put(e.request, res.clone());
        return res;
      }
      try {
        const res = await fetch(e.request);
        if (res.ok) cache.put(e.request, res.clone());
        return res;
      } catch (err) {
        return (await cache.match(e.request)) || (await cache.match('index.html'));
      }
    })
  );
});
