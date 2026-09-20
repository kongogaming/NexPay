// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Flowpay

package com.nexpay.app.helpers

import android.content.Context
import com.nexpay.app.constants.AppConstants
import com.nexpay.app.ui.activities.banks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SetupBankSelectionTest {

    private val context: Context
        get() = RuntimeEnvironment.getApplication()

    private val dummyCallback = object : SetupHelper.UICallback {
        override fun showToast(message: String) = Unit
        override fun navigateToTestConfiguration() = Unit
    }

    private fun createHelper(): SetupHelper = SetupHelper(context, dummyCallback)

    @Test
    fun `SetupHelper bank list includes Slice Small Finance Bank`() {
        val helper = createHelper()
        val bankList = helper.getBanks()
        val sliceEntry = bankList.find { it.first == "slice" }
        assertEquals("slice" to "Slice Small Finance Bank", sliceEntry)
    }

    @Test
    fun `SetupHelper bank list preserves all existing banks`() {
        val helper = createHelper()
        val bankMap = helper.getBanks().toMap()
        assertEquals("State Bank of India", bankMap["sbi"])
        assertEquals("HDFC Bank", bankMap["hdfc"])
        assertEquals("ICICI Bank", bankMap["icici"])
        assertEquals("Axis Bank", bankMap["axis"])
        assertEquals("Kotak Mahindra Bank", bankMap["kotak"])
        assertEquals("Punjab National Bank", bankMap["pnb"])
        assertEquals("Bank of Baroda", bankMap["bob"])
        assertEquals("Yes Bank", bankMap["yes"])
        assertEquals("IDBI Bank", bankMap["idbi"])
        assertEquals("Canara Bank", bankMap["canara"])
        assertEquals("Slice Small Finance Bank", bankMap["slice"])
    }

    @Test
    fun `SetupHelper validates slice bank selection as valid`() {
        val helper = createHelper()
        val result = helper.validateBankSelection("slice")
        assertTrue("Slice bank selection should be valid", result.isValid)
        assertEquals("", result.errorMessage)
    }

    @Test
    fun `SetupHelper saves slice bank selection to shared preferences`() {
        val helper = createHelper()
        val setupData = SetupHelper.SetupData(
            selectedBank = "slice",
            selectedPrimarySim = "airtel",
            isDualSimEnabled = false,
            selectedSecondarySim = "",
            disclaimerAccepted = true
        )
        helper.saveSetupData(setupData)

        val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        assertEquals("slice", prefs.getString("selected_bank", null))
    }

    @Test
    fun `SettingsActivity banks list includes Slice Small Finance Bank`() {
        val sliceBank = banks.find { it.id == "slice" }
        assertTrue("Slice bank should be present in SettingsActivity banks", sliceBank != null)
        assertEquals("Slice Small Finance Bank", sliceBank?.name)
    }
}
