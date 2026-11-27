// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

/**
 * JSON minimal, sans dépendance (le même code tourne sur la JVM, Android et le navigateur).
 * Valeurs : `null`, Boolean, Long, Double, String, List<Any?>, Map<String, Any?> (ordre d'insertion conservé).
 */
internal object MiniJson {
    fun write(v: Any?): String = StringBuilder().also { put(it, v, 0) }.toString()

    private fun put(sb: StringBuilder, v: Any?, depth: Int) {
        when (v) {
            null -> sb.append("null")
            is Boolean -> sb.append(v)
            is Int, is Long -> sb.append(v.toString())
            is Float, is Double -> sb.append(v.toString())
            is String -> quote(sb, v)
            is List<*> -> {
                if (v.all { it == null || it is String || it is Number || it is Boolean } && v.sumOf { (it as? String)?.length ?: 4 } < 80) {
                    sb.append('['); v.forEachIndexed { i, x -> if (i > 0) sb.append(", "); put(sb, x, depth) }; sb.append(']')
                } else {
                    sb.append("[\n")
                    v.forEachIndexed { i, x -> sb.append("  ".repeat(depth + 1)); put(sb, x, depth + 1); if (i < v.lastIndex) sb.append(','); sb.append('\n') }
                    sb.append("  ".repeat(depth)).append(']')
                }
            }
            is Map<*, *> -> {
                sb.append("{\n")
                val es = v.entries.toList()
                es.forEachIndexed { i, (k, x) ->
                    sb.append("  ".repeat(depth + 1)); quote(sb, k.toString()); sb.append(": "); put(sb, x, depth + 1)
                    if (i < es.lastIndex) sb.append(','); sb.append('\n')
                }
                sb.append("  ".repeat(depth)).append('}')
            }
            else -> quote(sb, v.toString())
        }
    }

    private fun quote(sb: StringBuilder, s: String) {
        sb.append('"')
        for (c in s) when (c) {
            '"' -> sb.append("\\\""); '\\' -> sb.append("\\\\"); '\n' -> sb.append("\\n"); '\r' -> sb.append("\\r"); '\t' -> sb.append("\\t")
            else -> if (c < ' ') sb.append("\\u").append(c.code.toString(16).padStart(4, '0')) else sb.append(c)
        }
        sb.append('"')
    }

    fun parse(s: String): Any? {
        val p = Parser(s)
        p.ws()
        val v = p.value()
        p.ws()
        if (p.i != s.length) p.fail("texte après la valeur")
        return v
    }

    private class Parser(val s: String) {
        var i = 0
        fun fail(msg: String): Nothing = throw StoryboardException("JSON invalide à la position $i : $msg")
        fun ws() { while (i < s.length && s[i].isWhitespace()) i++ }
        fun value(): Any? {
            ws()
            if (i >= s.length) fail("fin inattendue")
            return when (s[i]) {
                '{' -> obj(); '[' -> arr(); '"' -> str()
                't' -> lit("true", true); 'f' -> lit("false", false); 'n' -> lit("null", null)
                else -> num()
            }
        }
        fun lit(w: String, v: Any?): Any? { if (!s.startsWith(w, i)) fail("valeur inconnue"); i += w.length; return v }
        fun num(): Any {
            val st = i
            while (i < s.length && (s[i].isDigit() || s[i] in "+-.eE")) i++
            val t = s.substring(st, i)
            return t.toLongOrNull() ?: t.toDoubleOrNull() ?: fail("nombre invalide")
        }
        fun str(): String {
            i++
            val sb = StringBuilder()
            while (i < s.length && s[i] != '"') {
                val c = s[i++]
                if (c != '\\') { sb.append(c); continue }
                if (i >= s.length) fail("échappement incomplet")
                when (val e = s[i++]) {
                    'n' -> sb.append('\n'); 't' -> sb.append('\t'); 'r' -> sb.append('\r'); 'b' -> sb.append('\b'); 'f' -> sb.append('\u000C')
                    'u' -> { if (i + 4 > s.length) fail("\\u incomplet"); sb.append(s.substring(i, i + 4).toInt(16).toChar()); i += 4 }
                    '"', '\\', '/' -> sb.append(e)
                    else -> fail("échappement inconnu")
                }
            }
            if (i >= s.length) fail("chaîne non fermée")
            i++
            return sb.toString()
        }
        fun arr(): List<Any?> {
            i++
            val out = ArrayList<Any?>()
            ws()
            if (i < s.length && s[i] == ']') { i++; return out }
            while (true) {
                out += value(); ws()
                if (i >= s.length) fail("tableau non fermé")
                if (s[i] == ',') { i++; continue }
                if (s[i] == ']') { i++; return out }
                fail("virgule ou ] attendu")
            }
        }
        fun obj(): Map<String, Any?> {
            i++
            val out = LinkedHashMap<String, Any?>()
            ws()
            if (i < s.length && s[i] == '}') { i++; return out }
            while (true) {
                ws()
                if (i >= s.length || s[i] != '"') fail("clé attendue")
                val k = str(); ws()
                if (i >= s.length || s[i] != ':') fail(": attendu")
                i++
                out[k] = value(); ws()
                if (i >= s.length) fail("objet non fermé")
                if (s[i] == ',') { i++; continue }
                if (s[i] == '}') { i++; return out }
                fail("virgule ou } attendu")
            }
        }
    }
}

/** Empreinte stable (FNV-1a 64 bits) : même contenu ⇒ même empreinte, sur toutes les plateformes. */
object Hash64 {
    fun of(s: String): String {
        var h = -3750763034362895579L // 0xCBF29CE484222325
        for (c in s) { h = h xor c.code.toLong(); h *= 1099511628211L }
        return h.toULong().toString(16).padStart(16, '0')
    }
}
