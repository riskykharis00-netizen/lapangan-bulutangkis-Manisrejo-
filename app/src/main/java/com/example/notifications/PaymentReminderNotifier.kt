package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.BookingEntity
import com.example.data.local.PaymentStatus
import com.example.util.ScheduleFormatters

object PaymentReminderNotifier {
    const val CHANNEL_ID = "shuttlecourt_payment_reminders"
    const val CHANNEL_CONFIRM_ID = "shuttlecourt_booking_confirmations"

    const val EXTRA_BOOKING_ID = "extra_booking_id"
    const val EXTRA_STAGE_LABEL = "extra_stage_label"

    const val ACTION_MARK_PAID = "com.example.shuttlecourt.ACTION_MARK_PAID"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val reminderChannel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.notification_channel_desc)
                    enableVibration(true)
                }
                manager.createNotificationChannel(reminderChannel)
            }
            if (manager.getNotificationChannel(CHANNEL_CONFIRM_ID) == null) {
                val confirmChannel = NotificationChannel(
                    CHANNEL_CONFIRM_ID,
                    "Konfirmasi Pemesanan",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Pemberitahuan otomatis saat pemesanan lapangan berhasil dikonfirmasi."
                }
                manager.createNotificationChannel(confirmChannel)
            }
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun schedulePaymentReminder(context: Context, booking: BookingEntity, delayMinutes: Int = 60): Long? {
        cancelPaymentReminder(context, booking.id)
        if (!booking.reminderEnabled || booking.paymentStatus == PaymentStatus.PAID) {
            return null
        }
        ensureChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + (delayMinutes.coerceAtLeast(1) * 60_000L)
        val pendingIntent = buildAlarmPendingIntent(context, booking.id, "Pengingat Pembayaran")
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent
        )
        return triggerAt
    }

    fun cancelPaymentReminder(context: Context, bookingId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, PaymentReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            bookingId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        NotificationManagerCompat.from(context).cancel(bookingId.toInt())
    }

    fun showBookingConfirmedNotification(
        context: Context,
        booking: BookingEntity
    ): Boolean {
        ensureChannel(context)
        if (!hasNotificationPermission(context)) return false

        val scheduleLabel = "${ScheduleFormatters.dayName(booking.dayOfWeek)} ${ScheduleFormatters.formatHourRange(booking.startHour, booking.durationHours)}"
        val totalLabel = ScheduleFormatters.formatRupiah(booking.totalPrice)
        val statusInfo = when (booking.paymentStatus) {
            PaymentStatus.PAID -> "Status: Lunas ($totalLabel)."
            PaymentStatus.DP -> "DP tercatat. Sisa tagihan ${ScheduleFormatters.formatRupiah((booking.totalPrice - booking.paidAmount).coerceAtLeast(0L))}."
            else -> "Total sewa $totalLabel • Status: Belum Bayar."
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_BOOKING_ID, booking.id)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            (booking.id.toInt() * 100) + 9,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_CONFIRM_ID)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle("Pemesanan Dikonfirmasi • ${booking.courtName}")
            .setContentText("${booking.customerName} • $scheduleLabel")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Jadwal ${booking.customerName} di ${booking.courtName} ($scheduleLabel) berhasil dicatat. $statusInfo Silakan konfirmasi langsung via WhatsApp ke ${ScheduleFormatters.VENUE_PHONE}."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify((booking.id.toInt() * 100) + 9, notification)
        return true
    }

    fun showPaymentReminderNotification(
        context: Context,
        bookingId: Long,
        customerName: String,
        courtName: String,
        scheduleLabel: String,
        amountDue: Long,
        stageLabel: String = "Pengingat Pembayaran"
    ): Boolean {
        ensureChannel(context)
        if (!hasNotificationPermission(context)) {
            return false
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_BOOKING_ID, bookingId)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            bookingId.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val markPaidIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_MARK_PAID
            putExtra(EXTRA_BOOKING_ID, bookingId)
        }
        val markPaidPendingIntent = PendingIntent.getBroadcast(
            context,
            (bookingId.toInt() * 31) + 7,
            markPaidIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val amountLabel = ScheduleFormatters.formatRupiah(amountDue)
        val bodyText = "Penyewa $customerName ($courtName • $scheduleLabel) memiliki sisa tagihan $amountLabel. Konfirmasi ke ${ScheduleFormatters.VENUE_PHONE}."

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("$stageLabel • $amountLabel")
            .setContentText("$customerName - $courtName ($scheduleLabel)")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(
                android.R.drawable.checkbox_on_background,
                "Tandai Lunas",
                markPaidPendingIntent
            )
            .build()

        NotificationManagerCompat.from(context).notify(bookingId.toInt(), notification)
        return true
    }

    private fun buildAlarmPendingIntent(
        context: Context,
        bookingId: Long,
        stageLabel: String
    ): PendingIntent {
        val intent = Intent(context, PaymentReminderReceiver::class.java).apply {
            putExtra(EXTRA_BOOKING_ID, bookingId)
            putExtra(EXTRA_STAGE_LABEL, stageLabel)
        }
        return PendingIntent.getBroadcast(
            context,
            bookingId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
