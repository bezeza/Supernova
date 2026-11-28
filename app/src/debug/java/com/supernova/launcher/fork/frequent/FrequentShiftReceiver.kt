package com.supernova.launcher.fork.frequent

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

// Debug builds only (this file is in src/debug): moves the clock of the Frequent category, to check the warm-up
// and the daily recalculation on a phone without waiting. Only the shell may send it (the manifest asks for DUMP).
//
//   adb shell am broadcast -n com.supernova.launcher.debug/com.supernova.launcher.fork.frequent.FrequentShiftReceiver
//       moves it on by 24 hours; add --ei hours N for another step, or --ez reset true to go back to real time.
//
// After a shift, open the drawer: the new top is worked out then and shown at the opening after that.
class FrequentShiftReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reset = intent.getBooleanExtra(EXTRA_RESET, false)
        val hours = intent.getIntExtra(EXTRA_HOURS, DEFAULT_HOURS)
        val offset = FrequentDebugShift.shift(context, if (reset) null else hours * HOUR_MS)
        Log.i(TAG, "Frequent clock is ${offset / HOUR_MS} h ahead")
    }

    private companion object {
        const val TAG = "FrequentShift"
        const val EXTRA_HOURS = "hours"
        const val EXTRA_RESET = "reset"
        const val DEFAULT_HOURS = 24
        const val HOUR_MS = 60L * 60 * 1000
    }
}
