package com.supernova.launcher.fork.frequent

import kotlin.math.max
import kotlin.math.pow

// The score of an app: every launch adds 1, and what was there halves every half-life. Worked out when it is
// asked for, from the last launch; no timer touches it.
internal object FrequentScore {

    // The score now: score * 2^(-dt/H), dt the time since the last launch (never negative).
    fun decayed(stat: AppStat, now: Long): Double {
        val dt = max(0L, now - stat.lastLaunchMs)
        return stat.score * 2.0.pow(-dt.toDouble() / FrequentConfig.HALF_LIFE_MS)
    }

    // The stat after a launch at [now]: score = score * 2^(-dt/H) + 1.
    fun launched(stat: AppStat?, now: Long): AppStat {
        val before = stat?.let { decayed(it, now) } ?: 0.0
        return AppStat(before + 1.0, now, (stat?.launchCount ?: 0) + 1)
    }
}
