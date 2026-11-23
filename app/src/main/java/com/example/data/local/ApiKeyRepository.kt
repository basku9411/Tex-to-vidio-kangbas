package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.ApiKeyBalance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.kangbasDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "kangbas_settings"
)

data class StoredKeyState(
    val keyPool: List<String> = emptyList(),
    val activeIndex: Int = 0,
    val cachedBalances: Map<String, Double?> = emptyMap(),
    val cachedErrors: Map<String, String?> = emptyMap(),
)

class ApiKeyRepository(private val context: Context) {
    private object Keys {
        val KEY_POOL = stringPreferencesKey("kangbas_video_key_pool_v1")
        val ACTIVE_INDEX = intPreferencesKey("kangbas_video_active_key_v1")
        val CACHED_BALANCES = stringPreferencesKey("kangbas_video_credit_balances_v1")
    }

    val stateFlow: Flow<StoredKeyState> = context.kangbasDataStore.data.map { prefs ->
        val pool = parseKeyPool(prefs[Keys.KEY_POOL])
        val rawActive = prefs[Keys.ACTIVE_INDEX] ?: 0
        val safeActive = if (pool.isEmpty()) 0 else rawActive.coerceIn(0, pool.lastIndex)
        val (balances, errors) = parseBalances(prefs[Keys.CACHED_BALANCES])
        StoredKeyState(
            keyPool = pool,
            activeIndex = safeActive,
            cachedBalances = balances,
            cachedErrors = errors,
        )
    }

    suspend fun currentState(): StoredKeyState = stateFlow.first()

    suspend fun addKeys(incomingRaw: String): Pair<Int, List<String>> {
        val incoming = incomingRaw
            .split(Regex("\\r?\\n"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        var added = 0
        var updatedPool: List<String> = emptyList()
        context.kangbasDataStore.edit { prefs ->
            val current = parseKeyPool(prefs[Keys.KEY_POOL]).toMutableList()
            incoming.forEach { key ->
                if (!current.contains(key)) {
                    current.add(key)
                    added++
                }
            }
            updatedPool = current
            prefs[Keys.KEY_POOL] = serializeKeyPool(current)
            if (current.size == 1) {
                prefs[Keys.ACTIVE_INDEX] = 0
            } else {
                val idx = prefs[Keys.ACTIVE_INDEX] ?: 0
                if (idx >= current.size) {
                    prefs[Keys.ACTIVE_INDEX] = 0
                }
            }
        }
        return added to updatedPool
    }

    suspend fun setActiveIndex(index: Int) {
        context.kangbasDataStore.edit { prefs ->
            val pool = parseKeyPool(prefs[Keys.KEY_POOL])
            if (pool.isNotEmpty()) {
                prefs[Keys.ACTIVE_INDEX] = index.coerceIn(0, pool.lastIndex)
            } else {
                prefs[Keys.ACTIVE_INDEX] = 0
            }
        }
    }

    suspend fun setActiveKey(key: String) {
        context.kangbasDataStore.edit { prefs ->
            val pool = parseKeyPool(prefs[Keys.KEY_POOL])
            val idx = pool.indexOf(key)
            if (idx >= 0) {
                prefs[Keys.ACTIVE_INDEX] = idx
            }
        }
    }

    suspend fun removeKeyAt(index: Int) {
        context.kangbasDataStore.edit { prefs ->
            val current = parseKeyPool(prefs[Keys.KEY_POOL]).toMutableList()
            if (index in current.indices) {
                val removed = current.removeAt(index)
                prefs[Keys.KEY_POOL] = serializeKeyPool(current)
                val active = prefs[Keys.ACTIVE_INDEX] ?: 0
                prefs[Keys.ACTIVE_INDEX] = when {
                    current.isEmpty() -> 0
                    active >= current.size -> 0
                    else -> active
                }
                val (balances, errors) = parseBalances(prefs[Keys.CACHED_BALANCES])
                val mutBal = balances.toMutableMap().apply { remove(removed) }
                val mutErr = errors.toMutableMap().apply { remove(removed) }
                prefs[Keys.CACHED_BALANCES] = serializeBalances(mutBal, mutErr)
            }
        }
    }

    suspend fun clearAllKeys() {
        context.kangbasDataStore.edit { prefs ->
            prefs.remove(Keys.KEY_POOL)
            prefs.remove(Keys.ACTIVE_INDEX)
            prefs.remove(Keys.CACHED_BALANCES)
        }
    }

    suspend fun saveBalances(results: List<ApiKeyBalance>) {
        context.kangbasDataStore.edit { prefs ->
            val balances = results.associate { it.key to it.balance }
            val errors = results.associate { it.key to it.error }
            prefs[Keys.CACHED_BALANCES] = serializeBalances(balances, errors)
        }
    }

    private fun parseKeyPool(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                val seen = HashSet<String>()
                for (i in 0 until arr.length()) {
                    val v = arr.optString(i, "").trim()
                    if (v.isNotEmpty() && seen.add(v)) {
                        add(v)
                    }
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun serializeKeyPool(pool: List<String>): String {
        val arr = JSONArray()
        pool.forEach { arr.put(it) }
        return arr.toString()
    }

    private fun parseBalances(raw: String?): Pair<Map<String, Double?>, Map<String, String?>> {
        if (raw.isNullOrBlank()) return emptyMap<String, Double?>() to emptyMap()
        return try {
            val obj = JSONObject(raw)
            val balances = mutableMapOf<String, Double?>()
            val errors = mutableMapOf<String, String?>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val entry = obj.optJSONObject(k)
                if (entry != null) {
                    balances[k] = if (entry.has("balance") && !entry.isNull("balance")) {
                        entry.optDouble("balance").takeIf { it.isFinite() }
                    } else null
                    errors[k] = if (entry.has("error") && !entry.isNull("error")) {
                        entry.optString("error")
                    } else null
                }
            }
            balances to errors
        } catch (_: Exception) {
            emptyMap<String, Double?>() to emptyMap()
        }
    }

    private fun serializeBalances(
        balances: Map<String, Double?>,
        errors: Map<String, String?>,
    ): String {
        val root = JSONObject()
        val allKeys = (balances.keys + errors.keys).distinct()
        allKeys.forEach { k ->
            val entry = JSONObject()
            val bal = balances[k]
            if (bal != null && bal.isFinite()) {
                entry.put("balance", bal)
            } else {
                entry.put("balance", JSONObject.NULL)
            }
            val err = errors[k]
            if (err != null) {
                entry.put("error", err)
            } else {
                entry.put("error", JSONObject.NULL)
            }
            root.put(k, entry)
        }
        return root.toString()
    }
}
