package com.fdv.fdvflightlogger.data.whatsnew

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The bundled release notes: assets/whats-new.md, whose first line is "# 1.5.1". */
data class WhatsNew(val version: String, val body: String) {

    companion object {
        const val ASSET_NAME = "whats-new.md"

        // "# 1.5.1" or "# v1.5.1"; the version is everything after the # (and an optional v)
        private val TITLE = Regex("""#\s*v?(\S+)""")

        /** Null when the text doesn't start with a "# <version>" line or has nothing under it. */
        fun parse(text: String): WhatsNew? {
            val lines = text.trim().lines()
            val title = lines.firstOrNull()?.trim() ?: return null
            val version = TITLE.matchEntire(title)?.groupValues?.get(1) ?: return null
            val body = lines.drop(1).joinToString("\n").trim()
            return if (body.isEmpty()) null else WhatsNew(version, body)
        }

        /**
         * Whether to show [notes] on this launch: only after an update (never on a first install),
         * once per version, and only when the notes are for [current], so a file left over from the
         * last release shows nothing.
         *
         * [updatedInPlace] is Android's "installed over an older version"; it covers the first update
         * from 1.5.0 and older, which never stored [lastSeen].
         */
        fun shouldShow(notes: WhatsNew?, current: String, lastSeen: String?, updatedInPlace: Boolean): Boolean {
            if (notes == null || notes.version != current) return false
            if (lastSeen == current) return false
            return lastSeen != null || updatedInPlace
        }

        /** Reads and parses the bundled file; null if it's missing or malformed. */
        suspend fun load(context: Context): WhatsNew? = withContext(Dispatchers.IO) {
            runCatching {
                context.applicationContext.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
            }.getOrNull()?.let(::parse)
        }
    }
}