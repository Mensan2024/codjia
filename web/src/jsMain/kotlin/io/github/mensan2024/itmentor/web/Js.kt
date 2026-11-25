// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.web

import kotlinx.browser.window
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine
import kotlin.js.Promise

/** Petits outils d'interopérabilité JavaScript, sans bibliothèque externe. */

suspend fun <T> Promise<T>.await(): T = suspendCoroutine { c ->
    then({ c.resume(it) }, { c.resumeWithException(it) })
}

/** Lance une tâche asynchrone ; une erreur est affichée au lieu de casser la page. */
fun launch(onError: (Throwable) -> Unit = { console.error(it) }, block: suspend () -> Unit) {
    block.startCoroutine(Continuation(EmptyCoroutineContext) { r -> r.exceptionOrNull()?.let(onError) })
}

suspend fun sleep(ms: Int) {
    Promise<Unit> { res, _ -> window.setTimeout({ res(Unit) }, ms) }.await()
}

/** Objet JavaScript littéral. */
fun obj(vararg pairs: Pair<String, Any?>): dynamic {
    val o: dynamic = js("({})")
    for ((k, v) in pairs) o[k] = v
    return o
}

/** Octets d'un ArrayBuffer ou d'une vue typée, sans copie quand c'est possible. */
fun bytesOf(buf: dynamic): ByteArray {
    val ab: dynamic = if (buf.buffer != undefined) buf.buffer else buf
    val off: Int = if (buf.byteOffset != undefined) buf.byteOffset as Int else 0
    val len: Int = buf.byteLength as Int
    val view: dynamic = js("new Int8Array(ab, off, len)")
    return view.unsafeCast<ByteArray>()
}

/** Vue Uint8Array d'un ByteArray (Kotlin/JS : ByteArray est un Int8Array). */
fun uint8(b: ByteArray): dynamic {
    val i8: dynamic = b
    val buffer = i8.buffer
    val off = i8.byteOffset
    val len = i8.length
    return js("new Uint8Array(buffer, off, len)")
}
