# Checklist gRPC yang Dibutuhkan Modul Pembayaran (per Modul)

Dokumen ini merangkum gRPC apa saja yang **modul Pembayaran** butuhkan **dari modul lain** agar alur payroll, wallet, dan approval Admin Utama berjalan utuh sesuai PDF proyek.

---

## ✅ Modul User/Auth harus menyediakan gRPC:

- [ ] `ValidateAdminRole(adminId)` → `bool valid`
- [ ] `ValidateUserRole(userId, expectedRole)` → `bool valid`
- [ ] `GetUserById(userId)` → `UserSummary` (minimal: `id`, `nama`, `email`, `role`)
- [ ] `GetUsersByIds(List<userId>)` → `List<UserSummary>` (untuk listing payroll bulk)
- [ ] Response `UserSummary` wajib include `role` (enum: `ADMIN`, `BURUH`, `MANDOR`, `SUPIR_TRUK`)
- [ ] Jalan di port `9091`

**Kenapa Pembayaran butuh ini:**
- Verifikasi approver payroll benar-benar Admin Utama sebelum debit wallet Admin.
- Resolve `X-User-Id` yang masuk dari header jadi profil lengkap (nama, email) untuk tampilan daftar payroll & wallet.
- Validasi target payroll (Buruh/Supir/Mandor) memang user yang valid dan role-nya cocok sebelum payroll dibuat.

---

## ✅ Modul Harvest/Panen harus menyediakan gRPC:

- [ ] `GetHarvestById(harvestId)` → `HarvestSummary`
- [ ] `GetApprovedHarvests(List<harvestId>)` → `List<HarvestSummary>`
- [ ] Response wajib include field `approved` (bool), `weight_kg` (string), `buruh_user_id` (string), `harvested_at` (string)
- [ ] Jalan di port `9092`

**Kenapa Pembayaran butuh ini:**
- Cross-check `source_id` event payroll Buruh ke modul Harvest sebelum payroll BURUH dibuat (defensive, anti event palsu).
- Saat Admin Utama review payroll BURUH, Pembayaran perlu menampilkan ringkasan hasil panen sumber payroll-nya.

---

## ✅ Modul Pengiriman/Shipment harus menyediakan gRPC:

- [ ] `GetShipmentById(shipmentId)` → `ShipmentSummary`
- [ ] `GetRecognizedWeight(shipmentId)` → `string recognized_kg`
- [ ] Response wajib include field `status`, `supir_user_id`, `mandor_user_id`, `delivered_kg`, `recognized_kg`
- [ ] Mendukung query untuk payroll `role="SUPIR_TRUK"` (pakai `delivered_kg`) dan `role="MANDOR"` (pakai `recognized_kg`)
- [ ] Jalan di port `9094`

**Kenapa Pembayaran butuh ini:**
- Verifikasi `source_id` event payroll Supir/Mandor benar-benar shipment yang sudah disetujui Mandor/Admin.
- Untuk payroll MANDOR, Pembayaran perlu `recognized_kg` (kilogram yang diakui pabrik) yang bisa berbeda dari `delivered_kg` karena partial rejection oleh Admin Utama.
- Saat Admin review payroll, tampilkan detail shipment sumber payroll.

---

## Ringkasan Port

| Modul | Port gRPC |
| --- | --- |
| User/Auth | `9091` |
| Harvest/Panen | `9092` |
| Pembayaran (server-only, milik kita) | `9093` |
| Pengiriman/Shipment | `9094` |

---

## Catatan

- Modul **Kebun/Estate** tidak dibutuhkan langsung oleh Pembayaran. Validasi Mandor/Supir satu kebun dilakukan oleh modul Pengiriman & User, bukan Pembayaran.
- Modul **Notifikasi** opsional dari PDF, tidak masuk checklist wajib di atas.
- Saat ini Pembayaran masih membaca identitas user via header `X-User-Id` dan `X-User-Role` dari API Gateway. Begitu gRPC `ValidateAdminRole` & `GetUserById` tersedia, header tersebut harus diganti / divalidasi ulang via gRPC ke modul User.
