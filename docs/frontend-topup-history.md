# Prompt Frontend — Fitur History Top-Up

> Dioper ke tim Frontend. Backend Pembayaran (`mysawit-pembayaran`) sudah expose endpoint listing top-up dengan filter di branch `feat/topup-history-filters`.

---

## Konteks

Admin Utama melakukan top-up wallet via Xendit. Saat ini frontend cuma bisa lihat list top-up tanpa filter. Tugas: bikin halaman **History Top-Up** yang menampilkan transaksi top-up admin dengan filter status & rentang tanggal.

## Endpoint yang dipakai

### List history top-up (sudah ada, dengan filter baru)

```
GET /api/pembayaran/wallet/topup
```

Headers wajib (dari API Gateway / auth flow yang existing):
- `X-User-Id: <UUID admin>`
- `X-User-Role: ADMIN`

Query params (semua opsional, kombinasi bebas):

| Param | Tipe | Contoh | Keterangan |
| --- | --- | --- | --- |
| `status` | `PENDING` \| `SUCCESS` \| `FAILED` | `?status=SUCCESS` | Filter berdasarkan status transaksi |
| `startDate` | ISO-8601 datetime | `?startDate=2026-01-01T00:00:00` | Inklusif. Hanya transaksi `createdAt >= startDate` |
| `endDate` | ISO-8601 datetime | `?endDate=2026-12-31T23:59:59` | Inklusif. Hanya transaksi `createdAt <= endDate` |

Tanpa filter = return semua top-up milik user yang login, sorted by `createdAt` descending.

Contoh:
```
GET /api/pembayaran/wallet/topup?status=SUCCESS&startDate=2026-05-01T00:00:00&endDate=2026-05-31T23:59:59
```

### Response shape

```json
[
  {
    "id": "uuid",
    "userId": "uuid",
    "amountRupiah": 100000.00,
    "amountSawitDollar": 10.00,
    "paymentGatewayRef": "string",
    "paymentUrl": null,
    "status": "SUCCESS",
    "createdAt": "2026-05-20T14:32:10"
  }
]
```

> Catatan: `paymentUrl` hanya berisi nilai saat top-up baru diinisiasi (POST). Di response listing nilainya `null`.

### Endpoint pendukung (sudah ada)

- `GET /api/pembayaran/wallet/topup/{id}` — detail satu transaksi
- `POST /api/pembayaran/wallet/topup/{id}/sync` — paksa sync status ke Xendit (tombol "Refresh status" buat row PENDING)

## Spesifikasi UI yang diharapkan

1. **Halaman History Top-Up** (admin only)
   - Tabel kolom: Tanggal, Nominal Rupiah, Nominal SawitDollar, Status (badge: kuning PENDING / hijau SUCCESS / merah FAILED), Aksi.
   - Sort default: terbaru di atas (sudah default dari backend).
2. **Filter bar di atas tabel**:
   - Dropdown Status: `Semua`, `PENDING`, `SUCCESS`, `FAILED`. Default `Semua` (tidak kirim param).
   - Date range picker: `startDate` & `endDate`. Default kosong.
   - Tombol "Terapkan" → re-fetch dengan query params.
   - Tombol "Reset" → kosongin filter, re-fetch tanpa params.
3. **Aksi per row**:
   - Kalau status `PENDING` → tampilkan tombol "Refresh status" yang call `POST /api/pembayaran/wallet/topup/{id}/sync`, lalu refresh list.
   - Klik row → buka detail (call `GET .../{id}`).
4. **Empty state**: "Tidak ada riwayat top-up sesuai filter."
5. **Format**:
   - `amountRupiah`: `Rp 100.000` (locale ID).
   - `amountSawitDollar`: `10 SawitDollar`.
   - `createdAt`: `20 Mei 2026, 14:32`.

## Format tanggal yang harus dikirim ke backend

Backend pakai `@DateTimeFormat(iso = ISO.DATE_TIME)` → harus ISO-8601 **tanpa timezone offset**:

✅ `2026-05-22T00:00:00`
❌ `2026-05-22` (akan 400)
❌ `2026-05-22T00:00:00Z` (akan 400)

Tip: kalau pakai date picker yang return `Date` JS, format dulu via `date.toISOString().slice(0, 19)` atau pakai `date-fns` `format(date, "yyyy-MM-dd'T'HH:mm:ss")`.

## Error handling

- `401` → token / headers auth invalid. Redirect ke login.
- `403` → user bukan admin. Sembunyikan menu History Top-Up.
- `400` → format query param salah (biasanya `startDate`/`endDate`). Tampilkan toast.

## Acceptance

- [ ] Halaman bisa load list tanpa filter
- [ ] Filter status berfungsi (3 nilai + "Semua")
- [ ] Filter date range berfungsi (inklusif start & end)
- [ ] Kombinasi filter status + date range berfungsi
- [ ] Tombol "Refresh status" muncul hanya untuk PENDING dan call endpoint sync
- [ ] Empty state muncul saat hasil kosong
