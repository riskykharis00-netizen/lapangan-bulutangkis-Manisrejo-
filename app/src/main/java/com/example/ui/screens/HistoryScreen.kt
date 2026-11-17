package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.BookingHistoryEntity
import com.example.data.local.BookingHistoryStatus
import com.example.ui.ShuttleUiState
import com.example.ui.components.PaymentStatusPill
import com.example.ui.components.WebsiteFooterCard
import com.example.ui.components.openWhatsAppConfirmation
import com.example.ui.theme.MonoPriceStyle
import com.example.ui.theme.MonoTimeStyle
import com.example.ui.theme.StatusOverdueBg
import com.example.ui.theme.StatusOverdueFg
import com.example.ui.theme.StatusPaidBg
import com.example.ui.theme.StatusPaidFg
import com.example.util.ScheduleFormatters

private enum class HistoryStatusFilter {
    ALL,
    AKTIF,
    DIBATALKAN
}

@Composable
fun HistoryScreen(
    uiState: ShuttleUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedCourtFilter by remember { mutableStateOf("SEMUA") }
    var statusFilter by remember { mutableStateOf(HistoryStatusFilter.ALL) }

    val courtFilterOptions = remember(uiState.courts, uiState.bookingHistory) {
        val fromCourts = uiState.courts.map { it.name }
        val fromHistory = uiState.bookingHistory.map { it.courtName }
        (listOf("Lapangan A", "Lapangan B") + fromCourts + fromHistory).distinct()
    }

    val fieldAEntries = remember(uiState.bookingHistory) {
        uiState.bookingHistory.filter { it.courtName.equals("Lapangan A", ignoreCase = true) }
    }
    val fieldBEntries = remember(uiState.bookingHistory) {
        uiState.bookingHistory.filter { it.courtName.equals("Lapangan B", ignoreCase = true) }
    }

    val filteredHistory = remember(uiState.bookingHistory, selectedCourtFilter, statusFilter) {
        uiState.bookingHistory.filter { entry ->
            val courtMatch = selectedCourtFilter == "SEMUA" ||
                entry.courtName.equals(selectedCourtFilter, ignoreCase = true)
            val statusMatch = when (statusFilter) {
                HistoryStatusFilter.ALL -> true
                HistoryStatusFilter.AKTIF -> entry.bookingStatus == BookingHistoryStatus.AKTIF
                HistoryStatusFilter.DIBATALKAN -> entry.bookingStatus == BookingHistoryStatus.DIBATALKAN
            }
            courtMatch && statusMatch
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("history_screen_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Column {
                        Text(
                            text = "Lapangan Bulutangkis Manisrejo",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Catatan transaksi Lapangan A & Lapangan B (${uiState.bookingHistory.size} riwayat)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        // Summary Header for Lapangan A & Lapangan B
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HistoryCourtSummaryCard(
                    title = "Lapangan A",
                    entries = fieldAEntries,
                    isSelected = selectedCourtFilter.equals("Lapangan A", ignoreCase = true),
                    onClick = {
                        selectedCourtFilter = if (selectedCourtFilter.equals("Lapangan A", ignoreCase = true)) {
                            "SEMUA"
                        } else {
                            "Lapangan A"
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("history_summary_court_a")
                )
                HistoryCourtSummaryCard(
                    title = "Lapangan B",
                    entries = fieldBEntries,
                    isSelected = selectedCourtFilter.equals("Lapangan B", ignoreCase = true),
                    onClick = {
                        selectedCourtFilter = if (selectedCourtFilter.equals("Lapangan B", ignoreCase = true)) {
                            "SEMUA"
                        } else {
                            "Lapangan B"
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("history_summary_court_b")
                )
            }
        }

        // Court Filter Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCourtFilter == "SEMUA",
                        onClick = { selectedCourtFilter = "SEMUA" },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        label = { Text("Semua Lapangan (${uiState.bookingHistory.size})") },
                        modifier = Modifier.testTag("history_filter_all")
                    )
                    courtFilterOptions.forEach { courtName ->
                        val count = uiState.bookingHistory.count { it.courtName.equals(courtName, ignoreCase = true) }
                        FilterChip(
                            selected = selectedCourtFilter.equals(courtName, ignoreCase = true),
                            onClick = { selectedCourtFilter = courtName },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.SportsTennis,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            label = { Text("$courtName ($count)") },
                            modifier = Modifier.testTag("history_filter_${courtName.lowercase().replace(" ", "_")}")
                        )
                    }
                }

                // Booking Status Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        HistoryStatusFilter.ALL to "Semua Status",
                        HistoryStatusFilter.AKTIF to "Aktif",
                        HistoryStatusFilter.DIBATALKAN to "Dibatalkan"
                    ).forEach { (filter, label) ->
                        FilterChip(
                            selected = statusFilter == filter,
                            onClick = { statusFilter = filter },
                            label = { Text(label) },
                            modifier = Modifier.testTag("history_status_${filter.name.lowercase()}")
                        )
                    }
                }
            }
        }

        if (filteredHistory.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_empty_card")
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
                            text = "Belum ada riwayat pemesanan",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Setiap pemesanan untuk Lapangan A maupun Lapangan B akan tercatat otomatis di sini.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredHistory, key = { it.id }) { entry ->
                BookingHistoryCard(
                    entry = entry,
                    onWhatsAppConfirm = {
                        val summary = "${entry.courtName} • ${ScheduleFormatters.dayName(entry.dayOfWeek)}, ${ScheduleFormatters.formatHourRange(entry.startHour, entry.durationHours)}"
                        val msg = ScheduleFormatters.buildBookingConfirmWhatsAppMessage(
                            customerName = entry.customerName,
                            scheduleSummary = summary,
                            totalPrice = entry.totalPrice
                        )
                        openWhatsAppConfirmation(context, msg)
                    }
                )
            }
        }

        item { WebsiteFooterCard() }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun HistoryCourtSummaryCard(
    title: String,
    entries: List<BookingHistoryEntity>,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeEntries = entries.filter { it.bookingStatus == BookingHistoryStatus.AKTIF }
    val totalValue = activeEntries.sumOf { it.totalPrice }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
            }
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${entries.size} riwayat (${activeEntries.size} aktif)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = ScheduleFormatters.formatRupiah(totalValue),
                style = MonoPriceStyle,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun BookingHistoryCard(
    entry: BookingHistoryEntity,
    onWhatsAppConfirm: () -> Unit
) {
    val isCancelled = entry.bookingStatus == BookingHistoryStatus.DIBATALKAN

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = if (isCancelled) {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_card_${entry.id}")
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
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = if (isCancelled) {
                                MaterialTheme.colorScheme.surfaceVariant
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = entry.courtName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isCancelled) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onPrimary
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            color = if (isCancelled) StatusOverdueBg else StatusPaidBg,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isCancelled) "Dibatalkan" else "Aktif",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isCancelled) StatusOverdueFg else StatusPaidFg,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = entry.customerName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "${ScheduleFormatters.dayName(entry.dayOfWeek)}, ${ScheduleFormatters.formatHourRange(entry.startHour, entry.durationHours)} (${entry.durationHours} jam)",
                        style = MonoTimeStyle,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (entry.customerPhone.isNotBlank()) {
                        Text(
                            text = "No. HP: ${entry.customerPhone}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "Dicatat: ${ScheduleFormatters.formatTimestamp(entry.createdAtMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PaymentStatusPill(status = entry.paymentStatus)
                    Text(
                        text = ScheduleFormatters.formatRupiah(entry.totalPrice),
                        style = MonoPriceStyle
                    )
                }
            }

            if (entry.notes.isNotBlank()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                Text(
                    text = "Catatan: ${entry.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!isCancelled) {
                OutlinedButton(
                    onClick = onWhatsAppConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("history_whatsapp_${entry.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Chat,
                        contentDescription = null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Hubungi WhatsApp ${ScheduleFormatters.VENUE_PHONE}")
                }
            }
        }
    }
}
