package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.BookingEntity
import com.example.data.local.ReminderLogEntity
import com.example.data.local.ReminderLogStatus
import com.example.ui.ShuttleUiState
import com.example.ui.components.PaymentStatusPill
import com.example.ui.theme.MonoPriceStyle
import com.example.ui.theme.MonoTimeStyle
import com.example.ui.theme.StatusAutoBg
import com.example.ui.theme.StatusAutoFg
import com.example.ui.theme.StatusPaidBg
import com.example.ui.theme.StatusPaidFg
import com.example.ui.theme.StatusPendingBg
import com.example.ui.theme.StatusPendingFg
import com.example.util.ScheduleFormatters

@Composable
fun PaymentsScreen(
    uiState: ShuttleUiState,
    hasNotificationPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onMarkPaid: (Long) -> Unit,
    onRecordDp: (Long, Long) -> Unit,
    onSendReminderNow: (Long) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var dpDialogBooking by remember { mutableStateOf<BookingEntity?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (!hasNotificationPermission) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Izin Notifikasi Belum Aktif",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Izinkan notifikasi agar konfirmasi pemesanan dan pengingat pembayaran tampil di bilah status.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Button(
                            onClick = onRequestNotificationPermission,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("grant_notification_permission_button")
                        ) {
                            Text("Izinkan")
                        }
                    }
                }
            }
        }

        // Financial Summary Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryMetricCard(
                    title = "Pendapatan Masuk",
                    value = ScheduleFormatters.formatRupiah(uiState.totalPaidRevenue),
                    icon = Icons.Filled.AccountBalanceWallet,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                SummaryMetricCard(
                    title = "Tagihan Tertunda",
                    value = ScheduleFormatters.formatRupiah(uiState.totalPendingAmount),
                    icon = Icons.Filled.Payments,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Pengingat Pembayaran Aktif",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    Text(
                        text = "${uiState.activeReminderCount} tagihan",
                        style = MonoTimeStyle,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Pending Payments Section
        item {
            Text(
                text = "Menunggu Pembayaran (${uiState.pendingBookings.size})",
                style = MaterialTheme.typography.titleLarge
            )
        }

        if (uiState.pendingBookings.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = StatusPaidFg
                        )
                        Text(
                            text = "Semua pemesanan sudah lunas atau belum ada tagihan tertunda.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(uiState.pendingBookings, key = { "pending_${it.id}" }) { booking ->
                PendingPaymentCard(
                    booking = booking,
                    onMarkPaid = { onMarkPaid(booking.id) },
                    onRecordDpClick = { dpDialogBooking = booking },
                    onSendReminderNow = { onSendReminderNow(booking.id) },
                    onCopyMessage = {
                        val remaining = (booking.totalPrice - booking.paidAmount).coerceAtLeast(0L)
                        val msg = ScheduleFormatters.buildPaymentReminderMessage(
                            customerName = booking.customerName,
                            courtName = booking.courtName,
                            dayOfWeek = booking.dayOfWeek,
                            startHour = booking.startHour,
                            durationHours = booking.durationHours,
                            amountDue = remaining
                        )
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Tagihan Lapangan Bulutangkis Manisrejo", msg))
                        onShowMessage("Pesan pengingat pembayaran disalin ke clipboard.")
                    }
                )
            }
        }

        // Notification & Reminder Timeline Log
        item {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Riwayat Notifikasi (${uiState.reminderLogs.size})",
                style = MaterialTheme.typography.titleLarge
            )
        }

        if (uiState.reminderLogs.isEmpty()) {
            item {
                Text(
                    text = "Notifikasi konfirmasi pemesanan dan pengingat pembayaran akan tampil di sini.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(uiState.reminderLogs.take(20), key = { "log_${it.id}" }) { log ->
                ReminderLogCard(log = log)
            }
        }

        item { Spacer(Modifier.height(72.dp)) }
    }

    // Record DP Dialog
    dpDialogBooking?.let { target ->
        val remaining = (target.totalPrice - target.paidAmount).coerceAtLeast(0L)
        var dpInput by remember(target.id) { mutableStateOf((remaining / 2).toString()) }
        AlertDialog(
            onDismissRequest = { dpDialogBooking = null },
            title = { Text("Catat Pembayaran DP") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "${target.customerName} • Sisa tagihan ${ScheduleFormatters.formatRupiah(remaining)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = dpInput,
                        onValueChange = { dpInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Nominal Dibayarkan (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = dpInput.toLongOrNull() ?: 0L
                        if (amount > 0L) onRecordDp(target.id, amount)
                        dpDialogBooking = null
                    }
                ) { Text("Simpan DP") }
            },
            dismissButton = {
                TextButton(onClick = { dpDialogBooking = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun SummaryMetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = 0.85f)
            )
            Text(
                text = value,
                style = MonoPriceStyle,
                color = contentColor
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PendingPaymentCard(
    booking: BookingEntity,
    onMarkPaid: () -> Unit,
    onRecordDpClick: () -> Unit,
    onSendReminderNow: () -> Unit,
    onCopyMessage: () -> Unit
) {
    val remaining = (booking.totalPrice - booking.paidAmount).coerceAtLeast(0L)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pending_payment_card_${booking.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = booking.customerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${booking.courtName} • ${ScheduleFormatters.dayName(booking.dayOfWeek)} ${ScheduleFormatters.formatHourRange(booking.startHour, booking.durationHours)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (booking.reminderSentCount > 0) {
                            "Notifikasi pengingat terkirim ${booking.reminderSentCount}x"
                        } else {
                            "Pengingat pembayaran aktif"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PaymentStatusPill(
                        status = booking.paymentStatus
                    )
                    Text(
                        text = ScheduleFormatters.formatRupiah(remaining),
                        style = MonoPriceStyle
                    )
                    if (booking.paidAmount > 0L) {
                        Text(
                            text = "DP: ${ScheduleFormatters.formatRupiah(booking.paidAmount)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onMarkPaid,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("pay_full_${booking.id}")
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Lunas")
                }

                OutlinedButton(
                    onClick = onRecordDpClick,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Catat DP")
                }

                OutlinedButton(
                    onClick = onSendReminderNow,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("notify_now_${booking.id}")
                ) {
                    Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Kirim Notifikasi")
                }

                OutlinedButton(
                    onClick = onCopyMessage,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Salin Pesan")
                }
            }
        }
    }
}

@Composable
private fun ReminderLogCard(log: ReminderLogEntity) {
    val (bg, fg, label) = when (log.status) {
        ReminderLogStatus.KONFIRMASI -> Triple(StatusAutoBg, StatusAutoFg, "Konfirmasi")
        ReminderLogStatus.TERKIRIM -> Triple(StatusPaidBg, StatusPaidFg, "Terkirim")
        ReminderLogStatus.LUNAS -> Triple(StatusPaidBg, StatusPaidFg, "Lunas")
        else -> Triple(StatusPendingBg, StatusPendingFg, "Terjadwal")
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "${log.customerName} • ${log.stageLabel}",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "${log.courtName} (${log.scheduleLabel}) • ${ScheduleFormatters.formatTimestamp(log.scheduledForMillis)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(color = bg, shape = RoundedCornerShape(8.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = fg,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
