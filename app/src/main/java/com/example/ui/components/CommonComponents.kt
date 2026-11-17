package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.CourtEntity
import com.example.data.local.PaymentStatus
import com.example.ui.BookingConfirmationPrompt
import com.example.ui.BookingDraft
import com.example.ui.theme.MonoPriceStyle
import com.example.ui.theme.StatusPaidBg
import com.example.ui.theme.StatusPaidFg
import com.example.ui.theme.StatusPendingBg
import com.example.ui.theme.StatusPendingFg
import com.example.util.ScheduleFormatters

fun openWhatsAppConfirmation(context: Context, message: String) {
    val waIntent = Intent(Intent.ACTION_VIEW, Uri.parse(ScheduleFormatters.buildWhatsAppUrl(message)))
    runCatching { context.startActivity(waIntent) }.onFailure {
        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${ScheduleFormatters.VENUE_PHONE}"))
        context.startActivity(dialIntent)
    }
}

@Composable
fun BookingSuccessWhatsAppDialog(
    prompt: BookingConfirmationPrompt,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = StatusPaidFg,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text("Pemesanan Berhasil!")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = prompt.customerName,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = prompt.scheduleSummary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Total: ${ScheduleFormatters.formatRupiah(prompt.totalPrice)}",
                            style = MonoPriceStyle,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Text(
                    text = "Konfirmasi langsung dengan menghubungi WhatsApp pengelola di nomor ${ScheduleFormatters.VENUE_PHONE} agar jadwal segera diproses.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    openWhatsAppConfirmation(context, prompt.whatsAppMessage)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("confirm_whatsapp_dialog_button")
            ) {
                Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("WhatsApp ${ScheduleFormatters.VENUE_PHONE}")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Nanti Saja")
            }
        }
    )
}

