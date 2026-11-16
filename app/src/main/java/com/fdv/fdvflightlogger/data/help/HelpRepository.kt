package com.fdv.fdvflightlogger.data.help

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Loads the bundled help from assets/help once, then keeps it in memory. */
object HelpRepository {

    private const val DIR = "help"

    @Volatile
    private var cached: List<HelpSection>? = null

    suspend fun sections(context: Context): List<HelpSection> {
        cached?.let { return it }
        val loaded = withContext(Dispatchers.IO) {
            val assets = context.applicationContext.assets
            assets.list(DIR).orEmpty()
                .filter { HelpParser.FILE_NAME.matches(it) }
                .sorted()                                    // 01-, 02-, … is the reading order
                .mapNotNull { name ->
                    assets.open("$DIR/$name").bufferedReader().use { HelpParser.parseSection(name, it.readText()) }
                }
        }
        cached = loaded
        return loaded
    }
}