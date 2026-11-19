package com.supernova.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderAutoSortTest {

    private fun placed(folder: String, vararg pkgs: String) =
        pkgs.map { FolderAutoSort.Placed(folder, it, it.substringAfterLast('.')) }

    private val folders = placed("money", "ua.privatbank.ap24", "ua.bank.amo.app", "com.bank.kd", "ua.oschadbank.flumo") +
        placed("vpn", "ch.protonvpn.android", "com.vpnhood.connect.android", "com.macpaw.clearvpn.android") +
        placed("chat", "org.telegram.messenger", "com.viber.voip", "com.whatsapp")

    private fun c(pkg: String, label: String = pkg.substringAfterLast('.')) =
        FolderAutoSort.Candidate("0/$pkg/Main", pkg, label)

    @Test
    fun anAppSharingAWordWithAFolderGoesThere() {
        val plan = FolderAutoSort.suggest(folders, listOf(c("ua.bank.newbank"), c("com.vpnhood.client.android.web")))
        assertEquals("money", plan["0/ua.bank.newbank/Main"])
        assertEquals("vpn", plan["0/com.vpnhood.client.android.web/Main"])
    }

    @Test
    fun anAppWithNothingInCommonStaysWhereItIs() {
        assertTrue(FolderAutoSort.suggest(folders, listOf(c("com.example.calculator"))).isEmpty())
    }

    @Test
    fun aWordOnlyOneFolderAppCarriesIsNotEnough() {
        assertTrue(FolderAutoSort.suggest(folders, listOf(c("org.thing.messenger"))).isEmpty())
    }
}