@Composable
fun VenueContactMapsCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Lapangan Bulutangkis Manisrejo",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Konfirmasi cepat via WhatsApp & petunjuk arah",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "08:00–00:00",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        openWhatsAppConfirmation(
                            context,
                            "Halo Admin Lapangan Bulutangkis Manisrejo (${ScheduleFormatters.VENUE_PHONE}), saya ingin konfirmasi jadwal sewa lapangan."
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                    modifier = Modifier
                        .weight(1.25f)
                        .heightIn(min = 48.dp)
                        .testTag("venue_whatsapp_button")
                ) {
                    Icon(Icons.Filled.Chat, contentDescription = "WhatsApp pengelola", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "WA ${ScheduleFormatters.VENUE_PHONE}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = {
                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${ScheduleFormatters.VENUE_PHONE}"))
                        context.startActivity(dialIntent)
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("venue_phone_button")
                ) {
                    Icon(Icons.Filled.Call, contentDescription = "Telepon pengelola", modifier = Modifier.size(18.dp))
                }

                Button(
                    onClick = {
                        val mapsIntent = Intent(Intent.ACTION_VIEW, Uri.parse(ScheduleFormatters.VENUE_MAPS_URL))
                        context.startActivity(mapsIntent)
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(0.95f)
                        .heightIn(min = 48.dp)
                        .testTag("venue_maps_button")
                ) {
                    Icon(Icons.Filled.LocationOn, contentDescription = "Buka Google Maps", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Maps", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun DayOfWeekSelectorStrip(
    selectedDay: Int,
    bookingCounts: Map<Int, Int>,
    onSelectDay: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ScheduleFormatters.daysOfWeek.forEach { (dayInt, _) ->
            val isSelected = selectedDay == dayInt
            val count = bookingCounts[dayInt] ?: 0
            val containerColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                label = "day_chip_bg"
            )
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = containerColor,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                ),
                tonalElevation = if (isSelected) 4.dp else 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelectDay(dayInt) }
                    .testTag("day_chip_$dayInt")
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = ScheduleFormatters.shortDayName(dayInt),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(width = 22.dp, height = 16.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isSelected -> MaterialTheme.colorScheme.secondary
                                    count > 0 -> MaterialTheme.colorScheme.primaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.onSecondary
                                count > 0 -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentStatusPill(
    status: String
) {
    val (bg, fg, label) = when (status) {
        PaymentStatus.PAID -> Triple(StatusPaidBg, StatusPaidFg, "Lunas")
        PaymentStatus.DP -> Triple(StatusPendingBg, StatusPendingFg, "DP Sebagian")
        else -> Triple(StatusPendingBg, StatusPendingFg, "Belum Bayar")
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BookingFormBottomSheet(
    draft: BookingDraft,
    courts: List<CourtEntity>,
    isEditorMode: Boolean = false,
    calculatePrice: (List<CourtEntity>, Int, Int, Int) -> Long,
    onDismiss: () -> Unit,
    onConfirm: (
        courts: List<CourtEntity>,
        dayOfWeek: Int,
        startHour: Int,
        durationHours: Int,
        customerName: String,
        customerPhone: String,
        paymentStatus: String,
        paidAmount: Long,
        reminderEnabled: Boolean,
        notes: String
    ) -> Unit
) {
    val activeCourts = courts.filter { it.isActive }.ifEmpty { courts }
    val initialStart = draft.startHour.coerceIn(
        ScheduleFormatters.OPEN_HOUR,
        ScheduleFormatters.CLOSE_HOUR - 1
    )
    val initialEnd = (initialStart + 1).coerceAtMost(ScheduleFormatters.CLOSE_HOUR)
    val defaultCourt = activeCourts.firstOrNull { it.id == draft.courtId } ?: activeCourts.first()

    var selectedCourts by remember(draft, activeCourts) {
        mutableStateOf(listOf(defaultCourt))
    }
    var selectedDay by remember(draft) { mutableIntStateOf(draft.dayOfWeek) }
    var startHourText by remember(draft) { mutableStateOf(ScheduleFormatters.formatHour(initialStart)) }
    var endHourText by remember(draft) { mutableStateOf(ScheduleFormatters.formatHour(initialEnd)) }
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var paymentStatus by remember { mutableStateOf(PaymentStatus.UNPAID) }
    var dpAmountText by remember { mutableStateOf("10000") }
    var reminderEnabled by remember { mutableStateOf(true) }
    var notes by remember { mutableStateOf("") }

    val parsedStartHour = ScheduleFormatters.parseManualHourInput(startHourText, treatZeroAsMidnight = false)
    val parsedEndHour = ScheduleFormatters.parseManualHourInput(endHourText, treatZeroAsMidnight = true)
    val isStartValid = parsedStartHour != null &&
        parsedStartHour in ScheduleFormatters.OPEN_HOUR until ScheduleFormatters.CLOSE_HOUR
    val isEndValid = parsedEndHour != null &&
        parsedEndHour in (ScheduleFormatters.OPEN_HOUR + 1)..ScheduleFormatters.CLOSE_HOUR &&
        (parsedStartHour == null || parsedEndHour > parsedStartHour)
    val isTimeRangeValid = isStartValid && isEndValid
    val validStartHour = if (isTimeRangeValid) parsedStartHour!! else initialStart
    val durationHours = if (isTimeRangeValid) (parsedEndHour!! - parsedStartHour!!) else 1
    val totalPrice = calculatePrice(selectedCourts, selectedDay, validStartHour, durationHours)
    val twoCourtsOption = remember(activeCourts) { activeCourts.take(2) }
    val isTwoCourtsSelected = twoCourtsOption.size >= 2 &&
        selectedCourts.map { it.id }.toSet() == twoCourtsOption.map { it.id }.toSet()
    val selectedCourtsLabel = if (selectedCourts.size > 1) {
        "${selectedCourts.size} Lapangan (${selectedCourts.joinToString(" & ") { it.name }})"
    } else {
        selectedCourts.firstOrNull()?.name.orEmpty()
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Pemesanan Lapangan Baru",
                style = MaterialTheme.typography.headlineSmall
            )

            // Court Selector (1 Lapangan atau 2 Lapangan sekaligus)
            Text("Pilih Lapangan", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                activeCourts.forEach { court ->
                    val isSingleSelected = selectedCourts.size == 1 && selectedCourts.first().id == court.id
                    FilterChip(
                        selected = isSingleSelected,
                        onClick = { selectedCourts = listOf(court) },
                        label = {
                            Text("${court.name} (${ScheduleFormatters.formatRupiah(court.hourlyRateRegular)}/jam)")
                        },
                        modifier = Modifier.testTag("booking_court_${court.id}")
                    )
                }
                if (twoCourtsOption.size >= 2) {
                    FilterChip(
                        selected = isTwoCourtsSelected,
                        onClick = { selectedCourts = twoCourtsOption },
                        label = { Text("Pesan 2 Lapangan (A & B)") },
                        modifier = Modifier.testTag("booking_two_courts_chip")
                    )
                }
            }

            // Day of Week (Senin - Minggu)
            Text("Hari Sewa (Senin – Minggu)", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ScheduleFormatters.daysOfWeek.forEach { (dayNum, dayLabel) ->
                    FilterChip(
                        selected = selectedDay == dayNum,
                        onClick = { selectedDay = dayNum },
                        label = { Text(dayLabel) }
                    )
                }
            }

            // Manual Start Hour & End Hour typed by renter (08:00 - 00:00)
            Text("Jam Main (Diisi Manual: 08:00 – 00:00)", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = startHourText,
                    onValueChange = { raw ->
                        startHourText = raw.filter { ch -> ch.isDigit() || ch == ':' || ch == '.' }.take(5)
                    },
                    label = { Text("Jam Mulai") },
                    placeholder = { Text("08:00") },
                    isError = startHourText.isNotBlank() && !isStartValid,
                    supportingText = {
                        Text(
                            if (startHourText.isNotBlank() && !isStartValid) {
                                "Isi 08:00 – 23:00"
                            } else {
                                "Contoh: 08:00 atau 23"
                            }
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("booking_start_hour_input")
                )

                OutlinedTextField(
                    value = endHourText,
                    onValueChange = { raw ->
                        endHourText = raw.filter { ch -> ch.isDigit() || ch == ':' || ch == '.' }.take(5)
                    },
                    label = { Text("Jam Selesai") },
                    placeholder = { Text("00:00") },
                    isError = endHourText.isNotBlank() && !isEndValid,
                    supportingText = {
                        Text(
                            if (endHourText.isNotBlank() && !isEndValid) {
                                "Harus > mulai, maks 00:00"
                            } else {
                                "Contoh: 23:00 atau 00:00"
                            }
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("booking_end_hour_input")
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isTimeRangeValid) {
                                "$selectedCourtsLabel • ${ScheduleFormatters.dayName(selectedDay)}, ${ScheduleFormatters.formatHourRange(validStartHour, durationHours)}"
                            } else {
                                "Jam belum valid (08:00 – 00:00)"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = if (isTimeRangeValid) {
                                "${selectedCourts.size} lapangan x $durationHours jam"
                            } else {
                                "Isi jam mulai (08–23) dan jam selesai (09–00:00)"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                    Text(
                        text = if (isTimeRangeValid) ScheduleFormatters.formatRupiah(totalPrice) else "-",
                        style = MonoPriceStyle,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            OutlinedTextField(
                value = customerName,
                onValueChange = { customerName = it },
                label = { Text("Nama Penyewa / Klub *") },
                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("booking_customer_name")
            )

            OutlinedTextField(
                value = customerPhone,
                onValueChange = { customerPhone = it },
                label = { Text("No. HP / WhatsApp Penyewa") },
                leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("booking_customer_phone")
            )

            if (isEditorMode) {
                Text("Status Pembayaran (Editor)", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        PaymentStatus.UNPAID to "Belum Bayar",
                        PaymentStatus.DP to "DP",
                        PaymentStatus.PAID to "Lunas"
                    ).forEach { (code, label) ->
                        FilterChip(
                            selected = paymentStatus == code,
                            onClick = { paymentStatus = code },
                            label = { Text(label) }
                        )
                    }
                }

                if (paymentStatus == PaymentStatus.DP) {
                    OutlinedTextField(
                        value = dpAmountText,
                        onValueChange = { dpAmountText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Nominal DP (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (paymentStatus != PaymentStatus.PAID) {
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text("Notifikasi Pengingat Pembayaran", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "Kirim konfirmasi & jadwalkan pengingat bayar",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { reminderEnabled = it },
                            modifier = Modifier.testTag("booking_reminder_switch")
                        )
                    }
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Setelah konfirmasi jadwal, silakan lanjutkan pembayaran & konfirmasi ke WhatsApp Pengelola (${ScheduleFormatters.VENUE_PHONE}).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Catatan Tambahan (opsional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text("Batal")
                }
                Button(
                    onClick = {
                        val dp = dpAmountText.toLongOrNull() ?: 0L
                        onConfirm(
                            selectedCourts,
                            selectedDay,
                            validStartHour,
                            durationHours,
                            customerName,
                            customerPhone,
                            paymentStatus,
                            dp,
                            reminderEnabled,
                            notes
                        )
                    },
                    enabled = selectedCourts.isNotEmpty() && customerName.trim().isNotEmpty() && isTimeRangeValid,
                    modifier = Modifier
                        .weight(1.4f)
                        .heightIn(min = 48.dp)
                        .testTag("confirm_booking_button")
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Konfirmasi Jadwal", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun WebsiteFooterCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF062E22),
        modifier = modifier
            .fillMaxWidth()
            .testTag("website_footer")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Lapangan Bulutangkis Manisrejo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Buka Setiap Hari Senin – Minggu • 08:00 – 00:00 • Rp 15.000/jam",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        openWhatsAppConfirmation(
                            context,
                            "Halo Admin Lapangan Bulutangkis Manisrejo (${ScheduleFormatters.VENUE_PHONE}), saya ingin info jadwal lapangan."
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Filled.Chat, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("WhatsApp", maxLines = 1)
                }

                OutlinedButton(
                    onClick = {
                        val mapsIntent = Intent(Intent.ACTION_VIEW, Uri.parse(ScheduleFormatters.VENUE_MAPS_URL))
                        context.startActivity(mapsIntent)
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color(0xFFA6E22E), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Google Maps", maxLines = 1)
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.16f))

            Text(
                text = "© 2026 Lapangan Bulutangkis Manisrejo • WA ${ScheduleFormatters.VENUE_PHONE}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.68f)
            )
        }
    }
}
