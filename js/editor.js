(function () {
  const P = window.ManisrejoPortal;
  const SESSION_KEY = "manisrejo_editor_unlocked";
  let store = P.loadStore();

  const pinGateEl = document.getElementById("pinGateSection");
  const workspaceEl = document.getElementById("editorWorkspace");
  const lockBtnEl = document.getElementById("lockEditorBtn");

  function checkAuth() {
    const unlocked = sessionStorage.getItem(SESSION_KEY) === "1";
    pinGateEl.classList.toggle("hidden", unlocked);
    workspaceEl.classList.toggle("hidden", !unlocked);
    lockBtnEl.classList.toggle("hidden", !unlocked);
    if (unlocked) renderAll();
  }

  document.getElementById("pinLoginForm").addEventListener("submit", (e) => {
    e.preventDefault();
    store = P.loadStore();
    const entered = document.getElementById("pinInput").value.trim();
    if (entered === String(store.editorPin || "1234")) {
      sessionStorage.setItem(SESSION_KEY, "1");
      document.getElementById("pinInput").value = "";
      checkAuth();
      P.showToast("Mode Editor Website aktif.");
    } else {
      P.showToast("PIN Editor salah. Default: 1234.");
    }
  });

  lockBtnEl.onclick = () => {
    sessionStorage.removeItem(SESSION_KEY);
    checkAuth();
    P.showToast("Editor dikunci.");
  };

  function renderSummary() {
    const paid = store.bookings.reduce((s, b) => s + Number(b.paidAmount || 0), 0);
    const pending = store.bookings
      .filter((b) => b.paymentStatus !== "PAID")
      .reduce((s, b) => s + Math.max(b.totalPrice - b.paidAmount, 0), 0);
    document.getElementById("edPaidTotal").textContent = P.formatRupiah(paid);
    document.getElementById("edPendingTotal").textContent = P.formatRupiah(pending);
    document.getElementById("edBookingCount").textContent = `${store.bookings.length} jadwal`;
  }

  function renderAdminFormSelects() {
    const courtSel = document.getElementById("edBookCourt");
    const daySel = document.getElementById("edBookDay");
    courtSel.innerHTML = "";
    store.courts.filter((c) => c.active).forEach((c) => {
      const opt = document.createElement("option");
      opt.value = c.id;
      opt.textContent = `${c.name} (${P.formatRupiah(c.rate)}/jam)`;
      courtSel.appendChild(opt);
    });
    if (store.courts.filter((c) => c.active).length >= 2) {
      const bothOpt = document.createElement("option");
      bothOpt.value = "BOTH";
      bothOpt.textContent = "2 Lapangan Sekaligus (A & B)";
      courtSel.appendChild(bothOpt);
    }
    if (daySel.options.length === 0) {
      P.DAYS.forEach((d) => {
        const opt = document.createElement("option");
        opt.value = d.id;
        opt.textContent = d.name;
        daySel.appendChild(opt);
      });
      daySel.value = String(P.todayIsoDay());
    }
  }

  function renderBookings() {
    const listEl = document.getElementById("editorBookingsList");
    listEl.innerHTML = "";
    if (store.bookings.length === 0) {
      listEl.innerHTML = `<p style="color:var(--muted)">Belum ada jadwal pemesanan aktif.</p>`;
      return;
    }
    const sorted = [...store.bookings].sort((a, b) => a.dayOfWeek - b.dayOfWeek || a.startHour - b.startHour);
    sorted.forEach((b) => {
      const remaining = Math.max(b.totalPrice - b.paidAmount, 0);
      const statusClass = b.paymentStatus === "PAID" ? "status-paid" : "status-pending";
      const statusLabel = b.paymentStatus === "PAID" ? "Lunas" : b.paymentStatus === "DP" ? "DP Sebagian" : "Belum Bayar";

      const card = document.createElement("div");
      card.className = "slot-item";
      card.innerHTML = `
        <div style="flex:1 1 260px">
          <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap">
            <span class="time-badge mono">${P.dayName(b.dayOfWeek)} • ${P.formatRange(b.startHour, b.durationHours)}</span>
            <span class="status-pill ${statusClass}">${statusLabel}</span>
          </div>
          <div style="font-weight:800;margin-top:6px">${b.customerName} — ${b.courtName}</div>
          <div style="font-size:13px;color:var(--muted)">
            HP: ${b.customerPhone || "-"} • Total: <strong class="mono">${P.formatRupiah(b.totalPrice)}</strong>
            ${remaining > 0 ? ` • Sisa: <strong class="mono">${P.formatRupiah(remaining)}</strong>` : ""}
          </div>
        </div>
        <div class="chip-row">
          ${b.paymentStatus !== "PAID" ? `<button type="button" class="btn btn-primary" data-act="paid">Tandai Lunas</button>` : ""}
          ${b.paymentStatus !== "PAID" ? `<button type="button" class="btn btn-warning" data-act="remind">Pengingat WA (${b.reminderSentCount || 0}x)</button>` : ""}
          <button type="button" class="btn btn-danger" data-act="cancel">Batalkan</button>
        </div>
      `;

      const paidBtn = card.querySelector('[data-act="paid"]');
      if (paidBtn) {
        paidBtn.onclick = () => {
          b.paymentStatus = "PAID";
          b.paidAmount = b.totalPrice;
          P.saveStore(store);
          P.showToast(`${b.customerName} ditandai Lunas.`);
          renderAll();
        };
      }

      const remindBtn = card.querySelector('[data-act="remind"]');
      if (remindBtn) {
        remindBtn.onclick = () => {
          b.reminderSentCount = (b.reminderSentCount || 0) + 1;
          P.saveStore(store);
          const msg = `Halo ${b.customerName}, pengingat pembayaran sewa ${b.courtName} (${P.dayName(b.dayOfWeek)}, ${P.formatRange(b.startHour, b.durationHours)}) sebesar ${P.formatRupiah(remaining)}. Terima kasih — ${store.venue.name}.`;
          P.showToast(`Pengingat ke-${b.reminderSentCount} dicatat!`);
          if (b.customerPhone) window.open(P.buildWhatsAppUrl(b.customerPhone, msg), "_blank", "noopener");
          renderAll();
        };
      }

      card.querySelector('[data-act="cancel"]').onclick = () => {
        store.bookings = store.bookings.filter((item) => item.id !== b.id);
        store.history.unshift({
          id: Date.now(),
          courtId: b.courtId,
          courtName: b.courtName,
          dayOfWeek: b.dayOfWeek,
          startHour: b.startHour,
          durationHours: b.durationHours,
          customerName: b.customerName,
          totalPrice: b.totalPrice,
          status: "CANCELLED",
          createdAt: Date.now(),
        });
        P.saveStore(store);
        P.showToast(`Jadwal ${b.courtName} milik ${b.customerName} dibatalkan.`);
        renderAll();
      };

      listEl.appendChild(card);
    });
  }

  function renderCourts() {
    const gridEl = document.getElementById("editorCourtsGrid");
    gridEl.innerHTML = "";
    store.courts.forEach((c) => {
      const box = document.createElement("div");
      box.className = "card";
      box.style.boxShadow = "none";
      box.innerHTML = `
        <div class="form-grid">
          <div class="field">
            <label>Nama Lapangan</label>
            <input type="text" value="${c.name}" data-field="name" />
          </div>
          <div class="field">
            <label>Tarif Sewa per Jam (Rp)</label>
            <input type="number" min="0" step="1000" value="${c.rate}" data-field="rate" />
          </div>
          <label style="display:flex;align-items:center;gap:8px;font-weight:700;cursor:pointer">
            <input type="checkbox" ${c.active ? "checked" : ""} data-field="active" />
            Lapangan Aktif (Tampil di Halaman Orang Pesan)
          </label>
          <button type="button" class="btn btn-primary">Simpan Perubahan ${c.name}</button>
        </div>
      `;
      box.querySelector("button").onclick = () => {
        c.name = box.querySelector('[data-field="name"]').value.trim() || c.name;
        c.rate = Number(box.querySelector('[data-field="rate"]').value || 15000);
        c.active = box.querySelector('[data-field="active"]').checked;
        P.saveStore(store);
        P.showToast(`Pengaturan ${c.name} disimpan.`);
        renderAll();
      };
      gridEl.appendChild(box);
    });
  }

  function renderVenueForm() {
    document.getElementById("edVenueName").value = store.venue.name;
    document.getElementById("edVenuePhone").value = store.venue.phone;
    document.getElementById("edVenueMaps").value = store.venue.mapsUrl;
    document.getElementById("edNewPin").value = store.editorPin || "1234";
  }

  function renderAll() {
    store = P.loadStore();
    renderSummary();
    renderAdminFormSelects();
    renderBookings();
    renderCourts();
    renderVenueForm();
  }

  document.getElementById("editorBookingForm").addEventListener("submit", (e) => {
    e.preventDefault();
    store = P.loadStore();
    const courtVal = document.getElementById("edBookCourt").value;
    const activeCourts = store.courts.filter((c) => c.active);
    const courtIds = courtVal === "BOTH" ? activeCourts.slice(0, 2).map((c) => c.id) : [Number(courtVal)];
    const res = P.createBooking({
      store,
      courtIds,
      dayOfWeek: Number(document.getElementById("edBookDay").value),
      startHour: P.parseManualHour(document.getElementById("edBookStart").value, false),
      endHour: P.parseManualHour(document.getElementById("edBookEnd").value, true),
      customerName: document.getElementById("edBookName").value,
      customerPhone: document.getElementById("edBookPhone").value,
      paymentStatus: document.getElementById("edBookStatus").value,
      paidAmount: Number(document.getElementById("edBookDp").value || 0),
      notes: "Input Editor Website",
    });
    if (res.error) {
      P.showToast(res.error);
      return;
    }
    document.getElementById("editorBookingForm").reset();
    document.getElementById("edBookStart").value = "18:00";
    document.getElementById("edBookEnd").value = "20:00";
    document.getElementById("edBookDp").value = "10000";
    P.showToast("Jadwal admin berhasil disimpan.");
    renderAll();
  });

  document.getElementById("addCourtBtn").onclick = () => {
    store = P.loadStore();
    const nextIdx = store.courts.length;
    const nextName = nextIdx < 26 ? `Lapangan ${String.fromCharCode(65 + nextIdx)}` : `Lapangan ${nextIdx + 1}`;
    store.courts.push({ id: Date.now(), name: nextName, rate: 15000, active: true, note: "" });
    P.saveStore(store);
    P.showToast(`${nextName} ditambahkan.`);
    renderAll();
  };

  document.getElementById("venueEditorForm").addEventListener("submit", (e) => {
    e.preventDefault();
    store.venue.name = document.getElementById("edVenueName").value.trim() || store.venue.name;
    store.venue.phone = document.getElementById("edVenuePhone").value.trim() || store.venue.phone;
    store.venue.mapsUrl = document.getElementById("edVenueMaps").value.trim() || store.venue.mapsUrl;
    store.editorPin = document.getElementById("edNewPin").value.trim() || "1234";
    P.saveStore(store);
    P.showToast("Informasi website & PIN berhasil disimpan.");
    renderAll();
  });

  document.getElementById("exportJsonBtn").onclick = () => {
    const blob = new Blob([JSON.stringify(store, null, 2)], { type: "application/json" });
    const a = document.createElement("a");
    a.href = URL.createObjectURL(blob);
    a.download = "manisrejo-badminton-backup.json";
    a.click();
  };

  document.getElementById("resetDefaultBtn").onclick = () => {
    store = P.resetStore();
    P.showToast("Data website dikembalikan ke pengaturan awal.");
    renderAll();
  };

  checkAuth();
})();
