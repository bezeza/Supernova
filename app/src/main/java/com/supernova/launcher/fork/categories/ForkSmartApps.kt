package com.supernova.launcher.fork.categories

import com.supernova.launcher.data.SmartCategory

// Apps used in Ukraine that Smart Folders would not place: the original's list knows other countries' apps, and few
// of these declare a category. Every package here was looked up in Google Play (2026-11-25). Matched whole, not by
// prefix, so a maker's other apps are not swept in.
internal object ForkSmartApps {

    private val apps: Map<SmartCategory, List<String>> = mapOf(
        SmartCategory.FINANCE to listOf(
            "ua.privatbank.ap24", "com.ftband.mono", "ua.oschadbank.flumo", "com.fuib.android.spot.online",
            "ua.com.abank", "ua.raiffeisen.myraif", "ua.alfabank.mobile.android", "ua.izibank.app",
            "ua.otpbank.android", "com.ukrsibbank.uso.android", "com.ugb.app", "com.tas.tas2u"
        ),
        SmartCategory.MAPS to listOf("ee.mtakso.client", "ua.com.uklontaxi", "ua.gov.uz", "com.eway"),
        SmartCategory.ENTERTAINMENT to listOf("com.megogo.application", "tv.sweet.player", "com.kyivstar.tv.mobile"),
        SmartCategory.SHOPPING to listOf(
            "ua.com.rozetka.shop", "ua.slando", "ua.prom.b2c", "ua.epicentrk", "com.atb.atbmarket",
            "ua.silpo.android", "eu.novapost", "ua.novaposhtaa", "ua.ukrposhta.android.app", "com.glovo",
            "com.bolt.deliveryclient", "ua.comfy.comfystore", "allo.ua", "ua.com.foxtrot", "com.makeupstore.app",
            "com.alibaba.aliexpresshd", "com.einnovation.temu"
        ),
        SmartCategory.GOVERNMENT to listOf(
            "ua.gov.diia.app", "ua.gov.reserveplus", "my.tax.gov.ua", "com.ukrainealarm",
            "com.kyivstar.mykyivstar", "ua.vodafone.myvodafone", "com.life.my"
        )
    )

    private val byPackage: Map<String, SmartCategory> =
        apps.flatMap { (category, packages) -> packages.map { it to category } }.toMap()

    val size: Int get() = apps.values.sumOf { it.size }

    val packages: Set<String> get() = byPackage.keys

    fun categoryOf(packageName: String): SmartCategory? = byPackage[packageName]
}
