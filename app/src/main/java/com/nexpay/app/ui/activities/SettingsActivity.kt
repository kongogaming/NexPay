// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Flowpay

// SettingsActivity.kt
package com.nexpay.app.ui.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexpay.app.BuildConfig
import com.nexpay.app.NexPayApplication
import com.nexpay.app.R
import com.nexpay.app.SetupActivity
import com.nexpay.app.constants.AppConstants
import com.nexpay.app.data.SettingsRepository
import com.nexpay.app.repository.TransactionRepository
import com.nexpay.app.ui.theme.BlueAccentTheme
import com.nexpay.app.ui.theme.LocalNexPayAccentTheme
import com.nexpay.app.ui.theme.LocalNexPayColors
import com.nexpay.app.ui.theme.NexPayAccentGreenBright
import com.nexpay.app.ui.theme.NexPayStatusError
import com.nexpay.app.ui.theme.NexPayTheme
import com.nexpay.app.ui.theme.getSavedThemeMode
import com.nexpay.app.ui.theme.saveThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : ComponentActivity() {

    private var refreshTrigger = mutableIntStateOf(0)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> refreshTrigger.intValue++ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as? NexPayApplication
        val settingsRepository = app?.settingsRepository ?: SettingsRepository(applicationContext)
        setTheme(R.style.Theme_NexPay)
        setContent {
            val viewModel: SettingsViewModel = viewModel()
            val themeMode = viewModel.state.themeMode
            CompositionLocalProvider(LocalNexPayAccentTheme provides BlueAccentTheme) {
                NexPaySettingsTheme(themeMode = themeMode) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onBackPressed = { finish() },
                        settingsRepository = settingsRepository,
                        onRequestPermissions = { permissions ->
                            permissionLauncher.launch(permissions)
                        },
                        refreshTrigger = refreshTrigger
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshTrigger.intValue++
    }
}

// Theme
@Composable
fun NexPaySettingsTheme(
    themeMode: String = AppConstants.THEME_MODE_SYSTEM,
    content: @Composable () -> Unit
) {
    NexPayTheme(themeMode = themeMode, content = content)
}

// Data Classes
data class Bank(
    val id: String,
    val name: String
)

data class SettingsState(
    val selectedBank: Bank = banks.first(),
    val ussdTimeout: Int = 30,
    val smsDetectionEnabled: Boolean = true,
    val overlayEnabled: Boolean = true,
    val debugMode: Boolean = false,
    val setupCompleted: Boolean = true,
    val permissions: Map<String, Boolean> = emptyMap(),
    val themeMode: String = AppConstants.THEME_MODE_SYSTEM
)

// Bank Data
val banks = listOf(
    Bank("hdfc", "HDFC Bank"),
    Bank("sbi", "State Bank of India"),
    Bank("icici", "ICICI Bank"),
    Bank("axis", "Axis Bank"),
    Bank("kotak", "Kotak Mahindra Bank"),
    Bank("pnb", "Punjab National Bank"),
    Bank("bob", "Bank of Baroda"),
    Bank("yes", "Yes Bank"),
    Bank("idbi", "IDBI Bank"),
    Bank("canara", "Canara Bank"),
    Bank("slice", "Slice Small Finance Bank")
)

// ViewModel
class SettingsViewModel : androidx.lifecycle.ViewModel() {
    var state by mutableStateOf(SettingsState())
        private set

    fun updateBank(bank: Bank) {
        state = state.copy(selectedBank = bank)
    }

    fun updateThemeMode(mode: String, context: Context) {
        state = state.copy(themeMode = mode)
        saveThemeMode(context, mode)
    }

    fun toggleSmsDetection() {
        state = state.copy(smsDetectionEnabled = !state.smsDetectionEnabled)
    }

    fun toggleOverlay() {
        state = state.copy(overlayEnabled = !state.overlayEnabled)
    }

    fun refreshPermissions(context: Context) {
        val perms = mapOf(
            "phone" to (
                ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                ),
            "camera" to (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED),
            "sms" to (ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED),
            "contacts" to (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED),
            "overlay" to (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true)
        )
        state = state.copy(permissions = perms)
    }

    fun loadFromRepository(settingsRepository: SettingsRepository, context: Context) {
        val saved = settingsRepository.settingsFlow.value
        val bank = banks.find { it.id == saved.bankId } ?: banks.first()
        val savedTheme = getSavedThemeMode(context)
        state = state.copy(
            selectedBank = bank,
            ussdTimeout = saved.ussdTimeout,
            smsDetectionEnabled = saved.smsDetectionEnabled,
            overlayEnabled = saved.overlayEnabled,
            debugMode = saved.debugMode,
            setupCompleted = saved.setupCompleted,
            themeMode = savedTheme
        )
    }
}

// ═══════════════════════════════════════════
// Main Settings Screen
// ═══════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onBackPressed: () -> Unit,
    settingsRepository: SettingsRepository? = null,
    onRequestPermissions: (Array<String>) -> Unit = {},
    refreshTrigger: MutableIntState = mutableIntStateOf(0)
) {
    val context = LocalContext.current
    val accent = LocalNexPayAccentTheme.current
    val colors = LocalNexPayColors.current
    val state = viewModel.state

    // Load settings from repository on first composition
    LaunchedEffect(Unit) {
        settingsRepository?.let { viewModel.loadFromRepository(it, context) }
        viewModel.refreshPermissions(context)
    }

    // Refresh permissions when trigger changes
    val trigger by refreshTrigger
    LaunchedEffect(trigger) {
        viewModel.refreshPermissions(context)
    }

    // Get primary SIM name from shared prefs (reactive)
    var primarySimId by remember {
        val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        mutableStateOf(prefs.getString("selected_primary_sim", "") ?: "")
    }
    val primarySim = when (primarySimId) {
        "jio" -> "Jio"
        "airtel" -> "Airtel"
        "vodafone" -> "Vodafone"
        "bsnl" -> "BSNL"
        else -> stringResource(R.string.settings_not_set)
    }

    // Dialog states
    var showBankPicker by remember { mutableStateOf(false) }
    var showSimPicker by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showClearDataConfirm by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                            tint = colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background
                )
            )

            // Settings Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // ═══ CONFIGURATION ═══
                item { SectionHeader("CONFIGURATION") }
                item {
                    GroupCard {
                        SettingsRow(
                            icon = Icons.Default.AccountBalance,
                            title = "Bank",
                            value = state.selectedBank.name,
                            onClick = { showBankPicker = true }
                        )
                        GroupDivider()
                        SettingsRow(
                            icon = Icons.Default.SimCard,
                            title = "Primary SIM",
                            value = primarySim,
                            onClick = { showSimPicker = true }
                        )
                    }
                }

                // ═══ APPEARANCE ═══
                item { Spacer(modifier = Modifier.height(8.dp)) }
                item { SectionHeader(stringResource(R.string.settings_section_appearance)) }
                item {
                    GroupCard {
                        SettingsRow(
                            icon = Icons.Default.Palette,
                            title = stringResource(R.string.settings_theme_title),
                            value = when (state.themeMode) {
                                AppConstants.THEME_MODE_LIGHT -> stringResource(R.string.theme_mode_light)
                                AppConstants.THEME_MODE_DARK -> stringResource(R.string.theme_mode_dark)
                                else -> stringResource(R.string.theme_mode_system)
                            },
                            onClick = { showThemePicker = true }
                        )
                    }
                }

                // ═══ PERMISSIONS ═══
                item { Spacer(modifier = Modifier.height(8.dp)) }
                item { SectionHeader("PERMISSIONS") }
                item {
                    GroupCard {
                        PermissionRow(
                            icon = Icons.Default.Phone,
                            title = "Phone",
                            subtitle = "Calls & phone state",
                            granted = state.permissions["phone"] ?: false,
                            onRequest = {
                                onRequestPermissions(
                                    arrayOf(
                                        Manifest.permission.CALL_PHONE,
                                        Manifest.permission.READ_PHONE_STATE
                                    )
                                )
                            }
                        )
                        GroupDivider()
                        PermissionRow(
                            icon = Icons.Default.CameraAlt,
                            title = "Camera",
                            subtitle = "QR code scanning",
                            granted = state.permissions["camera"] ?: false,
                            onRequest = {
                                onRequestPermissions(arrayOf(Manifest.permission.CAMERA))
                            }
                        )
                        GroupDivider()
                        PermissionRow(
                            icon = Icons.Default.Sms,
                            title = "SMS",
                            subtitle = "Bank payment confirmations",
                            granted = state.permissions["sms"] ?: false,
                            onRequest = {
                                onRequestPermissions(arrayOf(Manifest.permission.RECEIVE_SMS))
                            }
                        )
                        GroupDivider()
                        PermissionRow(
                            icon = Icons.Default.Contacts,
                            title = "Contacts",
                            subtitle = "Pay by contact",
                            granted = state.permissions["contacts"] ?: false,
                            onRequest = {
                                onRequestPermissions(arrayOf(Manifest.permission.READ_CONTACTS))
                            }
                        )
                    }
                }

                // ═══ ACTIONS ═══
                item { Spacer(modifier = Modifier.height(8.dp)) }
                item { SectionHeader("ACTIONS") }
                item {
                    GroupCard {
                        SettingsRow(
                            icon = Icons.Default.DeleteForever,
                            title = "Clear App Data",
                            value = "Reset all settings",
                            destructive = true,
                            onClick = { showClearDataConfirm = true }
                        )
                    }
                }

                // ═══ ABOUT ═══
                item { Spacer(modifier = Modifier.height(8.dp)) }
                item { SectionHeader("ABOUT") }
                item {
                    GroupCard {
                        SettingsRow(
                            icon = Icons.Default.Info,
                            title = "Version",
                            // Read from the build, never hardcoded: this row
                            // once said "1.0.0" while the app shipped as 2.1.0.
                            value = BuildConfig.VERSION_NAME
                        )
                        GroupDivider()
                        SettingsRow(
                            icon = Icons.Default.PhoneAndroid,
                            title = "Android",
                            value = Build.VERSION.RELEASE
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    // Bank Picker Dialog
    if (showBankPicker) {
        BankPickerDialog(
            selectedBank = state.selectedBank,
            onBankSelected = { bank ->
                viewModel.updateBank(bank)
                settingsRepository?.saveSettings(
                    settingsRepository.settingsFlow.value.copy(bankId = bank.id)
                )
                // Also sync to NexPayPrefs so the main screen picks it up
                context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
                    .edit().putString(AppConstants.KEY_SELECTED_BANK, bank.id).apply()
                showBankPicker = false
            },
            onDismiss = { showBankPicker = false }
        )
    }

    // SIM Picker Dialog
    if (showSimPicker) {
        SimPickerDialog(
            selectedSimId = primarySimId,
            onSimSelected = { simId ->
                primarySimId = simId
                context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
                    .edit().putString("selected_primary_sim", simId).apply()
                showSimPicker = false
            },
            onDismiss = { showSimPicker = false }
        )
    }

    // Theme Picker Dialog
    if (showThemePicker) {
        ThemePickerDialog(
            currentThemeMode = state.themeMode,
            onThemeSelected = { mode ->
                viewModel.updateThemeMode(mode, context)
                showThemePicker = false
            },
            onDismiss = { showThemePicker = false }
        )
    }

    // Clear Data Confirmation Dialog
    if (showClearDataConfirm) {
        AlertDialog(
            onDismissRequest = { showClearDataConfirm = false },
            containerColor = colors.surface,
            titleContentColor = colors.textPrimary,
            textContentColor = colors.textSecondary,
            title = {
                Text(
                    stringResource(R.string.settings_clear_data_title),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    "This permanently deletes your entire transaction history and " +
                        "resets all settings, then returns you to the setup screen. " +
                        "This cannot be undone.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showClearDataConfirm = false
                    // Wipe the encrypted transaction DB before anything else, on
                    // the process scope so the delete survives the CLEAR_TASK
                    // relaunch below; only then reset prefs and restart into Setup.
                    val app = NexPayApplication.from(context)
                    val relaunch = {
                        settingsRepository?.clearAllData()
                        context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
                            .edit().clear().apply()
                        context.startActivity(
                            Intent(context, SetupActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                        )
                    }
                    if (app != null) {
                        app.appScope.launch {
                            runCatching {
                                TransactionRepository.getInstance(context).deleteAllTransactions()
                            }
                            withContext(Dispatchers.Main) { relaunch() }
                        }
                    } else {
                        relaunch()
                    }
                }) {
                    Text(
                        stringResource(R.string.action_clear),
                        color = NexPayStatusError,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataConfirm = false }) {
                    Text(stringResource(R.string.action_cancel), color = colors.textSecondary)
                }
            }
        )
    }
}

// ═══════════════════════════════════════════
// Reusable Components
// ═══════════════════════════════════════════

@Composable
private fun SectionHeader(title: String) {
    val colors = LocalNexPayColors.current
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = colors.textTertiary,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun GroupCard(content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalNexPayColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.surface
    ) {
        Column(content = content)
    }
}

@Composable
private fun GroupDivider() {
    val colors = LocalNexPayColors.current
    HorizontalDivider(
        color = colors.border,
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 56.dp)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    value: String,
    destructive: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val accent = LocalNexPayAccentTheme.current
    val colors = LocalNexPayColors.current
    val iconColor = if (destructive) NexPayStatusError else accent.primary
    val iconBg = if (destructive) NexPayStatusError.copy(alpha = 0.12f) else accent.primary.copy(alpha = 0.12f)
    val titleColor = if (destructive) NexPayStatusError else colors.textPrimary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = titleColor,
            modifier = Modifier.weight(1f)
        )

        // Value or chevron
        if (onClick != null) {
            Text(
                text = value,
                fontSize = 14.sp,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 160.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Text(
                text = value,
                fontSize = 14.sp,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    granted: Boolean,
    onRequest: () -> Unit
) {
    val accent = LocalNexPayAccentTheme.current
    val colors = LocalNexPayColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(accent.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent.primary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title + subtitle
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = colors.textSecondary
            )
        }

        // Status pill
        if (granted) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = NexPayAccentGreenBright.copy(alpha = 0.12f)
            ) {
                Text(
                    text = "Granted",
                    color = NexPayAccentGreenBright,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        } else {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accent.primary.copy(alpha = 0.12f),
                modifier = Modifier.clickable(onClick = onRequest)
            ) {
                Text(
                    text = "Grant",
                    color = accent.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun BankPickerDialog(
    selectedBank: Bank,
    onBankSelected: (Bank) -> Unit,
    onDismiss: () -> Unit
) {
    val accent = LocalNexPayAccentTheme.current
    val colors = LocalNexPayColors.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.surface
        ) {
            Column(
                modifier = Modifier.padding(vertical = 20.dp)
            ) {
                Text(
                    text = "Select Bank",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(banks) { bank ->
                        val isSelected = bank.id == selectedBank.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onBankSelected(bank) }
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = bank.name,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) accent.primary else colors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = accent.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        if (bank != banks.last()) {
                            HorizontalDivider(
                                color = colors.border,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(stringResource(R.string.action_cancel), color = colors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun SimPickerDialog(
    selectedSimId: String,
    onSimSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val accent = LocalNexPayAccentTheme.current
    val colors = LocalNexPayColors.current
    val sims = listOf(
        "jio" to "Jio",
        "airtel" to "Airtel",
        "vodafone" to "Vodafone",
        "bsnl" to "BSNL"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.surface
        ) {
            Column(modifier = Modifier.padding(vertical = 20.dp)) {
                Text(
                    text = "Select Primary SIM",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )

                sims.forEachIndexed { index, (id, name) ->
                    val isSelected = id == selectedSimId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSimSelected(id) }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = name,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) accent.primary else colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = accent.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    if (index < sims.lastIndex) {
                        HorizontalDivider(
                            color = colors.border,
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(stringResource(R.string.action_cancel), color = colors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun ThemePickerDialog(
    currentThemeMode: String,
    onThemeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val accent = LocalNexPayAccentTheme.current
    val colors = LocalNexPayColors.current

    val options = listOf(
        AppConstants.THEME_MODE_SYSTEM to stringResource(R.string.theme_mode_system),
        AppConstants.THEME_MODE_LIGHT to stringResource(R.string.theme_mode_light),
        AppConstants.THEME_MODE_DARK to stringResource(R.string.theme_mode_dark)
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.surface
        ) {
            Column(
                modifier = Modifier.padding(vertical = 20.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_select_theme),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(options) { (mode, label) ->
                        val isSelected = mode == currentThemeMode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onThemeSelected(mode) }
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) accent.primary else colors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = stringResource(R.string.theme_selected),
                                    tint = accent.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        if (mode != options.last().first) {
                            HorizontalDivider(
                                color = colors.border,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(stringResource(R.string.action_cancel), color = colors.textSecondary)
                }
            }
        }
    }
}
