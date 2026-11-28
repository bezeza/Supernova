package com.supernova.launcher.fork.frequent

import android.content.Context
import android.content.pm.ApplicationInfo

// The one place the Frequent category asks what time it is, so a debug build can move it forward.
internal object FrequentClock {

    fun now(context: Context): Long = System.currentTimeMillis() + FrequentDebugShift.offsetMs(context)
}

// A debug build only: the clock of the Frequent category set ahead, to see the warm-up and the daily recalculation
// without waiting a day. A release build is never debuggable, so there the offset is 0 and nothing can set it.
// Only the Frequent category reads this clock; the system's time is not touched.
//
// Moved by the receiver of the debug build (adb shell am broadcast, see FrequentShiftReceiver).
internal object FrequentDebugShift {

    private const val FILE = "fork_frequent_debug"
    private const val KEY = "offset_ms"

    fun isDebuggable(context: Context): Boolean =
        context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    fun offsetMs(context: Context): Long =
        if (isDebuggable(context)) prefs(context).getLong(KEY, 0L) else 0L

    // Moves the clock on by [deltaMs] (back to the real time when [deltaMs] is null). Returns the offset now.
    fun shift(context: Context, deltaMs: Long?): Long {
        if (!isDebuggable(context)) return 0L
        val next = if (deltaMs == null) 0L else offsetMs(context) + deltaMs
        prefs(context).edit().putLong(KEY, next).apply()
        return next
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
