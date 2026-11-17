package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.data.local.AppDatabase
import com.example.data.local.PaymentStatus
import com.example.data.local.ReminderLogStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != PaymentReminderNotifier.ACTION_MARK_PAID) return
        val bookingId = intent.getLongExtra(PaymentReminderNotifier.EXTRA_BOOKING_ID, -1L)
        if (bookingId <= 0L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val booking = db.bookingDao().getBookingById(bookingId)
                if (booking != null) {
                    db.bookingDao().updateBooking(
                        booking.copy(
                            paymentStatus = PaymentStatus.PAID,
                            paidAmount = booking.totalPrice,
                            reminderEnabled = false
                        )
                    )
                    db.reminderLogDao().updateStatusForBooking(
                        bookingId = booking.id,
                        status = ReminderLogStatus.LUNAS,
                        sentAt = System.currentTimeMillis()
                    )
                }
                PaymentReminderNotifier.cancelPaymentReminder(context, bookingId)
                NotificationManagerCompat.from(context).cancel(bookingId.toInt())
            } finally {
                pendingResult.finish()
            }
        }
    }
}
