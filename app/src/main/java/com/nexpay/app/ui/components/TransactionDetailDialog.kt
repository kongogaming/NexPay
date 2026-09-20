// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Flowpay

package com.nexpay.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nexpay.app.R
import com.nexpay.app.data.Transaction
import com.nexpay.app.ui.theme.NexPayStatusError
import com.nexpay.app.ui.theme.LocalNexPayAccentTheme
import com.nexpay.app.ui.theme.LocalNexPayColors
import com.nexpay.app.ui.theme.statusColor
import com.nexpay.app.utils.CurrencyFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionDetailDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val clipboardManager = LocalClipboardManager.current
    val accent = LocalNexPayAccentTheme.current
    val colors = LocalNexPayColors.current
    val statusColor = statusColor(transaction.status)
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.surface,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.border)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.detail_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surfaceElevated)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.detail_close),
                            tint = colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Large amount
                Text(
                    text = stringResource(R.string.amount_rupees, CurrencyFormat.inr(transaction.amount)),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent.headerGradientStart,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Status pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = transaction.status.lowercase()
                            .replaceFirstChar { it.uppercase() },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                }

                // Non-success outcomes get a plain-language explanation
                statusExplainerText(transaction.status)?.let { explainer ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = explainer,
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Detail card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.surfaceElevated,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Bank reference — the number from the bank's own SMS,
                        // the one a user would actually quote back to their
                        // bank in a dispute. Shown first and only when the
                        // bank supplied one (a PENDING row has none yet).
                        if (!transaction.bankRef.isNullOrEmpty()) {
                            DetailRow(
                                label = stringResource(R.string.label_bank_reference),
                                value = transaction.bankRef,
                                onCopy = { clipboardManager.setText(AnnotatedString(transaction.bankRef)) }
                            )
                            DetailDivider()
                        }

                        // Bank
                        DetailRow(
                            label = stringResource(R.string.label_bank),
                            value = transaction.bankName,
                            onCopy = { clipboardManager.setText(AnnotatedString(transaction.bankName)) }
                        )

                        // Recipient
                        if (!transaction.recipientName.isNullOrEmpty()) {
                            DetailDivider()
                            DetailRow(
                                label = stringResource(R.string.detail_label_recipient),
                                value = transaction.recipientName,
                                onCopy = { clipboardManager.setText(AnnotatedString(transaction.recipientName)) }
                            )
                        }

                        // Phone
                        if (!transaction.phoneNumber.isNullOrEmpty()) {
                            DetailDivider()
                            DetailRow(
                                label = stringResource(R.string.detail_label_phone_number),
                                value = transaction.phoneNumber,
                                onCopy = { clipboardManager.setText(AnnotatedString(transaction.phoneNumber)) }
                            )
                        }

                        // UPI ID
                        if (!transaction.upiId.isNullOrEmpty()) {
                            DetailDivider()
                            DetailRow(
                                label = stringResource(R.string.label_upi_id),
                                value = transaction.upiId,
                                onCopy = { clipboardManager.setText(AnnotatedString(transaction.upiId)) }
                            )
                        }

                        DetailDivider()

                        // Date & Time
                        DetailRow(
                            label = stringResource(R.string.label_date_time),
                            value = formatFullDate(transaction.timestamp),
                            onCopy = {
                                clipboardManager.setText(
                                    AnnotatedString(formatFullDate(transaction.timestamp))
                                )
                            }
                        )
                    }
                }

                // Privacy-safe bank summary (raw SMS bodies are not stored)
                if (transaction.smsExcerpt.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = colors.surfaceElevated,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = stringResource(R.string.detail_bank_confirmation),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = transaction.smsExcerpt,
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Delete button
                if (onDelete != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(NexPayStatusError.copy(alpha = 0.1f))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { showDeleteConfirm = true }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.detail_delete_transaction),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = NexPayStatusError
                        )
                    }
                }
            }
        }
    }

    // Deletion is permanent, so confirm before removing the record.
    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = colors.surface,
            titleContentColor = colors.textPrimary,
            textContentColor = colors.textSecondary,
            title = {
                Text(
                    stringResource(R.string.detail_delete_confirm_title),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    "This removes the record from your history on this device. " +
                        "It cannot be undone and does not affect the actual payment.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) {
                    Text(
                        stringResource(R.string.action_delete),
                        color = NexPayStatusError,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.action_cancel), color = colors.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    val colors = LocalNexPayColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(colors.surface)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onCopy() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy",
                modifier = Modifier.size(14.dp),
                tint = colors.textSecondary
            )
        }
    }
}

@Composable
private fun DetailDivider() {
    val colors = LocalNexPayColors.current
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 2.dp),
        thickness = 0.5.dp,
        color = colors.border
    )
}

/** Localised plain-language meaning of a non-success lifecycle status. */
@androidx.compose.runtime.Composable
private fun statusExplainerText(status: String): String? = when (status.uppercase()) {
    "PENDING" -> androidx.compose.ui.res.stringResource(com.nexpay.app.R.string.status_explainer_pending)
    "UNVERIFIED" -> androidx.compose.ui.res.stringResource(com.nexpay.app.R.string.status_explainer_unverified)
    "NEEDS_REVIEW" -> androidx.compose.ui.res.stringResource(com.nexpay.app.R.string.status_explainer_needs_review)
    "CANCELLED" -> androidx.compose.ui.res.stringResource(com.nexpay.app.R.string.status_explainer_cancelled)
    "FAILED" -> androidx.compose.ui.res.stringResource(com.nexpay.app.R.string.status_explainer_failed)
    else -> null
}

private fun formatFullDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale("en", "IN"))
    return formatter.format(Date(timestamp))
}
