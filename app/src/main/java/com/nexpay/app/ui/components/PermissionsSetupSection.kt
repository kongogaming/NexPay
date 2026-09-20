// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Flowpay

package com.nexpay.app.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.nexpay.app.CompleteSetupButton
import com.nexpay.app.R
import com.nexpay.app.ui.theme.LocalNexPayAccentTheme
import com.nexpay.app.ui.theme.LocalNexPayColors
import com.nexpay.app.ui.theme.NexPayAccentGreen
import com.nexpay.app.ui.theme.NexPayStatusWarning

data class PermissionItemData(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val isGranted: Boolean,
    val isDenied: Boolean,
    val deniedNotice: String
)

private object PermissionChecker {
    fun isPhoneGranted(context: Context): Boolean {
        val hasCall = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
        val hasPhoneState = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
        return hasCall && hasPhoneState
    }

    fun isSmsGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED

    fun isCameraGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    fun isContactsGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

    fun isOverlayGranted(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }

    fun isNotifGranted(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    fun refresh(context: Context, map: MutableMap<String, Boolean>) {
        map["phone"] = isPhoneGranted(context)
        map["sms"] = isSmsGranted(context)
        map["camera"] = isCameraGranted(context)
        map["contacts"] = isContactsGranted(context)
        map["overlay"] = isOverlayGranted(context)
        map["notif"] = isNotifGranted(context)
    }
}

@Composable
fun PermissionsSetupSection(
    onContinue: () -> Unit
) {
    val colors = LocalNexPayColors.current
    val permissionItems = rememberPermissionItems()

    PermissionsListCard(items = permissionItems)

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = stringResource(R.string.setup_permission_optional_note),
        fontSize = 12.sp,
        color = colors.textSecondary,
        modifier = Modifier.padding(horizontal = 8.dp)
    )

    Spacer(modifier = Modifier.height(20.dp))

    CompleteSetupButton(
        enabled = true,
        buttonText = stringResource(R.string.setup_continue_to_app),
        onCompleteSetup = onContinue
    )
}

@Composable
private fun rememberInitialStates(
    context: Context
): SnapshotStateMap<String, Boolean> {
    val states = remember {
        mutableStateMapOf(
            "phone" to PermissionChecker.isPhoneGranted(context),
            "sms" to PermissionChecker.isSmsGranted(context),
            "camera" to PermissionChecker.isCameraGranted(context),
            "contacts" to PermissionChecker.isContactsGranted(context),
            "overlay" to PermissionChecker.isOverlayGranted(context),
            "notif" to PermissionChecker.isNotifGranted(context)
        )
    }
    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                PermissionChecker.refresh(context, states)
            }
        }
        val owner = context as? LifecycleOwner
        owner?.lifecycle?.addObserver(observer)
        onDispose { owner?.lifecycle?.removeObserver(observer) }
    }
    return states
}

