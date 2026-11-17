package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CourtDao {
    @Query("SELECT * FROM courts ORDER BY id ASC")
    fun observeAllCourts(): Flow<List<CourtEntity>>

    @Query("SELECT * FROM courts ORDER BY id ASC")
    suspend fun getAllCourtsOnce(): List<CourtEntity>

    @Query("SELECT COUNT(*) FROM courts")
    suspend fun countCourts(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourt(court: CourtEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourts(courts: List<CourtEntity>)

    @Update
    suspend fun updateCourt(court: CourtEntity)
}

@Dao
interface BookingDao {
    @Query("SELECT * FROM bookings ORDER BY dayOfWeek ASC, startHour ASC, courtId ASC")
    fun observeAllBookings(): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings ORDER BY dayOfWeek ASC, startHour ASC, courtId ASC")
    suspend fun getAllBookingsOnce(): List<BookingEntity>

    @Query("SELECT * FROM bookings WHERE id = :bookingId LIMIT 1")
    suspend fun getBookingById(bookingId: Long): BookingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: BookingEntity): Long

    @Update
    suspend fun updateBooking(booking: BookingEntity)

    @Query("DELETE FROM bookings WHERE id = :bookingId")
    suspend fun deleteBookingById(bookingId: Long)

    @Query("UPDATE bookings SET courtName = :newName WHERE courtName = :oldName")
    suspend fun renameCourtInBookings(oldName: String, newName: String)
}

@Dao
interface ReminderLogDao {
    @Query("SELECT * FROM reminder_logs ORDER BY scheduledForMillis DESC, id DESC")
    fun observeAllLogs(): Flow<List<ReminderLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ReminderLogEntity): Long

    @Query("UPDATE reminder_logs SET status = :status, sentAtMillis = :sentAt WHERE bookingId = :bookingId AND status != 'LUNAS'")
    suspend fun updateStatusForBooking(bookingId: Long, status: String, sentAt: Long?)

    @Query("DELETE FROM reminder_logs WHERE bookingId = :bookingId")
    suspend fun deleteLogsForBooking(bookingId: Long)

    @Query("UPDATE reminder_logs SET courtName = :newName WHERE courtName = :oldName")
    suspend fun renameCourtInLogs(oldName: String, newName: String)
}

@Dao
interface BookingHistoryDao {
    @Query("SELECT * FROM booking_history ORDER BY createdAtMillis DESC, id DESC")
    fun observeAllHistory(): Flow<List<BookingHistoryEntity>>

    @Query("SELECT COUNT(*) FROM booking_history WHERE bookingId = :bookingId")
    suspend fun countForBooking(bookingId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entry: BookingHistoryEntity): Long

    @Query("UPDATE booking_history SET paymentStatus = :paymentStatus, paidAmount = :paidAmount WHERE bookingId = :bookingId")
    suspend fun updatePaymentForBooking(bookingId: Long, paymentStatus: String, paidAmount: Long)

    @Query("UPDATE booking_history SET bookingStatus = :bookingStatus WHERE bookingId = :bookingId")
    suspend fun updateBookingStatus(bookingId: Long, bookingStatus: String)

    @Query("UPDATE booking_history SET courtName = :newName WHERE courtName = :oldName")
    suspend fun renameCourtInHistory(oldName: String, newName: String)
}
