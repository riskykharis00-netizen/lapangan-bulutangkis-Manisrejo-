(function () {
  const STORAGE_KEY = "manisrejo_badminton_portal_v2";
  const OPEN_HOUR = 8;
  const CLOSE_HOUR = 24;

  const DAYS = [
    { id: 1, name: "Senin", short: "Sen" },
    { id: 2, name: "Selasa", short: "Sel" },
    { id: 3, name: "Rabu", short: "Rab" },
    { id: 4, name: "Kamis", short: "Kam" },
    { id: 5, name: "Jumat", short: "Jum" },
    { id: 6, name: "Sabtu", short: "Sab" },
    { id: 7, name: "Minggu", short: "Min" },
  ];

  const DEFAULT_DATA = {
    editorPin: "1234",
    venue: {
      name: "Lapangan Bulutangkis Manisrejo",
      phone: "082142272367",
      mapsUrl: "https://maps.app.goo.gl/xEdtA2gNiUx9pz9f9",
      openHoursText: "08:00 – 00:00",
    },
    courts: [
      { id: 1, name: "Lapangan A", rate: 15000, active: true, note: "" },
      { id: 2, name: "Lapangan B", rate: 15000, active: true, note: "" },
    ],
    bookings: [
      {
        id: 101,
        courtId: 1,
        courtName: "Lapangan A",
        dayOfWeek: 1,
        startHour: 18,
        durationHours: 2,
        customerName: "PB Manisrejo",
        customerPhone: "081234567890",
        totalPrice: 30000,
        paidAmount: 30000,
        paymentStatus: "PAID",
        reminderSentCount: 0,
        notes: "Rutin Senin",
        createdAt: Date.now() - 86400000 * 2,
      },
      {
        id: 102,
        courtId: 2,
        courtName: "Lapangan B",
        dayOfWeek: 3,
        startHour: 19,
        durationHours: 2,
        customerName: "Tim Badminton RT 04",
        customerPhone: "085711223344",
        totalPrice: 30000,
        paidAmount: 10000,
        paymentStatus: "DP",
        reminderSentCount: 1,
        notes: "DP Rp 10.000",
        createdAt: Date.now() - 86400000,
      },
    ],
    history: [
      {
        id: 1,
        courtId: 1,
        courtName: "Lapangan A",
        dayOfWeek: 1,
        startHour: 18,
        durationHours: 2,
        customerName: "PB Manisrejo",
        totalPrice: 30000,
        status: "CONFIRMED",
        createdAt: Date.now() - 86400000 * 2,
      },
      {
        id: 2,
        courtId: 2,
        courtName: "Lapangan B",
        dayOfWeek: 3,
        startHour: 19,
        durationHours: 2,
        customerName: "Tim Badminton RT 04",
        totalPrice: 30000,
        status: "CONFIRMED",
        createdAt: Date.now() - 86400000,
      },
    ],
  };

  function loadStore() {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) {
        const cloned = JSON.parse(JSON.stringify(DEFAULT_DATA));
        saveStore(cloned);
        return cloned;
      }
      const parsed = JSON.parse(raw);
      if (!parsed.editorPin) parsed.editorPin = "1234";
      return parsed;
    } catch (_) {
      return JSON.parse(JSON.stringify(DEFAULT_DATA));
    }
  }

  function saveStore(data) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
  }

  function formatHour(hour) {
    const h = Number(hour);
    if (h === 24 || h === 0) return "00:00";
    return String(h).padStart(2, "0") + ":00";
  }

  function formatRange(startHour, durationHours) {
    return `${formatHour(startHour)} – ${formatHour(startHour + durationHours)}`;
  }

  function parseManualHour(raw, allowMidnight) {
    if (!raw) return null;
    const cleaned = String(raw).trim().replace(".", ":");
    const parts = cleaned.split(":");
    if (parts.length > 2) return null;
    if (!/^\d+$/.test(parts[0])) return null;
    if (parts.length === 2 && parts[1] !== "00") return null;
    const parsed = parseInt(parts[0], 10);
    if ((parsed === 0 || parsed === 24) && allowMidnight) return 24;
    return parsed;
  }

  function formatRupiah(amount) {
    return "Rp " + Number(amount || 0).toLocaleString("id-ID");
  }

  function dayName(dayId) {
    return (DAYS.find((d) => d.id === Number(dayId)) || DAYS[0]).name;
  }

  function buildWhatsAppUrl(phone, message) {
    const normalized = String(phone || "082142272367").replace(/[^0-9]/g, "");
    const intl = normalized.startsWith("0") ? "62" + normalized.slice(1) : normalized;
    return `https://wa.me/${intl}?text=${encodeURIComponent(message)}`;
  }

  function showToast(msg) {
    const el = document.getElementById("toast");
    if (!el) return;
    el.textContent = msg;
    el.classList.remove("hidden");
    clearTimeout(window.__toastTimer);
    window.__toastTimer = setTimeout(() => el.classList.add("hidden"), 3400);
  }

  function todayIsoDay() {
    const jsDay = new Date().getDay();
    return jsDay === 0 ? 7 : jsDay;
  }

  function formatAvailableRangesSummary(courtBookings) {
    const sorted = [...courtBookings].sort((a, b) => a.startHour - b.startHour);
    const ranges = [];
    let cursor = OPEN_HOUR;
    for (const b of sorted) {
      if (b.startHour > cursor) {
        ranges.push(`${formatHour(cursor)}-${formatHour(b.startHour)}`);
      }
      cursor = Math.max(cursor, b.startHour + b.durationHours);
    }
    if (cursor < CLOSE_HOUR) {
      ranges.push(`${formatHour(cursor)}-${formatHour(CLOSE_HOUR)}`);
    }
    return ranges.length > 0 ? ranges.join(", ") : "Penuh sepanjang hari";
  }

  function createBooking({
    store,
    courtIds,
    dayOfWeek,
    startHour,
    endHour,
    customerName,
    customerPhone,
    paymentStatus,
    paidAmount,
    notes,
  }) {
    if (!customerName || !customerName.trim()) {
      return { error: "Nama penyewa / klub wajib diisi." };
    }
    if (!Array.isArray(courtIds) || courtIds.length === 0) {
      return { error: "Pilih minimal 1 lapangan." };
    }
    if (startHour === null || startHour < OPEN_HOUR || startHour >= CLOSE_HOUR) {
      return { error: "Jam mulai harus antara 08:00 hingga 23:00 (kelipatan jam penuh)." };
    }
    if (endHour === null || endHour <= startHour || endHour > CLOSE_HOUR) {
      return { error: "Jam selesai harus lebih besar dari jam mulai dan maksimal 00:00." };
    }

    const duration = endHour - startHour;
    const targetCourts = store.courts.filter((c) => courtIds.includes(c.id) && c.active);
    if (targetCourts.length === 0) {
      return { error: "Lapangan yang dipilih sedang tidak aktif." };
    }

    for (const court of targetCourts) {
      const conflict = store.bookings.find(
        (b) =>
          b.courtId === court.id &&
          b.dayOfWeek === dayOfWeek &&
          startHour < b.startHour + b.durationHours &&
          b.startHour < endHour
      );
      if (conflict) {
        return {
          error: `Bentrok di ${court.name}: ${formatRange(conflict.startHour, conflict.durationHours)} sudah dipesan (${conflict.customerName}).`,
        };
      }
    }

    const createdBookings = [];
    const now = Date.now();

    targetCourts.forEach((court, idx) => {
      const totalPrice = court.rate * duration;
      const finalPaid =
        paymentStatus === "PAID"
          ? totalPrice
          : paymentStatus === "DP"
          ? Math.min(Number(paidAmount || 0), totalPrice)
          : 0;

      const booking = {
        id: now + idx,
        courtId: court.id,
        courtName: court.name,
        dayOfWeek,
        startHour,
        durationHours: duration,
        customerName: customerName.trim(),
        customerPhone: (customerPhone || "").trim(),
        totalPrice,
        paidAmount: finalPaid,
        paymentStatus: paymentStatus || "UNPAID",
        reminderSentCount: 0,
        notes: (notes || "").trim(),
        createdAt: now,
      };
      store.bookings.push(booking);
      store.history.unshift({
        id: now + idx + 1000,
        courtId: court.id,
        courtName: court.name,
        dayOfWeek,
        startHour,
        durationHours: duration,
        customerName: booking.customerName,
        totalPrice,
        status: "CONFIRMED",
        createdAt: now,
      });
      createdBookings.push(booking);
    });

    saveStore(store);
    return { bookings: createdBookings };
  }

  window.ManisrejoPortal = {
    OPEN_HOUR,
    CLOSE_HOUR,
    DAYS,
    loadStore,
    saveStore,
    resetStore() {
      const fresh = JSON.parse(JSON.stringify(DEFAULT_DATA));
      saveStore(fresh);
      return fresh;
    },
    formatHour,
    formatRange,
    parseManualHour,
    formatRupiah,
    dayName,
    buildWhatsAppUrl,
    showToast,
    todayIsoDay,
    formatAvailableRangesSummary,
    createBooking,
  };
})();
