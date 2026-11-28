package com.supernova.launcher.fork.frequent

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

// The statistics of the Frequent category, in a file of their own: device-local, never in the launcher's backup,
// and not touched by a reset of the settings. Every launch rewrites it, as the launch counts' file is, and for the
// same reason it is not the main store. Nothing here runs unless the category is on (the callers check).
private val Context.forkFrequentStore by preferencesDataStore(name = "fork_frequent")

private object FrequentKeys {
    val stats = stringSetPreferencesKey("stats")
    val shown = stringPreferencesKey("shown")
    val pending = stringPreferencesKey("pending")
    val hasPending = booleanPreferencesKey("has_pending")
    val lastRecalc = longPreferencesKey("last_recalc_ms")
    val enabledAt = longPreferencesKey("enabled_at_ms")
    val on = booleanPreferencesKey("on")
    val warm = booleanPreferencesKey("warm")
}

internal object FrequentStore {

    suspend fun read(context: Context): FrequentState = decode(context.forkFrequentStore.data.first())

    // A launch from the launcher. Counted only while the category is [enabled]; an app of the private space is
    // never counted, so nothing of it is kept.
    suspend fun noteLaunch(context: Context, key: String, enabled: Boolean) {
        if (!enabled) return
        val now = FrequentClock.now(context)
        update(context) { FrequentEngine.record(it, key, now) }
    }

    // The category turned on or off.
    suspend fun setEnabled(context: Context, on: Boolean) {
        val now = FrequentClock.now(context)
        update(context) { FrequentEngine.setEnabled(it, on, now) }
    }

    // The drawer was opened: see FrequentEngine.open. [installed] are all the apps the device has; the records of
    // the others are dropped here.
    suspend fun open(
        context: Context,
        places: Int,
        installed: Set<String>,
        eligible: Set<String>,
        excluded: Set<String>
    ): FrequentEngine.Opened {
        val now = FrequentClock.now(context)
        var opened: FrequentEngine.Opened? = null
        update(context) { state ->
            FrequentEngine.open(FrequentEngine.prune(state, installed), now, places, eligible, excluded)
                .also { opened = it }.state
        }
        return checkNotNull(opened)
    }

    private suspend fun update(context: Context, change: (FrequentState) -> FrequentState) {
        context.forkFrequentStore.edit { prefs -> write(prefs, change(decode(prefs))) }
    }

    private fun decode(prefs: Preferences): FrequentState = FrequentState(
        stats = FrequentCodec.decodeStats(prefs[FrequentKeys.stats].orEmpty()),
        shown = FrequentCodec.decodeList(prefs[FrequentKeys.shown]),
        pending = FrequentCodec.decodeList(prefs[FrequentKeys.pending])
            .takeIf { prefs[FrequentKeys.hasPending] == true },
        lastRecalcMs = prefs[FrequentKeys.lastRecalc] ?: 0L,
        enabledAtMs = prefs[FrequentKeys.enabledAt] ?: 0L,
        on = prefs[FrequentKeys.on] ?: false,
        warm = prefs[FrequentKeys.warm] ?: false
    )

    private fun write(prefs: MutablePreferences, state: FrequentState) {
        prefs[FrequentKeys.stats] = FrequentCodec.encodeStats(state.stats)
        prefs[FrequentKeys.shown] = FrequentCodec.encodeList(state.shown)
        prefs[FrequentKeys.hasPending] = state.pending != null
        prefs[FrequentKeys.pending] = FrequentCodec.encodeList(state.pending.orEmpty())
        prefs[FrequentKeys.lastRecalc] = state.lastRecalcMs
        prefs[FrequentKeys.enabledAt] = state.enabledAtMs
        prefs[FrequentKeys.on] = state.on
        prefs[FrequentKeys.warm] = state.warm
    }
}
