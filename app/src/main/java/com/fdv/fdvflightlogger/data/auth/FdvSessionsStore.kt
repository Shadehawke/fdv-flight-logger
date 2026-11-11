package com.fdv.fdvflightlogger.data.auth

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fdv.fdvflightlogger.data.network.AppHttp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

// Its own file, separate from fdv_prefs, so backup rules can exclude exactly this
private val Context.authStore by preferencesDataStore(name = "fdv_auth")

class FdvSessionStore(private val context: Context) {

    private val sessionKey = stringPreferencesKey("session")

    /** The stored session, or null if signed out or the stored copy can't be decrypted. */
    val session: Flow<FdvSession?> = context.authStore.data.map { prefs ->
        prefs[sessionKey]?.let(::decode)
    }

    @Throws(FdvAuthException::class)
    suspend fun save(session: FdvSession) {
        val sealed = withContext(Dispatchers.IO) {
            try {
                TokenCipher.encrypt(AppHttp.json.encodeToString(FdvSession.serializer(), session))
            } catch (e: Exception) {
                // Keystore failures surface as several unrelated exception types; all mean the same thing here
                throw FdvAuthException("Couldn't store your sign-in securely on this device.")
            }
        }
        context.authStore.edit { it[sessionKey] = sealed }
    }

    suspend fun clear() {
        context.authStore.edit { it.remove(sessionKey) }
    }

    private fun decode(sealed: String): FdvSession? = try {
        AppHttp.json.decodeFromString(FdvSession.serializer(), TokenCipher.decrypt(sealed))
    } catch (e: Exception) {
        // The key is gone (Keystore reset, data copied from another device) or the blob is damaged.
        // Either way the pilot just signs in again.
        Log.w("FdvAuth", "Stored session unreadable: ${e.javaClass.simpleName}")
        null
    }
}