// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Flowpay

package com.nexpay.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexpay.app.helpers.SetupHelper
import com.nexpay.app.ui.components.PermissionsSetupSection
import com.nexpay.app.ui.theme.BlueAccentTheme
import com.nexpay.app.ui.theme.LocalNexPayAccentTheme
import com.nexpay.app.ui.theme.LocalNexPayColors
import com.nexpay.app.ui.theme.NexPayLightGray
import com.nexpay.app.ui.theme.NexPayMediumGray
import com.nexpay.app.ui.theme.NexPayTheme

class SetupActivity : ComponentActivity() {
    private lateinit var setupHelper: SetupHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize setup helper
        setupHelper = SetupHelper(
            this,
            object : SetupHelper.UICallback {
                override fun showToast(message: String) {
                    runOnUiThread {
                        Toast.makeText(this@SetupActivity, message, Toast.LENGTH_LONG).show()
                    }
                }

                override fun navigateToTestConfiguration() {
                    val intent = Intent(this@SetupActivity, TestConfigurationActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        )

        setTheme(R.style.Theme_NexPay)
        // Edge-to-edge: Compose insets are the single source of padding (see MainActivity).
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            CompositionLocalProvider(LocalNexPayAccentTheme provides BlueAccentTheme) {
                NexPayTheme {
                    SetupScreen(setupHelper = setupHelper)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(setupHelper: SetupHelper) {
    val colors = LocalNexPayColors.current
    var currentStep by remember { mutableStateOf(1) }
    var pendingSetupData by remember { mutableStateOf<SetupHelper.SetupData?>(null) }

    BackHandler(enabled = currentStep == 2) {
        currentStep = 1
    }

    var selectedBank by remember { mutableStateOf("") }
    var selectedPrimarySim by remember { mutableStateOf("") }
    var selectedSecondarySim by remember { mutableStateOf("") }
    var isDualSimEnabled by remember { mutableStateOf(false) }
    var disclaimerAccepted by remember { mutableStateOf(false) }

    val banks = setupHelper.getBanks()
    val simCarriers = setupHelper.getSimCarriers()
    val secondarySimOptions = setupHelper.getSecondarySimOptions(selectedPrimarySim)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 420.dp)
                .align(Alignment.Center)
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Header Card with current step
            HeaderCard(step = currentStep)

            Spacer(modifier = Modifier.height(20.dp))

            if (currentStep == 1) {
                // Bank Selection Section
                BankSelectionSection(
                    banks = banks,
                    selectedBank = selectedBank,
                    onBankSelected = { selectedBank = it }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // SIM Card Selection Section
                SimCardSelectionSection(
                    simCarriers = simCarriers,
                    selectedPrimarySim = selectedPrimarySim,
                    selectedSecondarySim = selectedSecondarySim,
                    isDualSimEnabled = isDualSimEnabled,
                    onPrimarySimSelected = { selectedPrimarySim = it },
                    onSecondarySimSelected = { selectedSecondarySim = it },
                    onDualSimToggled = { isDualSimEnabled = it },
                    secondarySimOptions = secondarySimOptions
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Disclaimer Section
                DisclaimerSection(
                    isAccepted = disclaimerAccepted,
                    onAcceptedChange = { disclaimerAccepted = it }
                )

                Spacer(modifier = Modifier.height(20.dp))

                val isFormComplete = selectedBank.isNotBlank() &&
                    selectedPrimarySim.isNotBlank() &&
                    (!isDualSimEnabled || selectedSecondarySim.isNotBlank()) &&
                    disclaimerAccepted

                // Proceed to Step 2: Permissions Setup
                CompleteSetupButton(
                    enabled = isFormComplete,
                    buttonText = stringResource(R.string.setup_continue_to_app),
                    onCompleteSetup = {
                        val setupData = SetupHelper.SetupData(
                            selectedBank = selectedBank,
                            selectedPrimarySim = selectedPrimarySim,
                            isDualSimEnabled = isDualSimEnabled,
                            selectedSecondarySim = selectedSecondarySim,
                            disclaimerAccepted = disclaimerAccepted
                        )
                        pendingSetupData = setupData
                        currentStep = 2
                    }
                )
            } else {
                // Step 2: Permissions Setup Checklist
                PermissionsSetupSection(
                    onContinue = {
                        pendingSetupData?.let { setupHelper.completeSetup(it) }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun HeaderCard(step: Int = 1) {
    val accent = LocalNexPayAccentTheme.current
    val headerShape = RoundedCornerShape(20.dp)
    val titleRes = if (step == 1) R.string.setup_flowpay else R.string.setup_permissions_title
    val stepRes = if (step == 1) R.string.setup_step_1_of_2 else R.string.setup_step_2_of_2
    val subRes = if (step == 1) R.string.configure_upi_payments else R.string.setup_permissions_subtitle
    val icon = if (step == 1) Icons.Default.AccountBalanceWallet else Icons.Default.Security

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = headerShape,
                ambientColor = Color.Black.copy(alpha = 0.15f),
                spotColor = Color.Black.copy(alpha = 0.15f)
            ),
        shape = headerShape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(accent.headerGradientStart, accent.headerGradientEnd)
                    ),
                    shape = headerShape
                )
        ) {
            HeaderCardContent(
                step = step,
                titleRes = titleRes,
                stepRes = stepRes,
                subRes = subRes,
                icon = icon
            )
        }
    }
}

@Composable
private fun HeaderCardContent(
    step: Int,
    titleRes: Int,
    stepRes: Int,
    subRes: Int,
    icon: ImageVector
) {
    Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White.copy(alpha = 0.22f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = stringResource(titleRes),
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.15f),
                            offset = Offset(0f, 2f),
                            blurRadius = 6f
                        )
                    )
                )
                Text(
                    text = stringResource(stepRes),
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(subRes),
            fontSize = 15.sp,
            color = Color.White.copy(alpha = 0.85f),
            lineHeight = 22.sp
        )
        Spacer(modifier = Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProgressDot(isActive = true)
            ProgressDot(isActive = step >= 2)
        }
    }
}

@Composable
private fun SetupSectionHeader(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    val accent = LocalNexPayAccentTheme.current
    val colors = LocalNexPayColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(accent.primary.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun SetupFieldLabel(text: String) {
    val colors = LocalNexPayColors.current
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = colors.textSecondary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankSelectionSection(
    banks: List<Pair<String, String>>,
    selectedBank: String,
    onBankSelected: (String) -> Unit
) {
    val colors = LocalNexPayColors.current
    val accent = LocalNexPayAccentTheme.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        SetupSectionHeader(
            icon = Icons.Default.AccountBalance,
            title = stringResource(R.string.bank_selection),
            subtitle = stringResource(R.string.choose_primary_bank)
        )

        Spacer(modifier = Modifier.height(16.dp))

        SetupFieldLabel(stringResource(R.string.setup_select_bank))

        var expanded by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = banks.find { it.first == selectedBank }?.second
                    ?: stringResource(R.string.setup_choose_your_bank),
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent.primary,
                    unfocusedBorderColor = colors.border,
                    focusedContainerColor = colors.surfaceElevated,
                    unfocusedContainerColor = colors.surfaceElevated,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    focusedTrailingIconColor = colors.textPrimary,
                    unfocusedTrailingIconColor = colors.textSecondary
                ),
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(fontSize = 15.sp),
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(colors.surfaceElevated)
            ) {
                banks.forEach { (value, label) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                label,
                                color = colors.textPrimary,
                                fontSize = 15.sp
                            )
                        },
                        onClick = {
                            onBankSelected(value)
                            expanded = false
                        },
                        modifier = Modifier.background(colors.surfaceElevated)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimCardSelectionSection(
    simCarriers: List<Pair<String, String>>,
    selectedPrimarySim: String,
    selectedSecondarySim: String,
    isDualSimEnabled: Boolean,
    onPrimarySimSelected: (String) -> Unit,
    onSecondarySimSelected: (String) -> Unit,
    onDualSimToggled: (Boolean) -> Unit,
    secondarySimOptions: List<Pair<String, String>>
) {
    val accent = LocalNexPayAccentTheme.current
    val colors = LocalNexPayColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        SetupSectionHeader(
            icon = Icons.Default.SimCard,
            title = stringResource(R.string.sim_card_selection),
            subtitle = stringResource(R.string.configure_sim_cards)
        )

        Spacer(modifier = Modifier.height(16.dp))

        SetupFieldLabel(stringResource(R.string.setup_primary_sim))

        var primaryExpanded by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(
            expanded = primaryExpanded,
            onExpandedChange = { primaryExpanded = !primaryExpanded }
        ) {
            OutlinedTextField(
                value = simCarriers.find { it.first == selectedPrimarySim }?.second
                    ?: stringResource(R.string.setup_select_primary_sim),
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = primaryExpanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent.primary,
                    unfocusedBorderColor = colors.border,
                    focusedContainerColor = colors.surfaceElevated,
                    unfocusedContainerColor = colors.surfaceElevated,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    focusedTrailingIconColor = colors.textPrimary,
                    unfocusedTrailingIconColor = colors.textSecondary
                ),
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(fontSize = 15.sp),
            )

            ExposedDropdownMenu(
                expanded = primaryExpanded,
                onDismissRequest = { primaryExpanded = false },
                modifier = Modifier.background(colors.surfaceElevated)
            ) {
                simCarriers.forEach { (value, label) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                label,
                                color = colors.textPrimary,
                                fontSize = 15.sp
                            )
                        },
                        onClick = {
                            onPrimarySimSelected(value)
                            primaryExpanded = false
                        },
                        modifier = Modifier.background(colors.surfaceElevated)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDualSimToggled(!isDualSimEnabled) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .border(
                        width = 2.dp,
                        color = if (isDualSimEnabled) accent.accent else colors.border,
                        shape = CircleShape
                    )
                    .background(
                        color = if (isDualSimEnabled) accent.accent else Color.Transparent,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isDualSimEnabled) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color.White, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = stringResource(R.string.enable_dual_sim),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary
            )
        }

        if (isDualSimEnabled) {
            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(
                color = colors.border,
                thickness = 0.5.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            SetupFieldLabel(stringResource(R.string.secondary_sim))

            var secondaryExpanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = secondaryExpanded,
                onExpandedChange = { secondaryExpanded = !secondaryExpanded }
            ) {
                OutlinedTextField(
                    value = secondarySimOptions.find { it.first == selectedSecondarySim }?.second
                        ?: stringResource(R.string.setup_select_secondary_sim),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = secondaryExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent.primary,
                        unfocusedBorderColor = colors.border,
                        focusedContainerColor = colors.surfaceElevated,
                        unfocusedContainerColor = colors.surfaceElevated,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedTrailingIconColor = colors.textPrimary,
                        unfocusedTrailingIconColor = colors.textSecondary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(fontSize = 15.sp),
                )

                ExposedDropdownMenu(
                    expanded = secondaryExpanded,
                    onDismissRequest = { secondaryExpanded = false },
                    modifier = Modifier.background(colors.surfaceElevated)
                ) {
                    secondarySimOptions.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    label,
                                    color = colors.textPrimary,
                                    fontSize = 15.sp
                                )
                            },
                            onClick = {
                                onSecondarySimSelected(value)
                                secondaryExpanded = false
                            },
                            modifier = Modifier.background(colors.surfaceElevated)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DisclaimerSection(
    isAccepted: Boolean,
    onAcceptedChange: (Boolean) -> Unit
) {
    val accent = LocalNexPayAccentTheme.current
    val colors = LocalNexPayColors.current
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        SetupSectionHeader(
            icon = Icons.Outlined.Info,
            title = stringResource(R.string.disclaimer),
            subtitle = stringResource(R.string.setup_please_read_before_continuing)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(22.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onAcceptedChange(!isAccepted) }
                        .border(
                            width = 2.dp,
                            color = if (isAccepted) accent.accent else colors.border,
                            shape = CircleShape
                        )
                        .background(
                            color = if (isAccepted) accent.accent else Color.Transparent,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isAccepted) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color.White, CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { isExpanded = !isExpanded }
                ) {
                    Text(
                        text = stringResource(
                            if (isExpanded) {
                                R.string.disclaimer_text
                            } else {
                                R.string.disclaimer_summary
                            }
                        ),
                        fontSize = 14.sp,
                        color = colors.textSecondary,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(
                            if (isExpanded) {
                                R.string.setup_show_less
                            } else {
                                R.string.setup_show_more
                            }
                        ),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = accent.accent
                    )
                }
            }
        }
    }
}

@Composable
fun CompleteSetupButton(
    enabled: Boolean,
    buttonText: String = stringResource(R.string.complete_setup),
    onCompleteSetup: () -> Unit
) {
    val accent = LocalNexPayAccentTheme.current
    val buttonShape = RoundedCornerShape(16.dp)

    val gradientColors = if (enabled) {
        listOf(accent.headerGradientStart, accent.headerGradientEnd)
    } else {
        listOf(NexPayLightGray, NexPayMediumGray)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(
                brush = Brush.linearGradient(gradientColors),
                shape = buttonShape
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = if (enabled) 0.15f else 0.05f),
                shape = buttonShape
            )
            .clip(buttonShape)
            .clickable(enabled = enabled) { onCompleteSetup() }
            .alpha(if (enabled) 1f else 0.4f),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = buttonText,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
