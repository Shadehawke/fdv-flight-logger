package com.fdv.fdvflightlogger.data.update

/** Compares version strings the way release tags are written: "v1.2.0", "1.2", even "v1.1_Full". */
internal object AppVersion {

    /** The numeric parts ("v1.2.0" → [1, 2, 0]), or null if the string doesn't start with a number. */
    fun parse(raw: String): List<Int>? {
        val core = raw.trim()
            .removePrefix("v").removePrefix("V")
            // Stop at the first character that isn't part of a plain version: "1.1_Full" → "1.1"
            .takeWhile { it.isDigit() || it == '.' }
        val parts = core.split('.')
            .filter { it.isNotEmpty() }
            .map { it.toIntOrNull() ?: return null }
        return parts.takeIf { it.isNotEmpty() }
    }

    /** True only when [candidate] is strictly newer; unreadable versions are never "newer". */
    fun isNewer(candidate: String, current: String): Boolean {
        val a = parse(candidate) ?: return false
        val b = parse(current) ?: return false
        for (i in 0 until maxOf(a.size, b.size)) {
            // A missing part counts as 0, so 1.2 and 1.2.0 are the same version
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}