(function () {
  const P = window.ManisrejoPortal;
  let store = P.loadStore();
  let selectedDay = P.todayIsoDay();
  let courtFilter = 0;
  let statusFilter = "ALL";
  let selectedCourtMode = "1";
  let historyCourtFilter = 0;

  const dayStripEl = document.getElementById("dayStrip");
  const slotListEl = document.getElementById("slotList");
  const weeklyListEl = document.getElementById("weeklyOverviewList");
  const historyListEl = document.getElementById("historyList");
  const inputDayEl = document.getElementById("inputDay");
  const inputStartEl = document.getElementById("inputStartHour");
  const inputEndEl = document.getElementById("inputEndHour");

  P.DAYS.forEach((d) => {
    const opt = document.createElement("option");
    opt.value = d.id;
    opt.textContent = d.name;
    inputDayEl.appendChild(opt);
  });
  inputDayEl.value = String(selectedDay);

  function switchTab(tab) {
    const tabs = {
      jadwal: ["tabJadwalBtn", "sectionJadwal"],
      mingguan: ["tabMingguanBtn", "sectionMingguan"],
      riwayat: ["tabRiwayatBtn", "sectionRiwayat"],
    };
    Object.entries(tabs).forEach(([key, [btnId, secId]]) => {
      document.getElementById(btnId).classList.toggle("active", key === tab);
      document.getElementById(secId).classList.toggle("hidden", key !== tab);
    });
    if (tab === "mingguan") renderWeekly();
    if (tab === "riwayat") renderHistory();
  }

  document.getElementById("tabJadwalBtn").onclick = () => switchTab("jadwal");
  document.getElementById("tabMingguanBtn").onclick = () => switchTab("mingguan");
  document.getElementById("tabRiwayatBtn").onclick = () => switchTab("riwayat");

  function selectedCourtsFromMode() {
    const active = store.courts.filter((c) => c.active);
    if (selectedCourtMode === "BOTH") return active.slice(0, 2);
    const found = active.find((c) => String(c.id) === selectedCourtMode);
    return found ? [found] : active.slice(0, 1);
  }

  function updatePricePreview() {
    const s = P.parseManualHour(inputStartEl.value, false);
    const e = P.parseManualHour(inputEndEl.value, true);
    const courts = selectedCourtsFromMode();
    const summaryEl = document.getElementById("priceSummaryText");
    const totalEl = document.getElementById("priceTotalText");

    if (s === null || e === null || s < P.OPEN_HOUR || s >= P.CLOSE_HOUR || e <= s || e > P.CLOSE_HOUR) {
      summaryEl.textContent = "Jam belum valid (isi 08:00 – 00:00)";
      totalEl.textContent = "-";
      return;
    }
    const dur = e - s;
    const total = courts.reduce((acc, c) => acc + c.rate * dur, 0);
    const names = courts.map((c) => c.name).join(" & ");
    summaryEl.textContent = `${names} • ${P.dayName(inputDayEl.value)}, ${P.formatRange(s, dur)} (${dur} jam)`;
    totalEl.textContent = P.formatRupiah(total);
  }

  function renderVenueLinks() {
    document.getElementById("venueNameNav").textContent = store.venue.name;
    document.getElementById("venueNameHero").textContent = store.venue.name;
    document.getElementById("footerVenueName").textContent = store.venue.name;
    const waUrl = P.buildWhatsAppUrl(store.venue.phone, `Halo Admin ${store.venue.name}, saya ingin konfirmasi jadwal lapangan.`);
    ["waHeroBtn", "waFooterBtn"].forEach((id) => (document.getElementById(id).href = waUrl));
    document.getElementById("waHeroBtn").textContent = `WhatsApp ${store.venue.phone}`;
    document.getElementById("waFooterBtn").textContent = `WA ${store.venue.phone}`;
    ["mapsHeroBtn", "mapsFooterBtn"].forEach((id) => (document.getElementById(id).href = store.venue.mapsUrl));
  }

  function renderDayStrip() {
    dayStripEl.innerHTML = "";
    P.DAYS.forEach((d) => {
      const count = store.bookings.filter(
        (b) => b.dayOfWeek === d.id && (courtFilter === 0 || b.courtId === courtFilter)
      ).length;
      const btn = document.createElement("button");
      btn.type = "button";
      btn.className = `day-btn ${selectedDay === d.id ? "active" : ""}`;
      btn.innerHTML = `<span>${d.short}</span><span class="day-count">${count}</span>`;
      btn.onclick = () => {
        selectedDay = d.id;
        inputDayEl.value = String(d.id);
        renderAll();
      };
      dayStripEl.appendChild(btn);
    });
  }

  function renderFilters() {
    const activeCourts = store.courts.filter((c) => c.active);
    const courtChipsEl = document.getElementById("courtFilterChips");
    courtChipsEl.innerHTML = "";

    const allBtn = document.createElement("button");
    allBtn.type = "button";
    allBtn.className = `chip ${courtFilter === 0 ? "active" : ""}`;
    allBtn.textContent = "Semua Lapangan";
    allBtn.onclick = () => { courtFilter = 0; renderAll(); };
    courtChipsEl.appendChild(allBtn);

    activeCourts.forEach((c) => {
      const chip = document.createElement("button");
      chip.type = "button";
      chip.className = `chip ${courtFilter === c.id ? "active" : ""}`;
      chip.textContent = c.name;
      chip.onclick = () => { courtFilter = c.id; renderAll(); };
      courtChipsEl.appendChild(chip);
    });

    const statusEl = document.getElementById("statusFilterChips");
    statusEl.innerHTML = "";
    [
      ["ALL", "Semua Slot"],
      ["AVAILABLE", "Tersedia"],
      ["BOOKED", "Sudah Dipesan"],
    ].forEach(([code, label]) => {
      const chip = document.createElement("button");
      chip.type = "button";
      chip.className = `chip ${statusFilter === code ? "active" : ""}`;
      chip.textContent = label;
      chip.onclick = () => { statusFilter = code; renderFilters(); renderSlots(); };
      statusEl.appendChild(chip);
    });

    const modeEl = document.getElementById("formCourtModeChips");
    modeEl.innerHTML = "";
    activeCourts.forEach((c) => {
      const btn = document.createElement("button");
      btn.type = "button";
      btn.className = `chip ${selectedCourtMode === String(c.id) ? "active" : ""}`;
      btn.textContent = `${c.name} (${P.formatRupiah(c.rate)}/jam)`;
      btn.onclick = () => { selectedCourtMode = String(c.id); renderFilters(); updatePricePreview(); };
      modeEl.appendChild(btn);
    });
    if (activeCourts.length >= 2) {
      const bothBtn = document.createElement("button");
      bothBtn.type = "button";
      bothBtn.className = `chip ${selectedCourtMode === "BOTH" ? "active" : ""}`;
      bothBtn.textContent = "Pesan 2 Lapangan (A & B)";
      bothBtn.onclick = () => { selectedCourtMode = "BOTH"; renderFilters(); updatePricePreview(); };
      modeEl.appendChild(bothBtn);
    }
  }

  function renderSlots() {
    const visibleCourts = store.courts.filter((c) => c.active && (courtFilter === 0 || c.id === courtFilter));
    const dayBookings = store.bookings.filter(
      (b) => b.dayOfWeek === selectedDay && (courtFilter === 0 || b.courtId === courtFilter)
    );
    const bookedHours = dayBookings.reduce((sum, b) => sum + b.durationHours, 0);
    const totalCap = Math.max(visibleCourts.length * (P.CLOSE_HOUR - P.OPEN_HOUR), 1);
    document.getElementById("statBookedHours").textContent = `${bookedHours} jam`;
    document.getElementById("statAvailableHours").textContent = `${Math.max(totalCap - bookedHours, 0)} jam`;
    document.getElementById("scheduleHeading").textContent = `Jadwal Lapangan • ${P.dayName(selectedDay)}`;

    slotListEl.innerHTML = "";
    const renderedBookingIds = new Set();

    for (let h = P.OPEN_HOUR; h < P.CLOSE_HOUR; h++) {
      visibleCourts.forEach((court) => {
        const booking = dayBookings.find(
          (b) => b.courtId === court.id && h >= b.startHour && h < b.startHour + b.durationHours
        );
        if (booking) {
          if (statusFilter !== "AVAILABLE" && !renderedBookingIds.has(booking.id)) {
            renderedBookingIds.add(booking.id);
            const item = document.createElement("div");
            item.className = "slot-item booked";
            const waConfirmUrl = P.buildWhatsAppUrl(
              store.venue.phone,
              `Halo Admin ${store.venue.name}, konfirmasi jadwal ${booking.courtName} (${P.dayName(booking.dayOfWeek)}, ${P.formatRange(booking.startHour, booking.durationHours)}) atas nama ${booking.customerName}.`
            );
            item.innerHTML = `
              <div style="display:flex;align-items:center;gap:12px;flex-wrap:wrap">
                <span class="time-badge mono">${P.formatRange(booking.startHour, booking.durationHours)}</span>
                <div>
                  <div style="font-weight:800">${booking.courtName} • ${booking.customerName}</div>
                  <span class="status-pill status-booked">Sudah Dipesan</span>
                </div>
              </div>
              <a href="${waConfirmUrl}" target="_blank" rel="noopener" class="btn btn-outline">Konfirmasi WA</a>
            `;
            slotListEl.appendChild(item);
          }
        } else if (statusFilter !== "BOOKED") {
          const item = document.createElement("div");
          item.className = "slot-item";
          item.innerHTML = `
            <div style="display:flex;align-items:center;gap:12px;flex-wrap:wrap">
              <span class="time-badge mono">${P.formatRange(h, 1)}</span>
              <div>
                <div style="font-weight:800">${court.name}</div>
                <div style="font-size:13px;color:var(--paid-fg);font-weight:700">Tersedia • ${P.formatRupiah(court.rate)}/jam</div>
              </div>
            </div>
            <button type="button" class="btn btn-primary">Pesan Jam Ini</button>
          `;
          item.querySelector("button").onclick = () => {
            selectedCourtMode = String(court.id);
            inputDayEl.value = String(selectedDay);
            inputStartEl.value = P.formatHour(h);
            inputEndEl.value = P.formatHour(Math.min(h + 1, P.CLOSE_HOUR));
            renderFilters();
            updatePricePreview();
            document.getElementById("form-booking").scrollIntoView({ behavior: "smooth" });
          };
          slotListEl.appendChild(item);
        }
      });
    }
  }

  function renderWeekly() {
    weeklyListEl.innerHTML = "";
    const activeCourts = store.courts.filter((c) => c.active);
    P.DAYS.forEach((d) => {
      const dayBookings = store.bookings
        .filter((b) => b.dayOfWeek === d.id)
        .sort((a, b) => a.startHour - b.startHour);
      const bookedSummary =
        dayBookings.length === 0
          ? "Belum ada jadwal yang dipesan"
          : dayBookings
              .map((b) => `• ${b.courtName} (${P.formatRange(b.startHour, b.durationHours)}) — ${b.customerName}`)
              .join("<br/>");
      const availSummary = activeCourts
        .map((c) => `• ${c.name}: ${P.formatAvailableRangesSummary(dayBookings.filter((b) => b.courtId === c.id))}`)
        .join("<br/>");

      const card = document.createElement("div");
      card.className = "card";
      card.innerHTML = `
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:10px;gap:8px;flex-wrap:wrap">
          <strong style="font-size:17px">${d.name}</strong>
          <button type="button" class="btn btn-outline">Lihat / Pesan</button>
        </div>
        <div class="grid-2">
          <div class="stat-box stat-booked">
            <div style="font-size:12px;font-weight:800;margin-bottom:4px">SUDAH DIPESAN (${dayBookings.length})</div>
            <div style="font-size:13px">${bookedSummary}</div>
          </div>
          <div class="stat-box stat-available">
            <div style="font-size:12px;font-weight:800;margin-bottom:4px">JAM TERSEDIA</div>
            <div style="font-size:13px">${availSummary}</div>
          </div>
        </div>
      `;
      card.querySelector("button").onclick = () => {
        selectedDay = d.id;
        inputDayEl.value = String(d.id);
        renderAll();
        switchTab("jadwal");
      };
      weeklyListEl.appendChild(card);
    });
  }

  function renderHistory() {
    const chipsEl = document.getElementById("historyFilterChips");
    chipsEl.innerHTML = "";
    [{ id: 0, name: "Semua Lapangan" }, ...store.courts].forEach((c) => {
      const btn = document.createElement("button");
      btn.type = "button";
      btn.className = `chip ${historyCourtFilter === c.id ? "active" : ""}`;
      btn.textContent = c.name;
      btn.onclick = () => { historyCourtFilter = c.id; renderHistory(); };
      chipsEl.appendChild(btn);
    });

    historyListEl.innerHTML = "";
    const list = store.history.filter((h) => historyCourtFilter === 0 || h.courtId === historyCourtFilter);
    if (list.length === 0) {
      historyListEl.innerHTML = `<p style="color:var(--muted)">Belum ada riwayat pemesanan.</p>`;
      return;
    }
    list.forEach((h) => {
      const row = document.createElement("div");
      row.className = "slot-item";
      row.innerHTML = `
        <div>
          <div style="font-weight:800">${h.customerName} — ${h.courtName}</div>
          <div style="font-size:13px;color:var(--muted)">${P.dayName(h.dayOfWeek)}, ${P.formatRange(h.startHour, h.durationHours)} (${h.durationHours} jam)</div>
        </div>
        <div style="text-align:right">
          <div class="mono" style="font-weight:800">${P.formatRupiah(h.totalPrice)}</div>
          <span class="status-pill ${h.status === "CANCELLED" ? "status-booked" : "status-paid"}">${h.status === "CANCELLED" ? "Dibatalkan" : "Tercatat"}</span>
        </div>
      `;
      historyListEl.appendChild(row);
    });
  }

  function renderAll() {
    store = P.loadStore();
    renderVenueLinks();
    renderDayStrip();
    renderFilters();
    renderSlots();
    updatePricePreview();
  }

  ["input", "change"].forEach((ev) => {
    inputDayEl.addEventListener(ev, updatePricePreview);
    inputStartEl.addEventListener(ev, updatePricePreview);
    inputEndEl.addEventListener(ev, updatePricePreview);
  });

  document.getElementById("renterBookingForm").addEventListener("submit", (e) => {
    e.preventDefault();
    store = P.loadStore();
    const courts = selectedCourtsFromMode();
    const day = Number(inputDayEl.value);
    const startHour = P.parseManualHour(inputStartEl.value, false);
    const endHour = P.parseManualHour(inputEndEl.value, true);
    const name = document.getElementById("inputCustomerName").value;
    const phone = document.getElementById("inputCustomerPhone").value;
    const notes = document.getElementById("inputNotes").value;

    const res = P.createBooking({
      store,
      courtIds: courts.map((c) => c.id),
      dayOfWeek: day,
      startHour,
      endHour,
      customerName: name,
      customerPhone: phone,
      paymentStatus: "UNPAID",
      paidAmount: 0,
      notes,
    });

    if (res.error) {
      P.showToast(res.error);
      return;
    }

    const totalCombined = res.bookings.reduce((sum, b) => sum + b.totalPrice, 0);
    const courtNames = res.bookings.map((b) => b.courtName).join(" & ");
    const summary = `${courtNames} • ${P.dayName(day)}, ${P.formatRange(startHour, endHour - startHour)}`;
    const waText = `Halo Admin ${store.venue.name} (${store.venue.phone}), saya ${name.trim()} ingin konfirmasi pemesanan:\n• Jadwal: ${summary}\n• Total: ${P.formatRupiah(totalCombined)}`;

    selectedDay = day;
    document.getElementById("renterBookingForm").reset();
    inputStartEl.value = "18:00";
    inputEndEl.value = "20:00";
    inputDayEl.value = String(selectedDay);
    renderAll();
    P.showToast(`Jadwal ${courtNames} tersimpan! Membuka WhatsApp konfirmasi...`);
    window.open(P.buildWhatsAppUrl(store.venue.phone, waText), "_blank", "noopener");
  });

  renderAll();
})();
