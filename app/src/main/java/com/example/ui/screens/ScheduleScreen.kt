package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CourtEmerald
import com.example.ui.theme.CourtEmeraldDeep
import com.example.ui.theme.CourtGoldAccent
import com.example.ui.theme.ShuttleLimeBright
import com.example.R
import com.example.data.local.BookingEntity
import com.example.data.local.CourtEntity
import com.example.data.local.PaymentStatus
import com.example.ui.ShuttleUiState
import com.example.ui.SlotFilter
import com.example.ui.UserRole
import com.example.ui.components.DayOfWeekSelectorStrip
import com.example.ui.components.PaymentStatusPill
import com.example.ui.components.VenueContactMapsCard
import com.example.ui.components.WebsiteFooterCard
import com.example.ui.components.openWhatsAppConfirmation
import com.example.ui.theme.MonoPriceStyle
import com.example.ui.theme.MonoTimeStyle
import com.example.ui.theme.StatusOverdueBg
import com.example.ui.theme.StatusOverdueFg
import com.example.ui.theme.StatusPaidBg
import com.example.ui.theme.StatusPaidFg
import com.example.util.ScheduleFormatters

private enum class ScheduleViewMode {
    DAILY,
    WEEKLY_OVERVIEW
}

private sealed class ScheduleRow {
    data class Booked(val booking: BookingEntity) : ScheduleRow()
    data class Available(val court: CourtEntity, val hour: Int, val hourlyRate: Long) : ScheduleRow()
}

