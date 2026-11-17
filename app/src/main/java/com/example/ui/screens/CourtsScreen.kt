package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.CourtEntity
import com.example.ui.ShuttleUiState
import com.example.ui.components.VenueContactMapsCard
import com.example.ui.theme.MonoPriceStyle
import com.example.ui.theme.MonoTimeStyle
import com.example.util.ScheduleFormatters

@Composable
fun CourtsScreen(
    uiState: ShuttleUiState,
    onSaveCourt: (CourtEntity) -> Unit,
    onToggleCourtActive: (CourtEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingCourt by remember { mutableStateOf<CourtEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val hoursPerDay = remember(uiState.bookings) {
        (1..7).map { day ->
            day to uiState.bookings.filter { it.dayOfWeek == day }.sumOf { it.durationHours }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            VenueContactMapsCard()
        }

        // Weekly Occupancy Canvas Chart (Senin - Minggu)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Okupansi Mingguan (Senin – Minggu)",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Total jam terpesan per hari di Lapangan A & B",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val maxHours = (hoursPerDay.maxOfOrNull { it.second } ?: 1).coerceAtLeast(4)
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val secondaryColor = MaterialTheme.colorScheme.secondary
                    val trackColor = MaterialTheme.colorScheme.surfaceVariant

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    ) {
                        val barCount = 7
                        val slotWidth = size.width / barCount
                        val barWidth = slotWidth * 0.52f
                        val maxBarHeight = size.height - 12f

                        hoursPerDay.forEachIndexed { index, (_, hours) ->
                            val x = index * slotWidth + (slotWidth - barWidth) / 2f
                            drawRoundRect(
                                color = trackColor,
                                topLeft = Offset(x, 6f),
                                size = Size(barWidth, maxBarHeight),
                                cornerRadius = CornerRadius(14f, 14f)
                            )
                            if (hours > 0) {
                                val ratio = (hours.toFloat() / maxHours.toFloat()).coerceIn(0.08f, 1f)
                                val barHeight = maxBarHeight * ratio
                                drawRoundRect(
                                    color = if (index >= 5) secondaryColor else primaryColor,
                                    topLeft = Offset(x, 6f + maxBarHeight - barHeight),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(14f, 14f)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        hoursPerDay.forEach { (dayInt, hrs) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = ScheduleFormatters.shortDayName(dayInt),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${hrs}j",
                                    style = MonoTimeStyle,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Courts Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Editor Website & Lapangan (${uiState.courts.size})",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Atur nama, tarif sewa per jam, dan status aktif lapangan",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("add_court_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Tambah")
                }
            }
        }

        items(uiState.courts, key = { it.id }) { court ->
            CourtManagementCard(
                court = court,
                onEditClick = { editingCourt = court },
                onToggleActive = { onToggleCourtActive(court) }
            )
        }

        item { Spacer(Modifier.height(72.dp)) }
    }

    if (showAddDialog) {
        val nextLetter = ('A'.code + uiState.courts.size).toChar()
        CourtEditDialog(
            initial = CourtEntity(
                name = if (uiState.courts.size < 26) "Lapangan $nextLetter" else "Lapangan ${uiState.courts.size + 1}",
                surfaceType = "",
                hourlyRateRegular = ScheduleFormatters.DEFAULT_HOURLY_RATE,
                hourlyRatePrime = ScheduleFormatters.DEFAULT_HOURLY_RATE,
                hourlyRateWeekend = ScheduleFormatters.DEFAULT_HOURLY_RATE,
                isActive = true
            ),
            title = "Tambah Lapangan Baru",
            onDismiss = { showAddDialog = false },
            onConfirm = { newCourt ->
                onSaveCourt(newCourt)
                showAddDialog = false
            }
        )
    }

    editingCourt?.let { target ->
        CourtEditDialog(
            initial = target,
            title = "Edit Tarif & Info Lapangan",
            onDismiss = { editingCourt = null },
            onConfirm = { updated ->
                onSaveCourt(updated)
                editingCourt = null
            }
        )
    }
}

@Composable
private fun CourtManagementCard(
    court: CourtEntity,
    onEditClick: () -> Unit,
    onToggleActive: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = if (court.isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SportsTennis,
                            contentDescription = null,
                            tint = if (court.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Column {
                        Text(
                            text = court.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (court.surfaceType.isNotBlank()) {
                                "${court.surfaceType} • ${if (court.isActive) "Aktif" else "Perawatan"}"
                            } else if (court.isActive) {
                                "Aktif"
                            } else {
                                "Perawatan"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit lapangan")
                    }
                    Switch(checked = court.isActive, onCheckedChange = { onToggleActive() })
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tarif Sewa per Jam",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${ScheduleFormatters.formatRupiah(court.hourlyRateRegular)} / jam",
                        style = MonoPriceStyle
                    )
                }
            }
        }
    }
}

@Composable
private fun CourtEditDialog(
    initial: CourtEntity,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (CourtEntity) -> Unit
) {
    var name by remember { mutableStateOf(initial.name) }
    var surface by remember { mutableStateOf(initial.surfaceType) }
    var rate by remember { mutableStateOf(initial.hourlyRateRegular.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Lapangan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = surface,
                    onValueChange = { surface = it },
                    label = { Text("Keterangan Lapangan (opsional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rate,
                    onValueChange = { rate = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Tarif per Jam (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedRate = rate.toLongOrNull() ?: initial.hourlyRateRegular
                    onConfirm(
                        initial.copy(
                            name = name.trim().ifEmpty { initial.name },
                            surfaceType = surface.trim(),
                            hourlyRateRegular = parsedRate,
                            hourlyRatePrime = parsedRate,
                            hourlyRateWeekend = parsedRate
                        )
                    )
                }
            ) { Text("Simpan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
