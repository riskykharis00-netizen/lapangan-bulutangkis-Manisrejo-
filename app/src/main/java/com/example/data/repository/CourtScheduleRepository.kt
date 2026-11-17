package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.BookingEntity
import com.example.data.local.BookingHistoryEntity
import com.example.data.local.BookingHistoryStatus
import com.example.data.local.CourtEntity
import com.example.data.local.PaymentStatus
import com.example.data.local.ReminderLogEntity
import com.example.data.local.ReminderLogStatus
import com.example.notifications.PaymentReminderNotifier
import com.example.util.ScheduleFormatters
import kotlinx.coroutines.flow.Flow

class CourtScheduleRepository(
    private val context: Context,
    private val db: AppDatabase
) {
    private val courtDao = db.courtDao()
    private val bookingDao = db.bookingDao()
    private val bookingHistoryDao = db.bookingHistoryDao()
    private val reminderLogDao = db.reminderLogDao()

    val courts: Flow<List<CourtEntity>> = courtDao.observeAllCourts()
    val bookings: Flow<List<BookingEntity>> = bookingDao.observeAllBookings()
    val bookingHistory: Flow<List<BookingHistoryEntity>> = bookingHistoryDao.observeAllHistory()
    val reminderLogs: Flow<List<ReminderLogEntity>> = reminderLogDao.observeAllLogs()

    suspend fun ensureDefaultCourts() {
        if (courtDao.countCourts() == 0) {
            courtDao.insertCourts(
                listOf(
                    CourtEntity(
                        name = "Lapangan A",
                        surfaceType = "",
                        hourlyRateRegular = ScheduleFormatters.DEFAULT_HOURLY_RATE,
                        hourlyRatePrime = ScheduleFormatters.DEFAULT_HOURLY_RATE,
                        hourlyRateWeekend = ScheduleFormatters.DEFAULT_HOURLY_RATE,
                        isActive = true
                    ),
                    CourtEntity(
                        name = "Lapangan B",
                        surfaceType = "",
                        hourlyRateRegular = ScheduleFormatters.DEFAULT_HOURLY_RATE,
                        hourlyRatePrime = ScheduleFormatters.DEFAULT_HOURLY_RATE,
                        hourlyRateWeekend = ScheduleFormatters.DEFAULT_HOURLY_RATE,
                        isActive = true
                    )
                )
            )
        } else {
            val existingCourts = courtDao.getAllCourtsOnce()
            existingCourts.forEach { court ->
                val updatedName = when {
                    court.name.equals("Lapangan 1", ignoreCase = true) -> "Lapangan A"
                    court.name.equals("Lapangan 2", ignoreCase = true) -> "Lapangan B"
                    else -> court.name
                }
                val updatedSurface = if (court.surfaceType.equals("Karpet BWF", ignoreCase = true)) {
                    ""
                } else {
                    court.surfaceType
                }
                if (updatedName != court.name || updatedSurface != court.surfaceType) {
                    courtDao.updateCourt(court.copy(name = updatedName, surfaceType = updatedSurface))
                }
            }
            bookingDao.renameCourtInBookings("Lapangan 1", "Lapangan A")
            bookingDao.renameCourtInBookings("Lapangan 2", "Lapangan B")
            bookingHistoryDao.renameCourtInHistory("Lapangan 1", "Lapangan A")
            bookingHistoryDao.renameCourtInHistory("Lapangan 2", "Lapangan B")
            reminderLogDao.renameCourtInLogs("Lapangan 1", "Lapangan A")
            reminderLogDao.renameCourtInLogs("Lapangan 2", "Lapangan B")
        }
    }

    fun calculateBookingPrice(
        court: CourtEntity,
        dayOfWeek: Int,
        startHour: Int,
        durationHours: Int
    ): Long {
        var total = 0L
        for (offset in 0 until durationHours) {
            val hour = startHour + offset
            total += when {
                dayOfWeek >= 6 -> court.hourlyRateWeekend
                hour >= 17 -> court.hourlyRatePrime
                else -> court.hourlyRateRegular
            }
        }
        return total
    }

    fun hasTimeConflict(
        existingBookings: List<BookingEntity>,
        courtId: Int,
        dayOfWeek: Int,
        startHour: Int,
        durationHours: Int,
        excludeBookingId: Long = -1L
    ): Boolean {
        val newEnd = startHour + durationHours
        return existingBookings.any { b ->
            b.id != excludeBookingId &&
                b.courtId == courtId &&
                b.dayOfWeek == dayOfWeek &&
                startHour < (b.startHour + b.durationHours) &&
                newEnd > b.startHour
        }
    }

    suspend fun createManualBooking(
        court: CourtEntity,
        dayOfWeek: Int,
        startHour: Int,
        durationHours: Int,
        customerName: String,
        customerPhone: String,
        paymentStatus: String,
        paidAmount: Long,
        reminderEnabled: Boolean,
        notes: String
    ): Result<BookingEntity> {
        return createManualBookings(
            courts = listOf(court),
            dayOfWeek = dayOfWeek,
            startHour = startHour,
            durationHours = durationHours,
            customerName = customerName,
            customerPhone = customerPhone,
            paymentStatus = paymentStatus,
            paidAmount = paidAmount,
            reminderEnabled = reminderEnabled,
            notes = notes
        ).map { it.first() }
    }

    suspend fun createManualBookings(
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
    ): Result<List<BookingEntity>> {
        val distinctCourts = courts.distinctBy { it.id }
        if (distinctCourts.isEmpty()) {
            return Result.failure(IllegalArgumentException("Pilih minimal 1 lapangan."))
        }
        val inactiveCourts = distinctCourts.filter { !it.isActive }
        if (inactiveCourts.isNotEmpty()) {
            return Result.failure(
                IllegalStateException("${inactiveCourts.joinToString(" & ") { it.name }} sedang dalam perawatan.")
            )
        }
        if (startHour < ScheduleFormatters.OPEN_HOUR ||
            durationHours < 1 ||
            startHour + durationHours > ScheduleFormatters.CLOSE_HOUR
        ) {
            return Result.failure(IllegalArgumentException("Jam operasional hanya pukul 08:00 - 00:00."))
        }

        val existing = bookingDao.getAllBookingsOnce()
        val conflictingCourts = distinctCourts.filter { court ->
            hasTimeConflict(existing, court.id, dayOfWeek, startHour, durationHours)
        }
        if (conflictingCourts.isNotEmpty()) {
            val conflictNames = conflictingCourts.joinToString(" & ") { it.name }
            return Result.failure(
                IllegalStateException("$conflictNames sudah dipesan pada jam tersebut.")
            )
        }

        val cleanName = customerName.trim()
        val cleanPhone = customerPhone.trim()
        val cleanNotes = notes.trim()
        var remainingDp = paidAmount.coerceAtLeast(0L)
        val savedBookings = mutableListOf<BookingEntity>()

        for (court in distinctCourts) {
            val totalPrice = calculateBookingPrice(court, dayOfWeek, startHour, durationHours)
            val effectivePaid = when (paymentStatus) {
                PaymentStatus.PAID -> totalPrice
                PaymentStatus.DP -> {
                    val allocated = remainingDp.coerceIn(0L, totalPrice)
                    remainingDp = (remainingDp - allocated).coerceAtLeast(0L)
                    allocated
                }
                else -> 0L
            }
            val effectivePaymentStatus = when {
                paymentStatus == PaymentStatus.PAID || effectivePaid >= totalPrice -> PaymentStatus.PAID
                paymentStatus == PaymentStatus.DP && effectivePaid > 0L -> PaymentStatus.DP
                paymentStatus == PaymentStatus.DP -> PaymentStatus.DP
                else -> PaymentStatus.UNPAID
            }

            val draft = BookingEntity(
                courtId = court.id,
                courtName = court.name,
                dayOfWeek = dayOfWeek,
                startHour = startHour,
                durationHours = durationHours,
                customerName = cleanName,
                customerPhone = cleanPhone,
                totalPrice = totalPrice,
                paidAmount = effectivePaid,
                paymentStatus = effectivePaymentStatus,
                reminderEnabled = reminderEnabled && effectivePaymentStatus != PaymentStatus.PAID,
                notes = cleanNotes
            )

            val newId = bookingDao.insertBooking(draft)
            val saved = draft.copy(id = newId)
            savedBookings.add(saved)

            bookingHistoryDao.insertHistory(
                BookingHistoryEntity(
                    bookingId = saved.id,
                    courtId = saved.courtId,
                    courtName = saved.courtName,
                    dayOfWeek = saved.dayOfWeek,
                    startHour = saved.startHour,
                    durationHours = saved.durationHours,
                    customerName = saved.customerName,
                    customerPhone = saved.customerPhone,
                    totalPrice = saved.totalPrice,
                    paidAmount = saved.paidAmount,
                    paymentStatus = saved.paymentStatus,
                    bookingStatus = BookingHistoryStatus.AKTIF,
                    notes = saved.notes,
                    createdAtMillis = saved.createdAtMillis
                )
            )

            notifyBookingConfirmed(saved)
            if (saved.reminderEnabled && saved.paymentStatus != PaymentStatus.PAID) {
                schedulePaymentReminderAndLog(saved)
            }
        }

        return Result.success(savedBookings)
    }

    suspend fun markBookingPaid(bookingId: Long) {
        val booking = bookingDao.getBookingById(bookingId) ?: return
        bookingDao.updateBooking(
            booking.copy(
                paymentStatus = PaymentStatus.PAID,
                paidAmount = booking.totalPrice,
                reminderEnabled = false
            )
        )
        bookingHistoryDao.updatePaymentForBooking(
            bookingId = bookingId,
            paymentStatus = PaymentStatus.PAID,
            paidAmount = booking.totalPrice
        )
        reminderLogDao.updateStatusForBooking(
            bookingId = bookingId,
            status = ReminderLogStatus.LUNAS,
            sentAt = System.currentTimeMillis()
        )
        PaymentReminderNotifier.cancelPaymentReminder(context, bookingId)
    }

    suspend fun recordDownPayment(bookingId: Long, dpAmount: Long) {
        val booking = bookingDao.getBookingById(bookingId) ?: return
        val newPaid = (booking.paidAmount + dpAmount).coerceIn(0L, booking.totalPrice)
        val status = if (newPaid >= booking.totalPrice) PaymentStatus.PAID else PaymentStatus.DP
        val updated = booking.copy(
            paidAmount = newPaid,
            paymentStatus = status,
            reminderEnabled = status != PaymentStatus.PAID && booking.reminderEnabled
        )
        bookingDao.updateBooking(updated)
        bookingHistoryDao.updatePaymentForBooking(
            bookingId = bookingId,
            paymentStatus = status,
            paidAmount = newPaid
        )
        if (status == PaymentStatus.PAID) {
            reminderLogDao.updateStatusForBooking(bookingId, ReminderLogStatus.LUNAS, System.currentTimeMillis())
            PaymentReminderNotifier.cancelPaymentReminder(context, bookingId)
        } else if (updated.reminderEnabled) {
            PaymentReminderNotifier.schedulePaymentReminder(context, updated)
        }
    }

    suspend fun sendImmediateReminder(bookingId: Long): Boolean {
        val booking = bookingDao.getBookingById(bookingId) ?: return false
        val amountDue = (booking.totalPrice - booking.paidAmount).coerceAtLeast(0L)
        val scheduleLabel = "${ScheduleFormatters.dayName(booking.dayOfWeek)} ${ScheduleFormatters.formatHourRange(booking.startHour, booking.durationHours)}"
        val shown = PaymentReminderNotifier.showPaymentReminderNotification(
            context = context,
            bookingId = booking.id,
            customerName = booking.customerName,
            courtName = booking.courtName,
            scheduleLabel = scheduleLabel,
            amountDue = amountDue,
            stageLabel = "Pengingat Pembayaran"
        )
        val msg = ScheduleFormatters.buildPaymentReminderMessage(
            customerName = booking.customerName,
            courtName = booking.courtName,
            dayOfWeek = booking.dayOfWeek,
            startHour = booking.startHour,
            durationHours = booking.durationHours,
            amountDue = amountDue
        )
        bookingDao.updateBooking(booking.copy(reminderSentCount = booking.reminderSentCount + 1))
        reminderLogDao.insertLog(
            ReminderLogEntity(
                bookingId = booking.id,
                customerName = booking.customerName,
                courtName = booking.courtName,
                scheduleLabel = scheduleLabel,
                stageLabel = "Pengingat Pembayaran",
                amountDue = amountDue,
                scheduledForMillis = System.currentTimeMillis(),
                sentAtMillis = System.currentTimeMillis(),
                status = ReminderLogStatus.TERKIRIM,
                message = msg
            )
        )
        return shown
    }

    suspend fun cancelBooking(bookingId: Long) {
        PaymentReminderNotifier.cancelPaymentReminder(context, bookingId)
        bookingHistoryDao.updateBookingStatus(bookingId, BookingHistoryStatus.DIBATALKAN)
        reminderLogDao.deleteLogsForBooking(bookingId)
        bookingDao.deleteBookingById(bookingId)
    }

    suspend fun saveCourt(court: CourtEntity) {
        if (court.id == 0) {
            courtDao.insertCourt(court)
        } else {
            courtDao.updateCourt(court)
        }
    }

    suspend fun toggleCourtActive(court: CourtEntity) {
        courtDao.updateCourt(court.copy(isActive = !court.isActive))
    }

    private suspend fun notifyBookingConfirmed(booking: BookingEntity) {
        PaymentReminderNotifier.showBookingConfirmedNotification(context, booking)
        logConfirmationOnly(booking)
    }

    private suspend fun logConfirmationOnly(booking: BookingEntity) {
        val amountDue = (booking.totalPrice - booking.paidAmount).coerceAtLeast(0L)
        val scheduleLabel = "${ScheduleFormatters.dayName(booking.dayOfWeek)} ${ScheduleFormatters.formatHourRange(booking.startHour, booking.durationHours)}"
        reminderLogDao.insertLog(
            ReminderLogEntity(
                bookingId = booking.id,
                customerName = booking.customerName,
                courtName = booking.courtName,
                scheduleLabel = scheduleLabel,
                stageLabel = "Konfirmasi Pemesanan",
                amountDue = amountDue,
                scheduledForMillis = System.currentTimeMillis(),
                sentAtMillis = System.currentTimeMillis(),
                status = ReminderLogStatus.KONFIRMASI,
                message = "Pemesanan ${booking.courtName} ($scheduleLabel) telah dikonfirmasi."
            )
        )
    }

    private suspend fun schedulePaymentReminderAndLog(booking: BookingEntity) {
        val amountDue = (booking.totalPrice - booking.paidAmount).coerceAtLeast(0L)
        val scheduleLabel = "${ScheduleFormatters.dayName(booking.dayOfWeek)} ${ScheduleFormatters.formatHourRange(booking.startHour, booking.durationHours)}"
        val msg = ScheduleFormatters.buildPaymentReminderMessage(
            customerName = booking.customerName,
            courtName = booking.courtName,
            dayOfWeek = booking.dayOfWeek,
            startHour = booking.startHour,
            durationHours = booking.durationHours,
            amountDue = amountDue
        )
        val triggerAt = PaymentReminderNotifier.schedulePaymentReminder(context, booking)
        if (triggerAt != null) {
            reminderLogDao.insertLog(
                ReminderLogEntity(
                    bookingId = booking.id,
                    customerName = booking.customerName,
                    courtName = booking.courtName,
                    scheduleLabel = scheduleLabel,
                    stageLabel = "Pengingat Pembayaran",
                    amountDue = amountDue,
                    scheduledForMillis = triggerAt,
                    status = ReminderLogStatus.TERJADWAL,
                    message = msg
                )
            )
        }
    }
}