@Composable
private fun rememberPermissionDispatcher(
    context: Context,
    states: SnapshotStateMap<String, Boolean>,
    denied: SnapshotStateMap<String, Boolean>
): (String) -> Unit {
    val phoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { res ->
        val g = res.values.all { it }
        states["phone"] = g
        denied["phone"] = !g
    }
    val smsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { g ->
        states["sms"] = g
        denied["sms"] = !g
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { g ->
        states["camera"] = g
        denied["camera"] = !g
    }
    val contactsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { g ->
        states["contacts"] = g
        denied["contacts"] = !g
    }
    val overlayLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val g = PermissionChecker.isOverlayGranted(context)
        states["overlay"] = g
        denied["overlay"] = !g
    }
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { g ->
        states["notif"] = g
        denied["notif"] = !g
    }

    return { key ->
        when (key) {
            "phone" -> phoneLauncher.launch(
                arrayOf(
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.READ_PHONE_STATE
                )
            )
            "sms" -> smsLauncher.launch(Manifest.permission.RECEIVE_SMS)
            "camera" -> cameraLauncher.launch(Manifest.permission.CAMERA)
            "contacts" -> contactsLauncher.launch(Manifest.permission.READ_CONTACTS)
            "overlay" -> launchOverlayPermission(context, overlayLauncher)
            "notif" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
private fun rememberPermissionItems(): List<Pair<PermissionItemData, () -> Unit>> {
    val context = LocalContext.current
    val states = rememberInitialStates(context)
    val denied = remember { mutableStateMapOf<String, Boolean>() }
    val dispatch = rememberPermissionDispatcher(context, states, denied)

    return remember(states.toMap(), denied.toMap()) {
        PermissionItemBuilder.build(context, states, denied, dispatch)
    }
}

private fun launchOverlayPermission(
    context: Context,
    launcher: ManagedActivityResultLauncher<Intent, ActivityResult>
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        launcher.launch(intent)
    }
}

private object PermissionItemBuilder {
    fun build(
        context: Context,
        states: Map<String, Boolean>,
        denied: Map<String, Boolean>,
        onLaunch: (String) -> Unit
    ): List<Pair<PermissionItemData, () -> Unit>> {
        val items = mutableListOf<Pair<PermissionItemData, () -> Unit>>()
        addCoreItems(items, context, states, denied, onLaunch)
        addSecondaryItems(items, context, states, denied, onLaunch)
        return items
    }

    private fun addCoreItems(
        items: MutableList<Pair<PermissionItemData, () -> Unit>>,
        context: Context,
        states: Map<String, Boolean>,
        denied: Map<String, Boolean>,
        onLaunch: (String) -> Unit
    ) {
        items.add(
            PermissionItemData(
                icon = Icons.Default.Phone,
                title = context.getString(R.string.perm_phone_title),
                description = context.getString(R.string.perm_phone_desc),
                isGranted = states["phone"] ?: false,
                isDenied = denied["phone"] ?: false,
                deniedNotice = context.getString(R.string.perm_phone_denied)
            ) to { onLaunch("phone") }
        )
        items.add(
            PermissionItemData(
                icon = Icons.Default.Sms,
                title = context.getString(R.string.perm_sms_title),
                description = context.getString(R.string.perm_sms_desc),
                isGranted = states["sms"] ?: false,
                isDenied = denied["sms"] ?: false,
                deniedNotice = context.getString(R.string.perm_sms_denied)
            ) to { onLaunch("sms") }
        )
        items.add(
            PermissionItemData(
                icon = Icons.Default.CameraAlt,
                title = context.getString(R.string.perm_camera_title),
                description = context.getString(R.string.perm_camera_desc),
                isGranted = states["camera"] ?: false,
                isDenied = denied["camera"] ?: false,
                deniedNotice = context.getString(R.string.perm_camera_denied)
            ) to { onLaunch("camera") }
        )
    }

    private fun addSecondaryItems(
        items: MutableList<Pair<PermissionItemData, () -> Unit>>,
        context: Context,
        states: Map<String, Boolean>,
        denied: Map<String, Boolean>,
        onLaunch: (String) -> Unit
    ) {
        items.add(
            PermissionItemData(
                icon = Icons.Default.Contacts,
                title = context.getString(R.string.perm_contacts_title),
                description = context.getString(R.string.perm_contacts_desc),
                isGranted = states["contacts"] ?: false,
                isDenied = denied["contacts"] ?: false,
                deniedNotice = context.getString(R.string.perm_contacts_denied)
            ) to { onLaunch("contacts") }
        )
        items.add(
            PermissionItemData(
                icon = Icons.Default.Layers,
                title = context.getString(R.string.perm_overlay_title),
                description = context.getString(R.string.perm_overlay_desc),
                isGranted = states["overlay"] ?: false,
                isDenied = denied["overlay"] ?: false,
                deniedNotice = context.getString(R.string.perm_overlay_denied)
            ) to { onLaunch("overlay") }
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            items.add(
                PermissionItemData(
                    icon = Icons.Default.Notifications,
                    title = context.getString(R.string.perm_notif_title),
                    description = context.getString(R.string.perm_notif_desc),
                    isGranted = states["notif"] ?: false,
                    isDenied = denied["notif"] ?: false,
                    deniedNotice = context.getString(R.string.perm_notif_denied)
                ) to { onLaunch("notif") }
            )
        }
    }
}

@Composable
private fun PermissionsListCard(
    items: List<Pair<PermissionItemData, () -> Unit>>
) {
    val accent = LocalNexPayAccentTheme.current
    val colors = LocalNexPayColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(accent.primary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = accent.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.setup_permissions_title),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = stringResource(R.string.setup_permissions_subtitle),
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        items.forEachIndexed { index, (data, onGrant) ->
            if (index > 0) {
                HorizontalDivider(
                    color = colors.border,
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
            PermissionItemRow(data = data, onGrant = onGrant)
        }
    }
}

@Composable
private fun PermissionGrantedBadge() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(NexPayAccentGreen.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = NexPayAccentGreen,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = stringResource(R.string.setup_permission_granted),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = NexPayAccentGreen
        )
    }
}

@Composable
private fun PermissionGrantButton(onGrant: () -> Unit) {
    val accent = LocalNexPayAccentTheme.current
    Button(
        onClick = onGrant,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = accent.primary),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        modifier = Modifier.height(34.dp)
    ) {
        Text(
            text = stringResource(R.string.setup_grant_permission),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

@Composable
private fun PermissionItemLeadingIcon(
    icon: ImageVector,
    isGranted: Boolean
) {
    val accent = LocalNexPayAccentTheme.current
    val iconBg = if (isGranted) {
        NexPayAccentGreen.copy(alpha = 0.15f)
    } else {
        accent.primary.copy(alpha = 0.12f)
    }
    val iconTint = if (isGranted) NexPayAccentGreen else accent.primary
    Box(
        modifier = Modifier
            .size(38.dp)
            .background(iconBg, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun PermissionItemRow(
    data: PermissionItemData,
    onGrant: () -> Unit
) {
    val colors = LocalNexPayColors.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PermissionItemLeadingIcon(
                icon = data.icon,
                isGranted = data.isGranted
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = data.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
                Text(
                    text = data.description,
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (data.isGranted) {
                PermissionGrantedBadge()
            } else {
                PermissionGrantButton(onGrant = onGrant)
            }
        }

        if (data.isDenied && !data.isGranted) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = data.deniedNotice,
                fontSize = 11.sp,
                color = NexPayStatusWarning,
                modifier = Modifier.padding(start = 50.dp)
            )
        }
    }
}
