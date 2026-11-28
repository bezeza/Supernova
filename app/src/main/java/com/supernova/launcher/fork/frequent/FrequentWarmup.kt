package com.supernova.launcher.fork.frequent

// Whether there is enough to go on for the tile to come alive. [stats] are those of apps that can be shown.
internal object FrequentWarmup {

    fun isWarm(stats: Map<String, AppStat>, enabledAtMs: Long, now: Long): Boolean {
        val launches = stats.values.sumOf { it.launchCount }
        val apps = stats.values.count { it.launchCount > 0 }
        if (launches >= FrequentConfig.WARM_LAUNCHES && apps >= FrequentConfig.WARM_APPS) return true
        val waited = enabledAtMs > 0L && now - enabledAtMs >= FrequentConfig.WARM_AFTER_MS
        return waited && launches >= FrequentConfig.WARM_MIN_LAUNCHES
    }
}
