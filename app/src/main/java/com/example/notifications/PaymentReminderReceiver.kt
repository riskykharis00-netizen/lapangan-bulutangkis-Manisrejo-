package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import com.example.data.local.PaymentStatus
import com.example.data.local.ReminderLogEntity
import com.example.data.local.ReminderLogStatus
import com.example.util.ScheduleFormatters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PaymentReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val bookingId = intent.getLongExtra(PaymentReminderNotifier.EXTRA_BOOKING_ID, -1L)
        if (bookingId <= 0L) return
        val stageLabel = intent.getStringExtra(PaymentReminderNotifier.EXTRA_STAGE_LABEL)
            ?: "Pengingat Pembayaran"

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val booking = db.bookingDao().getBookingById(bookingId)
                if (booking != null && booking.paymentStatus != PaymentStatus.PAID && booking.reminderEnabled) {
                    val amountDue = (booking.totalPrice - booking.paidAmount).coerceAtLeast(0L)
                    val scheduleLabel = "${ScheduleFormatters.dayName(booking.dayOfWeek)} ${ScheduleFormatters.formatHourRange(booking.startHour, booking.durationHours)}"
                    val shown = PaymentReminderNotifier.showPaymentReminderNotification(
                        context = context,
                        bookingId = booking.id,
                        customerName = booking.customerName,
                        courtName = booking.courtName,
                        scheduleLabel = scheduleLabel,
                        amountDue = amountDue,
                        stageLabel = stageLabel
                    )
                    if (shown) {
                        db.bookingDao().updateBooking(
                            booking.copy(reminderSentCount = booking.reminderSentCount + 1)
                        )
                        db.reminderLogDao().insertLog(
                            ReminderLogEntity(
                                bookingId = booking.id,
                                customerName = booking.customerName,
                                courtName = booking.courtName,
                                scheduleLabel = scheduleLabel,
                                stageLabel = stageLabel,
                                amountDue = amountDue,
                                scheduledForMillis = System.currentTimeMillis(),
                                sentAtMillis = System.currentTimeMillis(),
                                status = ReminderLogStatus.TERKIRIM,
                                message = ScheduleFormatters.buildPaymentReminderMessage(
                                    customerName = booking.customerName,
                                    courtName = booking.courtName,
                                    dayOfWeek = booking.dayOfWeek,
                                    startHour = booking.startHour,
                                    durationHours = booking.durationHours,
                                    amountDue = amountDue
                                )
                            )
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