@Composable
fun ScheduleScreen(
    uiState: ShuttleUiState,
    onSelectDay: (Int) -> Unit,
    onSelectCourtFilter: (Int) -> Unit,
    onSelectSlotFilter: (SlotFilter) -> Unit,
    onSlotBookClick: (courtId: Int, dayOfWeek: Int, startHour: Int) -> Unit,
    onMarkPaid: (Long) -> Unit,
    onSendReminder: (Long) -> Unit,
    onCancelBooking: (Long) -> Unit,
    onQuickBookClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bookingToCancel by remember { mutableStateOf<BookingEntity?>(null) }
    var viewMode by remember { mutableStateOf(ScheduleViewMode.DAILY) }
    val isEditorMode = uiState.userRole == UserRole.EDITOR

    val bookingCountsByDay = remember(uiState.bookings, uiState.selectedCourtId) {
        (1..7).associateWith { day -> uiState.bookingsCountForDay(day) }
    }

    val visibleCourts = remember(uiState.courts, uiState.selectedCourtId) {
        val active = uiState.courts.filter { it.isActive }
        if (uiState.selectedCourtId == 0) active else active.filter { it.id == uiState.selectedCourtId }
    }

    val dayBookings = remember(uiState.bookings, uiState.selectedDay, uiState.selectedCourtId) {
        uiState.bookings.filter {
            it.dayOfWeek == uiState.selectedDay &&
                (uiState.selectedCourtId == 0 || it.courtId == uiState.selectedCourtId)
        }
    }

    val scheduleRows = remember(visibleCourts, dayBookings, uiState.selectedDay, uiState.slotFilter) {
        val rows = mutableListOf<ScheduleRow>()
        val renderedBookingIds = mutableSetOf<Long>()
        for (hour in ScheduleFormatters.OPEN_HOUR until ScheduleFormatters.CLOSE_HOUR) {
            for (court in visibleCourts) {
                val coveringBooking = dayBookings.firstOrNull { b ->
                    b.courtId == court.id && hour >= b.startHour && hour < (b.startHour + b.durationHours)
                }
                if (coveringBooking != null) {
                    if (uiState.slotFilter != SlotFilter.AVAILABLE && renderedBookingIds.add(coveringBooking.id)) {
                        rows.add(ScheduleRow.Booked(coveringBooking))
                    }
                } else if (uiState.slotFilter != SlotFilter.BOOKED) {
                    val rate = when {
                        uiState.selectedDay >= 6 -> court.hourlyRateWeekend
                        hour >= 17 -> court.hourlyRatePrime
                        else -> court.hourlyRateRegular
                    }
                    rows.add(ScheduleRow.Available(court, hour, rate))
                }
            }
        }
        rows
    }

    val dayBookedHours = dayBookings.sumOf { it.durationHours }
    val dayTotalCapacity = (visibleCourts.size * ScheduleFormatters.TOTAL_OPERATIONAL_HOURS).coerceAtLeast(1)
    val dayUnbookedHours = (dayTotalCapacity - dayBookedHours).coerceAtLeast(0)
    val dayPendingCount = dayBookings.count { it.paymentStatus != PaymentStatus.PAID }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Aesthetic Hero Showcase Card — Lapangan Bulutangkis Manisrejo
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 206.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 206.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_court_banner),
                        contentDescription = stringResource(R.string.cd_court_banner),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        CourtEmeraldDeep.copy(alpha = 0.94f),
                                        CourtEmerald.copy(alpha = 0.84f),
                                        Color(0x88064E38)
                                    )
                                )
                            )
                            .drawBehind {
                                val stroke = Stroke(width = 1.6.dp.toPx())
                                val lineColor = Color.White.copy(alpha = 0.12f)
                                val courtW = size.width * 0.42f
                                val courtH = size.height * 0.72f
                                val left = size.width - courtW - 14.dp.toPx()
                                val top = (size.height - courtH) / 2f
                                drawRoundRect(
                                    color = lineColor,
                                    topLeft = Offset(left, top),
                                    size = Size(courtW, courtH),
                                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                                    style = stroke
                                )
                                drawLine(
                                    color = ShuttleLimeBright.copy(alpha = 0.24f),
                                    start = Offset(left, size.height / 2f),
                                    end = Offset(left + courtW, size.height / 2f),
                                    strokeWidth = 2.dp.toPx()
                                )
                                drawLine(
                                    color = lineColor,
                                    start = Offset(left + courtW / 2f, top),
                                    end = Offset(left + courtW / 2f, top + courtH),
                                    strokeWidth = 1.2.dp.toPx()
                                )
                            }
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color.White.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(ShuttleLimeBright)
                                    )
                                    Text(
                                        text = if (isEditorMode) {
                                            "MODE EDITOR WEBSITE • LAPANGAN A & B"
                                        } else {
                                            "PORTAL PENYEWA • LAPANGAN A & B"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.secondary,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "$dayBookedHours/$dayTotalCapacity jam",
                                    style = MonoTimeStyle,
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "LAPANGAN BULUTANGKIS",
                                style = MaterialTheme.typography.labelMedium,
                                color = ShuttleLimeBright,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.4.sp
                            )
                            Text(
                                text = "Manisrejo",
                                style = MaterialTheme.typography.displaySmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .width(56.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(ShuttleLimeBright, CourtGoldAccent)
                                        )
                                    )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Senin – Minggu  •  08:00 – 00:00  •  Rp 15.000/jam",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.90f)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondary,
                                shape = RoundedCornerShape(14.dp),
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onQuickBookClick() }
                                    .testTag("hero_quick_book_cta")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Pesan Lapangan",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondary
                                    )
                                }
                            }

                            if (isEditorMode && dayPendingCount > 0) {
                                Surface(
                                    color = CourtGoldAccent,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "$dayPendingCount belum lunas",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1F1300),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Venue Contact WhatsApp & Google Maps Card
        item {
            VenueContactMapsCard()
        }

        // View Mode Switcher: Jadwal Lapangan | Ringkasan Sen-Min
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = viewMode == ScheduleViewMode.DAILY,
                    onClick = { viewMode = ScheduleViewMode.DAILY },
                    leadingIcon = { Icon(Icons.Filled.Today, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    label = { Text("Jadwal Lapangan") },
                    modifier = Modifier.testTag("mode_daily")
                )
                FilterChip(
                    selected = viewMode == ScheduleViewMode.WEEKLY_OVERVIEW,
                    onClick = { viewMode = ScheduleViewMode.WEEKLY_OVERVIEW },
                    leadingIcon = { Icon(Icons.Filled.CalendarViewWeek, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    label = { Text("Ringkasan Sen–Min") },
                    modifier = Modifier.testTag("mode_weekly_overview")
                )
            }
        }

        if (viewMode == ScheduleViewMode.WEEKLY_OVERVIEW) {
            // Monday - Sunday Full Overview of Booked vs Unbooked Days & Hours
            items(ScheduleFormatters.daysOfWeek, key = { "week_day_${it.first}" }) { (dayInt, dayLabel) ->
                WeeklyDayBookedUnbookedCard(
                    dayOfWeek = dayInt,
                    dayName = dayLabel,
                    courts = uiState.courts.filter { it.isActive },
                    bookingsForDay = uiState.bookings.filter { it.dayOfWeek == dayInt }.sortedBy { it.startHour },
                    onOpenDayClick = {
                        onSelectDay(dayInt)
                        viewMode = ScheduleViewMode.DAILY
                    }
                )
            }
        } else {
            // Monday - Sunday Strip
            item {
                DayOfWeekSelectorStrip(
                    selectedDay = uiState.selectedDay,
                    bookingCounts = bookingCountsByDay,
                    onSelectDay = onSelectDay
                )
            }

            // Booked vs Unbooked Summary Strip for Selected Day
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = StatusOverdueBg,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, StatusOverdueFg.copy(alpha = 0.22f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(StatusOverdueFg.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.EventBusy,
                                    contentDescription = null,
                                    tint = StatusOverdueFg,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Sudah Dipesan",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StatusOverdueFg.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = "$dayBookedHours jam",
                                    style = MonoTimeStyle,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusOverdueFg
                                )
                            }
                        }
                    }
                    Surface(
                        color = StatusPaidBg,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, StatusPaidFg.copy(alpha = 0.22f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(StatusPaidFg.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.EventAvailable,
                                    contentDescription = null,
                                    tint = StatusPaidFg,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Tersedia",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StatusPaidFg.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = "$dayUnbookedHours jam",
                                    style = MonoTimeStyle,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusPaidFg
                                )
                            }
                        }
                    }
                }
            }

            // Court & Slot Filters
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.selectedCourtId == 0,
                            onClick = { onSelectCourtFilter(0) },
                            label = { Text("Semua Lapangan") },
                            modifier = Modifier.testTag("filter_court_all")
                        )
                        uiState.courts.forEach { court ->
                            FilterChip(
                                selected = uiState.selectedCourtId == court.id,
                                onClick = { onSelectCourtFilter(court.id) },
                                label = {
                                    Text(if (court.isActive) court.name else "${court.name} (Nonaktif)")
                                },
                                modifier = Modifier.testTag("filter_court_${court.id}")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            SlotFilter.ALL to "Semua",
                            SlotFilter.AVAILABLE to "Tersedia",
                            SlotFilter.BOOKED to "Sudah Dipesan (${dayBookings.size})"
                        ).forEach { (filter, label) ->
                            FilterChip(
                                selected = uiState.slotFilter == filter,
                                onClick = { onSelectSlotFilter(filter) },
                                label = { Text(label) },
                                modifier = Modifier.testTag("filter_slot_${filter.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            if (scheduleRows.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_empty_schedule),
                                contentDescription = stringResource(R.string.cd_empty_schedule),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(width = 200.dp, height = 140.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                            Text(
                                text = if (uiState.slotFilter == SlotFilter.BOOKED) {
                                    "Belum ada jadwal yang sudah dipesan pada hari ${ScheduleFormatters.dayName(uiState.selectedDay)}"
                                } else {
                                    "Tidak ada slot yang cocok dengan filter"
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Ketuk tombol di bawah untuk mengisi jam sewa manual atau melihat slot tersedia.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = { onSelectSlotFilter(SlotFilter.AVAILABLE) },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Text("Lihat Tersedia")
                                }
                                Button(
                                    onClick = onQuickBookClick,
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Text("Pesan Lapangan")
                                }
                            }
                        }
                    }
                }
            } else {
                items(
                    items = scheduleRows,
                    key = { row ->
                        when (row) {
                            is ScheduleRow.Booked -> "booked_${row.booking.id}"
                            is ScheduleRow.Available -> "avail_${row.court.id}_${row.hour}"
                        }
                    }
                ) { row ->
                    when (row) {
                        is ScheduleRow.Booked -> {
                            BookedSlotCard(
                                booking = row.booking,
                                isEditorMode = isEditorMode,
                                onMarkPaid = { onMarkPaid(row.booking.id) },
                                onSendReminder = { onSendReminder(row.booking.id) },
                                onWhatsAppConfirm = {
                                    val summary = "${row.booking.courtName} • ${ScheduleFormatters.dayName(row.booking.dayOfWeek)}, ${ScheduleFormatters.formatHourRange(row.booking.startHour, row.booking.durationHours)}"
                                    val msg = ScheduleFormatters.buildBookingConfirmWhatsAppMessage(
                                        customerName = row.booking.customerName,
                                        scheduleSummary = summary,
                                        totalPrice = row.booking.totalPrice
                                    )
                                    openWhatsAppConfirmation(context, msg)
                                },
                                onCancelClick = { bookingToCancel = row.booking }
                            )
                        }
                        is ScheduleRow.Available -> {
                            AvailableSlotRow(
                                court = row.court,
                                hour = row.hour,
                                hourlyRate = row.hourlyRate,
                                onBookClick = { onSlotBookClick(row.court.id, uiState.selectedDay, row.hour) }
                            )
                        }
                    }
                }
            }
        }

        item { WebsiteFooterCard() }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    bookingToCancel?.let { target ->
        AlertDialog(
            onDismissRequest = { bookingToCancel = null },
            title = { Text("Batalkan Pemesanan?") },
            text = {
                Text(
                    "Jadwal ${target.customerName} di ${target.courtName} " +
                        "(${ScheduleFormatters.dayName(target.dayOfWeek)}, " +
                        "${ScheduleFormatters.formatHourRange(target.startHour, target.durationHours)}) " +
                        "akan dibatalkan dan slot dibuka kembali."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCancelBooking(target.id)
                        bookingToCancel = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Batalkan Jadwal")
                }
            },
            dismissButton = {
                TextButton(onClick = { bookingToCancel = null }) {
                    Text("Tutup")
                }
            }
        )
    }
}

