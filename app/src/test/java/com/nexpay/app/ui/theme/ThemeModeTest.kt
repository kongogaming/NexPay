// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Flowpay

package com.nexpay.app.ui.theme

import android.content.Context
import android.content.res.Configuration
import com.nexpay.app.constants.AppConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ThemeModeTest {

    private val context: Context
        get() = RuntimeEnvironment.getApplication()

    @Test
    fun `theme mode constants match expected keys and values`() {
        assertEquals("app_theme_mode", AppConstants.KEY_THEME_MODE)
        assertEquals("system", AppConstants.THEME_MODE_SYSTEM)
        assertEquals("light", AppConstants.THEME_MODE_LIGHT)
        assertEquals("dark", AppConstants.THEME_MODE_DARK)
    }

    @Test
    fun `getSavedThemeMode defaults to system`() {
        val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(AppConstants.KEY_THEME_MODE).apply()
        assertEquals(AppConstants.THEME_MODE_SYSTEM, getSavedThemeMode(context))
    }

    @Test
    fun `saveThemeMode persists and getSavedThemeMode retrieves updated mode`() {
        saveThemeMode(context, AppConstants.THEME_MODE_LIGHT)
        assertEquals(AppConstants.THEME_MODE_LIGHT, getSavedThemeMode(context))

        saveThemeMode(context, AppConstants.THEME_MODE_DARK)
        assertEquals(AppConstants.THEME_MODE_DARK, getSavedThemeMode(context))

        saveThemeMode(context, AppConstants.THEME_MODE_SYSTEM)
        assertEquals(AppConstants.THEME_MODE_SYSTEM, getSavedThemeMode(context))
    }

    @Test
    fun `isAppInDarkTheme returns false for light mode`() {
        val config = Configuration().apply { uiMode = Configuration.UI_MODE_NIGHT_YES }
        assertFalse(isAppInDarkTheme(AppConstants.THEME_MODE_LIGHT, config))
    }

    @Test
    fun `isAppInDarkTheme returns true for dark mode`() {
        val config = Configuration().apply { uiMode = Configuration.UI_MODE_NIGHT_NO }
        assertTrue(isAppInDarkTheme(AppConstants.THEME_MODE_DARK, config))
    }

    @Test
    fun `isAppInDarkTheme follows system night mode when set to system default`() {
        val darkConfig = Configuration().apply { uiMode = Configuration.UI_MODE_NIGHT_YES }
        val lightConfig = Configuration().apply { uiMode = Configuration.UI_MODE_NIGHT_NO }
        assertTrue(isAppInDarkTheme(AppConstants.THEME_MODE_SYSTEM, darkConfig))
        assertFalse(isAppInDarkTheme(AppConstants.THEME_MODE_SYSTEM, lightConfig))
    }
}
