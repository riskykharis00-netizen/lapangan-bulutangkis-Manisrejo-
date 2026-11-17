package com.example

import com.example.data.local.BookingEntity
import com.example.util.ScheduleFormatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CourtScheduleLogicTest {

    @Test
    fun `default rate is 15000 and reminder message contains phone and maps link`() {
        assertEquals(15_000L, ScheduleFormatters.DEFAULT_HOURLY_RATE)
        assertEquals("082142272367", ScheduleFormatters.VENUE_PHONE)
        assertEquals("https://maps.app.goo.gl/xEdtA2gNiUx9pz9f9", ScheduleFormatters.VENUE_MAPS_URL)

        val msg = ScheduleFormatters.buildPaymentReminderMessage(
            customerName = "PB Garuda",
            courtName = "Lapangan A",
            dayOfWeek = 3,
            startHour = 19,
            durationHours = 2,
            amountDue = 30_000L
        )

        assertTrue(msg.contains("Lapangan A"))
        assertTrue(msg.contains("082142272367"))
        assertTrue(msg.contains("https://maps.app.goo.gl/xEdtA2gNiUx9pz9f9"))
    }

    @Test
    fun `computeAvailableHourRanges splits booked and unbooked intervals accurately`() {
        val bookings = listOf(
            BookingEntity(
                id = 1L,
                courtId = 1,
                courtName = "Lapangan A",
                dayOfWeek = 1,
                startHour = 10,
                durationHours = 2,
                customerName = "Andi",
                customerPhone = "081234",
                totalPrice = 30_000L
            ),
            BookingEntity(
                id = 2L,
                courtId = 1,
                courtName = "Lapangan A",
                dayOfWeek = 1,
                startHour = 19,
                durationHours = 2,
                customerName = "Budi",
                customerPhone = "085678",
                totalPrice = 30_000L
            )
        )

        val summary = ScheduleFormatters.formatAvailableRangesSummary(bookings)
        assertEquals("08:00-10:00, 12:00-19:00, 21:00-00:00", summary)
    }

    @Test
    fun `parseManualHourInput accepts hour numbers and midnight 00_00 within 08 to 00`() {
        assertEquals(8, ScheduleFormatters.parseManualHourInput("08:00"))
        assertEquals(8, ScheduleFormatters.parseManualHourInput("8"))
        assertEquals(19, ScheduleFormatters.parseManualHourInput("19.00"))
        assertEquals(23, ScheduleFormatters.parseManualHourInput("23:00"))
        assertEquals(23, ScheduleFormatters.parseManualHourInput("23"))
        assertEquals(24, ScheduleFormatters.parseManualHourInput("00:00", treatZeroAsMidnight = true))
        assertEquals(24, ScheduleFormatters.parseManualHourInput("00", treatZeroAsMidnight = true))
        assertEquals("00:00", ScheduleFormatters.formatHour(24))
        assertEquals("22:00 - 00:00", ScheduleFormatters.formatHourRange(22, 2))
        assertEquals(null, ScheduleFormatters.parseManualHourInput("08:30"))
        assertEquals(null, ScheduleFormatters.parseManualHourInput(""))
    }
}
