package com.supernova.launcher.fork.categories

import android.content.pm.ApplicationInfo
import com.supernova.launcher.data.AppCategories
import com.supernova.launcher.data.SmartCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Where Smart Folders put the apps of the fork's list, and that the original's own placing stays.
class ForkSmartAppsTest {

    private fun of(packageName: String) = AppCategories.categoryOf(packageName, ApplicationInfo.CATEGORY_UNDEFINED)

    @Test
    fun ukrainianAppsHaveACategory() {
        assertEquals(SmartCategory.FINANCE, of("ua.privatbank.ap24"))
        assertEquals(SmartCategory.FINANCE, of("com.ftband.mono"))
        assertEquals(SmartCategory.GOVERNMENT, of("ua.gov.diia.app"))
        assertEquals(SmartCategory.SHOPPING, of("ua.com.rozetka.shop"))
        assertEquals(SmartCategory.MAPS, of("ua.com.uklontaxi"))
    }

    @Test
    fun noPackageIsListedTwice() {
        assertEquals(ForkSmartApps.size, ForkSmartApps.packages.size)
    }

    @Test
    fun aPackageIsMatchedWholeAndTheOriginalsListStillPlacesItsOwn() {
        assertNull(of("ua.slando.other"))
        assertEquals(SmartCategory.FINANCE, of("com.paypal.android.p2pmobile"))
        assertEquals(SmartCategory.SOCIAL, of("org.telegram.messenger"))
    }
}