@Composable
private fun WeeklyDayBookedUnbookedCard(
    dayOfWeek: Int,
    dayName: String,
    courts: List<CourtEntity>,
    bookingsForDay: List<BookingEntity>,
    onOpenDayClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weekly_overview_day_$dayOfWeek")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(
                    onClick = onOpenDayClick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.heightIn(min = 40.dp)
                ) {
                    Text("Lihat / Pesan")
                }
            }

            // Sudah Di-Booking section
            Surface(
                color = if (bookingsForDay.isEmpty()) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f) else StatusOverdueBg.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Sudah Dipesan (${bookingsForDay.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (bookingsForDay.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else StatusOverdueFg
                    )
                    if (bookingsForDay.isEmpty()) {
                        Text(
                            text = "Belum ada jadwal yang dipesan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        bookingsForDay.forEach { b ->
                            Text(
                                text = "• ${b.courtName} (${ScheduleFormatters.formatHourRange(b.startHour, b.durationHours)}) - ${b.customerName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            // Tersedia section per court
            Surface(
                color = StatusPaidBg.copy(alpha = 0.65f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Tersedia (Jam Kosong)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = StatusPaidFg
                    )
                    courts.forEach { court ->
                        val courtBookings = bookingsForDay.filter { it.courtId == court.id }
                        val summary = ScheduleFormatters.formatAvailableRangesSummary(courtBookings)
                        Text(
                            text = "• ${court.name}: $summary",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AvailableSlotRow(
    court: CourtEntity,
    hour: Int,
    hourlyRate: Long,
    onBookClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onBookClick)
            .testTag("slot_available_${court.id}_$hour")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = ScheduleFormatters.formatHourRange(hour, 1),
                        style = MonoTimeStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Column {
                    Text(
                        text = court.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Tersedia • Isi jam manual",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StatusPaidFg
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = ScheduleFormatters.formatRupiah(hourlyRate),
                    style = MonoPriceStyle,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.cd_add_booking),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(6.dp).size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BookedSlotCard(
    booking: BookingEntity,
    isEditorMode: Boolean,
    onMarkPaid: () -> Unit,
    onSendReminder: () -> Unit,
    onWhatsAppConfirm: () -> Unit,
    onCancelClick: () -> Unit
) {
    val isUnpaid = booking.paymentStatus != PaymentStatus.PAID
    val remaining = (booking.totalPrice - booking.paidAmount).coerceAtLeast(0L)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.5.dp,
            color = if (isUnpaid) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("slot_booked_${booking.id}")
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = ScheduleFormatters.formatHourRange(booking.startHour, booking.durationHours),
                                style = MonoTimeStyle,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = "${booking.courtName} • Sudah Dipesan",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = booking.customerName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isEditorMode && booking.customerPhone.isNotBlank()) {
                        Text(
                            text = booking.customerPhone,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isEditorMode) {
                        PaymentStatusPill(
                            status = booking.paymentStatus
                        )
                        Text(
                            text = ScheduleFormatters.formatRupiah(booking.totalPrice),
                            style = MonoPriceStyle
                        )
                    } else {
                        Surface(
                            color = StatusOverdueBg,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Terbooking",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = StatusOverdueFg,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = isEditorMode && isUnpaid) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sisa tagihan: ${ScheduleFormatters.formatRupiah(remaining)}",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = if (booking.reminderSentCount > 0) {
                                    "Pengingat terkirim ${booking.reminderSentCount}x"
                                } else {
                                    "Ketuk lonceng untuk kirim pengingat bayar"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = onSendReminder,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("send_reminder_${booking.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.NotificationsActive,
                                    contentDescription = stringResource(R.string.cd_send_reminder),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = onMarkPaid,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("mark_paid_${booking.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = stringResource(R.string.cd_mark_paid),
                                    tint = Color(0xFF15803D)
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onWhatsAppConfirm,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("whatsapp_confirm_${booking.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Chat,
                        contentDescription = null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Konfirmasi WA")
                }

                if (isEditorMode) {
                    TextButton(
                        onClick = onCancelClick,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("cancel_booking_${booking.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Batalkan", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
