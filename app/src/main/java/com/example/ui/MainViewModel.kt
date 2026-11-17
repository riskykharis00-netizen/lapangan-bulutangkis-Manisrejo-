package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.BookingEntity
import com.example.data.local.BookingHistoryEntity
import com.example.data.local.CourtEntity
import com.example.data.local.PaymentStatus
import com.example.data.local.ReminderLogEntity
import com.example.data.repository.CourtScheduleRepository
import com.example.notifications.PaymentReminderNotifier
import com.example.util.ScheduleFormatters
import java.util.Calendar
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class UserRole {
    RENTER,
    EDITOR
}

enum class AppTab {
    SCHEDULE,
    HISTORY,
    PAYMENTS,
    COURTS
}

enum class SlotFilter {
    ALL,
    AVAILABLE,
    BOOKED
}

data class BookingDraft(
    val courtId: Int,
    val dayOfWeek: Int,
    val startHour: Int
)

data class BookingConfirmationPrompt(
    val customerName: String,
    val scheduleSummary: String,
    val totalPrice: Long,
    val whatsAppMessage: String
)

data class ShuttleUiState(
    val isLoading: Boolean = true,
    val userRole: UserRole = UserRole.RENTER,
    val selectedDay: Int = 1,
    val selectedCourtId: Int = 0, // 0 = Semua Lapangan
    val slotFilter: SlotFilter = SlotFilter.ALL,
    val courts: List<CourtEntity> = emptyList(),
    val bookings: List<BookingEntity> = emptyList(),
    val bookingHistory: List<BookingHistoryEntity> = emptyList(),
    val reminderLogs: List<ReminderLogEntity> = emptyList()
) {
    val totalPaidRevenue: Long
        get() = bookings.sumOf { it.paidAmount }

    val totalPendingAmount: Long
        get() = bookings.filter { it.paymentStatus != PaymentStatus.PAID }
            .sumOf { (it.totalPrice - it.paidAmount).coerceAtLeast(0L) }

    val pendingBookings: List<BookingEntity>
        get() = bookings.filter { it.paymentStatus != PaymentStatus.PAID }
            .sortedWith(compareBy<BookingEntity> { it.dayOfWeek }.thenBy { it.startHour })

    val paidBookings: List<BookingEntity>
        get() = bookings.filter { it.paymentStatus == PaymentStatus.PAID }
            .sortedByDescending { it.createdAtMillis }

    val activeReminderCount: Int
        get() = bookings.count { it.paymentStatus != PaymentStatus.PAID && it.reminderEnabled }

    fun bookingsCountForDay(day: Int): Int =
        bookings.count { it.dayOfWeek == day && (selectedCourtId == 0 || it.courtId == selectedCourtId) }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CourtScheduleRepository(
        context = application,
        db = AppDatabase.getInstance(application)
    )

    private val _userRole = MutableStateFlow(UserRole.RENTER)
    private val _selectedDay = MutableStateFlow(currentIsoDayOfWeek())
    private val _selectedCourtId = MutableStateFlow(0)
    private val _slotFilter = MutableStateFlow(SlotFilter.ALL)

    private val _activeTab = MutableStateFlow(AppTab.SCHEDULE)
    val activeTab: StateFlow<AppTab> = _activeTab.asStateFlow()

    private val _bookingDraft = MutableStateFlow<BookingDraft?>(null)
    val bookingDraft: StateFlow<BookingDraft?> = _bookingDraft.asStateFlow()

    private val _confirmationPrompt = MutableStateFlow<BookingConfirmationPrompt?>(null)
    val confirmationPrompt: StateFlow<BookingConfirmationPrompt?> = _confirmationPrompt.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    val uiState: StateFlow<ShuttleUiState> = combine(
        repository.courts,
        repository.bookings,
        repository.bookingHistory,
        repository.reminderLogs,
        _selectedDay,
        _selectedCourtId,
        _slotFilter,
        _userRole
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val courts = values[0] as List<CourtEntity>
        @Suppress("UNCHECKED_CAST")
        val bookings = values[1] as List<BookingEntity>
        @Suppress("UNCHECKED_CAST")
        val bookingHistory = values[2] as List<BookingHistoryEntity>
        @Suppress("UNCHECKED_CAST")
        val reminderLogs = values[3] as List<ReminderLogEntity>
        val selectedDay = values[4] as Int
        val selectedCourtId = values[5] as Int
        val slotFilter = values[6] as SlotFilter
        val userRole = values[7] as UserRole

        ShuttleUiState(
            isLoading = false,
            userRole = userRole,
            selectedDay = selectedDay,
            selectedCourtId = selectedCourtId,
            slotFilter = slotFilter,
            courts = courts,
            bookings = bookings,
            bookingHistory = bookingHistory,
            reminderLogs = reminderLogs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = ShuttleUiState(selectedDay = currentIsoDayOfWeek())
    )

    init {
        PaymentReminderNotifier.ensureChannel(application)
        viewModelScope.launch {
            repository.ensureDefaultCourts()
        }
    }

    fun selectUserRole(role: UserRole) {
        if (_userRole.value == role) return
        _userRole.value = role
        if (role == UserRole.RENTER && (_activeTab.value == AppTab.PAYMENTS || _activeTab.value == AppTab.COURTS)) {
            _activeTab.value = AppTab.SCHEDULE
        }
        val msg = if (role == UserRole.RENTER) {
            "Mode Penyewa aktif: lihat jadwal, pesan lapangan, dan riwayat."
        } else {
            "Mode Editor Website aktif: kelola tagihan, pembatalan, dan pengaturan lapangan."
        }
        _snackbarMessages.tryEmit(msg)
    }

    fun selectTab(tab: AppTab) {
        if (_userRole.value == UserRole.RENTER && (tab == AppTab.PAYMENTS || tab == AppTab.COURTS)) {
            _activeTab.value = AppTab.SCHEDULE
            return
        }
        _activeTab.value = tab
    }

    fun selectDay(day: Int) {
        _selectedDay.value = day.coerceIn(1, 7)
    }

    fun selectCourtFilter(courtId: Int) {
        _selectedCourtId.value = courtId
    }

    fun selectSlotFilter(filter: SlotFilter) {
        _slotFilter.value = filter
    }

    fun openBookingDialog(courtId: Int, dayOfWeek: Int, startHour: Int) {
        _bookingDraft.value = BookingDraft(courtId, dayOfWeek, startHour)
    }

    fun dismissBookingDialog() {
        _bookingDraft.value = null
    }

    fun dismissConfirmationPrompt() {
        _confirmationPrompt.value = null
    }

    fun calculateTotalPrice(
        courts: List<CourtEntity>,
        dayOfWeek: Int,
        startHour: Int,
        durationHours: Int
    ): Long = courts.sumOf { repository.calculateBookingPrice(it, dayOfWeek, startHour, durationHours) }

    fun createManualBooking(
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
    ) {
        viewModelScope.launch {
            val result = repository.createManualBookings(
                courts = courts,
                dayOfWeek = dayOfWeek,
                startHour = startHour,
                durationHours = durationHours,
                customerName = customerName,
                customerPhone = customerPhone,
                paymentStatus = paymentStatus,
                paidAmount = paidAmount,
                reminderEnabled = reminderEnabled,
                notes = notes
            )
            result.onSuccess { savedBookings ->
                _bookingDraft.value = null
                val first = savedBookings.first()
                val courtNames = savedBookings.joinToString(" & ") { it.courtName }
                val totalCombinedPrice = savedBookings.sumOf { it.totalPrice }
                val summary = "$courtNames • ${ScheduleFormatters.dayName(first.dayOfWeek)}, ${ScheduleFormatters.formatHourRange(first.startHour, first.durationHours)}"
                val waMsg = ScheduleFormatters.buildBookingConfirmWhatsAppMessage(
                    customerName = first.customerName,
                    scheduleSummary = summary,
                    totalPrice = totalCombinedPrice
                )
                _confirmationPrompt.value = BookingConfirmationPrompt(
                    customerName = first.customerName,
                    scheduleSummary = summary,
                    totalPrice = totalCombinedPrice,
                    whatsAppMessage = waMsg
                )
                _snackbarMessages.tryEmit("Jadwal $courtNames tersimpan! Silakan konfirmasi via WhatsApp.")
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.message ?: "Gagal membuat pemesanan.")
            }
        }
    }

    fun markBookingPaid(bookingId: Long) {
        viewModelScope.launch {
            repository.markBookingPaid(bookingId)
            _snackbarMessages.tryEmit("Pembayaran ditandai Lunas & pengingat dihentikan.")
        }
    }

    fun recordDownPayment(bookingId: Long, dpAmount: Long) {
        viewModelScope.launch {
            repository.recordDownPayment(bookingId, dpAmount)
            _snackbarMessages.tryEmit("Catatan DP pembayaran berhasil diperbarui.")
        }
    }

    fun sendImmediateReminder(bookingId: Long) {
        viewModelScope.launch {
            val shown = repository.sendImmediateReminder(bookingId)
            val msg = if (shown) {
                "Notifikasi pengingat pembayaran dikirim ke sistem."
            } else {
                "Pengingat dicatat. Aktifkan izin notifikasi agar tampil di status bar."
            }
            _snackbarMessages.tryEmit(msg)
        }
    }

    fun cancelBooking(bookingId: Long) {
        viewModelScope.launch {
            repository.cancelBooking(bookingId)
            _snackbarMessages.tryEmit("Pemesanan dibatalkan dan slot kembali tersedia.")
        }
    }

    fun saveCourt(court: CourtEntity) {
        viewModelScope.launch {
            repository.saveCourt(court)
            _snackbarMessages.tryEmit("Data ${court.name} tersimpan.")
        }
    }

    fun toggleCourtActive(court: CourtEntity) {
        viewModelScope.launch {
            repository.toggleCourtActive(court)
        }
    }

    private fun currentIsoDayOfWeek(): Int {
        val cal = Calendar.getInstance()
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    }
}
