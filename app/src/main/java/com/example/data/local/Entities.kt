package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

object PaymentStatus {
    const val UNPAID = "UNPAID"
    const val DP = "DP"
    const val PAID = "PAID"
}

object ReminderLogStatus {
    const val KONFIRMASI = "KONFIRMASI"
    const val TERJADWAL = "TERJADWAL"
    const val TERKIRIM = "TERKIRIM"
    const val LUNAS = "LUNAS"
}

object BookingHistoryStatus {
    const val AKTIF = "AKTIF"
    const val DIBATALKAN = "DIBATALKAN"
}

@Entity(tableName = "courts")
data class CourtEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val surfaceType: String,
    val hourlyRateRegular: Long = 15_000L,
    val hourlyRatePrime: Long = 15_000L,
    val hourlyRateWeekend: Long = 15_000L,
    val isActive: Boolean = true
)

@Entity(tableName = "bookings")
data class BookingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courtId: Int,
    val courtName: String,
    /** 1 = Senin .. 7 = Minggu */
    val dayOfWeek: Int,
    /** 8 .. 23 */
    val startHour: Int,
    val durationHours: Int = 1,
    val customerName: String,
    val customerPhone: String,
    val totalPrice: Long,
    val paidAmount: Long = 0L,
    val paymentStatus: String = PaymentStatus.UNPAID,
    val reminderEnabled: Boolean = true,
    val reminderSentCount: Int = 0,
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "booking_history")
data class BookingHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookingId: Long,
    val courtId: Int,
    val courtName: String,
    val dayOfWeek: Int,
    val startHour: Int,
    val durationHours: Int,
    val customerName: String,
    val customerPhone: String,
    val totalPrice: Long,
    val paidAmount: Long = 0L,
    val paymentStatus: String = PaymentStatus.UNPAID,
    val bookingStatus: String = BookingHistoryStatus.AKTIF,
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "reminder_logs")
data class ReminderLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookingId: Long,
    val customerName: String,
    val courtName: String,
    val scheduleLabel: String,
    val stageLabel: String,
    val amountDue: Long,
    val scheduledForMillis: Long,
    val sentAtMillis: Long? = null,
    val status: String = ReminderLogStatus.TERJADWAL,
    val message: String
)
