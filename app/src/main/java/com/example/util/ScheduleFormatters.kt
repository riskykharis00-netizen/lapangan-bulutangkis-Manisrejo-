package com.example.util

import android.net.Uri
import com.example.data.local.BookingEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ScheduleFormatters {

    const val VENUE_PHONE = "082142272367"
    const val VENUE_WHATSAPP_INTL = "6282142272367"
    const val VENUE_MAPS_URL = "https://maps.app.goo.gl/xEdtA2gNiUx9pz9f9"
    const val DEFAULT_HOURLY_RATE = 15_000L
    const val OPEN_HOUR = 8
    const val CLOSE_HOUR = 24
    const val TOTAL_OPERATIONAL_HOURS = CLOSE_HOUR - OPEN_HOUR

    private val indonesiaLocale = Locale.Builder().setLanguage("id").setRegion("ID").build()

    val daysOfWeek: List<Pair<Int, String>> = listOf(
        1 to "Senin",
        2 to "Selasa",
        3 to "Rabu",
        4 to "Kamis",
        5 to "Jumat",
        6 to "Sabtu",
        7 to "Minggu"
    )

    val shortDayNames: Map<Int, String> = mapOf(
        1 to "Sen",
        2 to "Sel",
        3 to "Rab",
        4 to "Kam",
        5 to "Jum",
        6 to "Sab",
        7 to "Min"
    )

    fun dayName(dayOfWeek: Int): String =
        daysOfWeek.firstOrNull { it.first == dayOfWeek }?.second ?: "Senin"

    fun shortDayName(dayOfWeek: Int): String =
        shortDayNames[dayOfWeek] ?: "Sen"

    fun formatRupiah(amount: Long): String {
        val formatter = NumberFormat.getNumberInstance(indonesiaLocale)
        return "Rp ${formatter.format(amount)}"
    }

    fun formatHourRange(startHour: Int, durationHours: Int): String {
        val endHour = (startHour + durationHours).coerceAtMost(CLOSE_HOUR)
        return "${formatHour(startHour)} - ${formatHour(endHour)}"
    }

    fun formatHour(hour: Int): String {
        val normalized = if (hour == 24 || hour == 0) 0 else hour
        return String.format(Locale.US, "%02d:00", normalized)
    }

    fun parseManualHourInput(raw: String, treatZeroAsMidnight: Boolean = false): Int? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val normalized = trimmed.replace('.', ':')
        val parsedHour = if (normalized.contains(':')) {
            val parts = normalized.split(':')
            if (parts.size > 2) return null
            val hour = parts[0].toIntOrNull() ?: return null
            val minutePart = parts.getOrNull(1).orEmpty()
            if (minutePart.isNotEmpty()) {
                val minute = minutePart.toIntOrNull() ?: return null
                if (minute != 0) return null
            }
            hour
        } else {
            trimmed.toIntOrNull()
        } ?: return null

        return if (treatZeroAsMidnight && parsedHour == 0) CLOSE_HOUR else parsedHour
    }

    fun formatTimestamp(millis: Long): String {
        val sdf = SimpleDateFormat("dd MMM, HH:mm", indonesiaLocale)
        return sdf.format(Date(millis))
    }

    fun computeAvailableHourRanges(bookingsForCourtAndDay: List<BookingEntity>): List<Pair<Int, Int>> {
        val occupiedHours = BooleanArray(CLOSE_HOUR) // indices 8..23 represent 08:00..23:00
        bookingsForCourtAndDay.forEach { booking ->
            val end = (booking.startHour + booking.durationHours).coerceAtMost(CLOSE_HOUR)
            for (h in booking.startHour.coerceAtLeast(OPEN_HOUR) until end) {
                occupiedHours[h] = true
            }
        }
        val ranges = mutableListOf<Pair<Int, Int>>()
        var rangeStart: Int? = null
        for (h in OPEN_HOUR until CLOSE_HOUR) {
            if (!occupiedHours[h]) {
                if (rangeStart == null) rangeStart = h
            } else if (rangeStart != null) {
                ranges.add(rangeStart to h)
                rangeStart = null
            }
        }
        if (rangeStart != null) {
            ranges.add(rangeStart to CLOSE_HOUR)
        }
        return ranges
    }

    fun formatAvailableRangesSummary(bookingsForCourtAndDay: List<BookingEntity>): String {
        val ranges = computeAvailableHourRanges(bookingsForCourtAndDay)
        return if (ranges.isEmpty()) {
            "Penuh seharian"
        } else {
            ranges.joinToString(", ") { (start, end) ->
                "${formatHour(start)}-${formatHour(end)}"
            }
        }
    }

    fun buildWhatsAppUrl(message: String): String {
        val encoded = Uri.encode(message)
        return "https://wa.me/$VENUE_WHATSAPP_INTL?text=$encoded"
    }

    fun buildBookingConfirmWhatsAppMessage(
        customerName: String,
        scheduleSummary: String,
        totalPrice: Long
    ): String {
        return "Halo Admin Lapangan Bulutangkis Manisrejo ($VENUE_PHONE), saya *$customerName* ingin konfirmasi langsung pemesanan sewa lapangan:\n$scheduleSummary\nTotal biaya: ${formatRupiah(totalPrice)}\nMohon konfirmasinya. Terima kasih!"
    }

    fun buildPaymentReminderMessage(
        customerName: String,
        courtName: String,
        dayOfWeek: Int,
        startHour: Int,
        durationHours: Int,
        amountDue: Long
    ): String {
        val schedule = "${dayName(dayOfWeek)}, ${formatHourRange(startHour, durationHours)}"
        return "Halo $customerName, pengingat pembayaran sewa $courtName ($schedule) sebesar ${formatRupiah(amountDue)}. Konfirmasi WhatsApp ke $VENUE_PHONE. Lokasi: $VENUE_MAPS_URL. Terima kasih! - Lapangan Bulutangkis Manisrejo"
    }
}
