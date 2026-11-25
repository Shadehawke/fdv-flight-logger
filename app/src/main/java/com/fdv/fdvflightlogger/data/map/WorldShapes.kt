package com.fdv.fdvflightlogger.data.map

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.IOException

/**
 * The world outline for the route map: land as closed rings, country borders as open lines.
 * Each shape is a FloatArray of lon, lat, lon, lat… in degrees.
 */
class WorldShapes(val land: List<FloatArray>, val borders: List<FloatArray>) {

    companion object {
        private val MAGIC = "FDVW".toByteArray(Charsets.US_ASCII)
        private const val VERSION = 1

        /** Reads world.bin, made by tools/prepare_world.py. */
        @Throws(IOException::class)
        fun parse(bytes: ByteArray): WorldShapes {
            val input = DataInputStream(ByteArrayInputStream(bytes))
            val magic = ByteArray(4).also { input.readFully(it) }
            if (!magic.contentEquals(MAGIC)) throw IOException("Not a world map file")
            val version = input.readUnsignedByte()
            if (version != VERSION) throw IOException("Unsupported world map version $version")
            return WorldShapes(land = readShapes(input), borders = readShapes(input))
        }

        private fun readShapes(input: DataInputStream): List<FloatArray> {
            val count = input.readInt()
            return List(count) {
                val points = input.readInt()
                FloatArray(points * 2) { input.readShort() / 100f }   // hundredths of a degree
            }
        }
    }
}

/** Loads world.bin from the APK once, then keeps it in memory. */
object WorldMap {
    @Volatile
    private var cached: WorldShapes? = null

    suspend fun load(context: Context): WorldShapes {
        cached?.let { return it }
        val shapes = withContext(Dispatchers.IO) {
            context.applicationContext.assets.open("world.bin").use { WorldShapes.parse(it.readBytes()) }
        }
        cached = shapes
        return shapes
    }
}