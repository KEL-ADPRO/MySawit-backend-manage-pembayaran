# Profiling — Submission

Dokumen ini merangkum bukti profiling, justifikasi metode yang dipakai, dan
analisis improvement untuk service `mysawit-pembayaran`.

## 1. Link Bukti Profiling

Semua artefak profiling tersimpan di dalam repository:

| Artefak | Lokasi |
|---|---|
| Test plan JMeter (reusable) | [monitoring/jmeter/mysawit-load-test.jmx](../monitoring/jmeter/mysawit-load-test.jmx) |
| Raw hasil run baseline (`.jtl`) | [profiling/jmeter/20260521-221532/results.jtl](../profiling/jmeter/20260521-221532/results.jtl) |
| HTML report JMeter (dashboard, percentiles, grafik) | [profiling/jmeter/20260521-221532/report/index.html](../profiling/jmeter/20260521-221532/report/index.html) |
| Ringkasan numerik per-endpoint | [profiling/jmeter/20260521-221532/report/statistics.json](../profiling/jmeter/20260521-221532/report/statistics.json) |
| Pointer ke run terbaru | [profiling/jmeter/latest-run.txt](../profiling/jmeter/latest-run.txt) |
| Dashboard Grafana (Prometheus metrics) | [monitoring/grafana/dashboards/mysawit-pembayaran.json](../monitoring/grafana/dashboards/mysawit-pembayaran.json) |
| Konfigurasi Prometheus + alert rules | [monitoring/prometheus/](../monitoring/prometheus/) |
| Panduan menjalankan profiling end-to-end | [docs/observability.md](observability.md) |

Cara membuka report HTML secara lokal:

```bash
open profiling/jmeter/20260521-221532/report/index.html
```

### Ringkasan Hasil Baseline

Konfigurasi run: **20 threads, ramp 20s, durasi 60s, think time 100ms**, target
`http://localhost:18085`. Beban mixed traffic 3 endpoint:

| Endpoint | Samples | Mean (ms) | p95 (ms) | p99 (ms) | Throughput (req/s) | Error % |
|---|---:|---:|---:|---:|---:|---:|
| `GET /actuator/health` | 3.126 | 4,20 | 10 | 29 | 52,29 | 0,00 |
| `GET /api/pembayaran/wage-config` | 3.120 | 5,48 | 12 | 32 | 52,36 | 0,00 |
| `GET /api/pembayaran/wallet/me` | 3.114 | 5,83 | 13 | ~32 | 52,40 | 0,00 |
| **Total** | **9.360** | **5,17** | **12** | **31** | **156,54** | **0,00** |

(angka diambil langsung dari `statistics.json`)

## 2. Justifikasi Proses Profiling

Profiling dibagi menjadi tiga lapis yang saling melengkapi — masing-masing
menjawab pertanyaan berbeda dan dipilih karena alasan teknis spesifik, bukan
karena "biar lengkap".

### 2.1 Load Generation — Apache JMeter

**Alat:** Apache JMeter 5.6.3 (CLI mode, non-GUI).

**Mengapa JMeter:**

- *Open source, tidak vendor lock-in*, sehingga test plan tetap dapat
  dijalankan oleh anggota tim lain tanpa lisensi.
- Sudah lazim dipakai untuk *HTTP load testing* dan menghasilkan HTML report
  standar (`-e -o`) yang menyajikan throughput, percentile, dan response-time
  over time — cukup sebagai bukti tertulis.
- CLI mode (`-n`) menghasilkan overhead jauh lebih kecil dibanding GUI mode,
  yang adalah praktik resmi yang disarankan oleh JMeter sendiri untuk
  pengukuran beban.
- Test plan disimpan sebagai `.jmx` (XML) sehingga *version-controlled* dan
  dapat di-replay (lihat `monitoring/jmeter/mysawit-load-test.jmx`).

**Mengapa parameter beban dipilih seperti itu:**

- *20 thread* dengan *ramp 20 detik* — kenaikan beban bertahap, satu thread
  baru per detik, untuk menghindari *cold-start spike* yang akan mencemari
  pengukuran p95/p99.
- *Durasi 60 detik* — cukup panjang agar JIT warm-up, HikariCP pool
  stabil, dan rate metrik Prometheus (window `1m` / `5m`) memiliki data yang
  bermakna.
- *Think time 100ms* — mensimulasikan klien nyata (bukan *infinite loop*),
  sehingga pola request mendekati traffic produksi.
- *Mixed traffic 3 endpoint* — `health` (sanity baseline tanpa DB),
  `wage-config` (read DB ringan), dan `wallet/me` (read DB per-user) — dipilih
  agar profil mencakup *non-DB*, *DB shared-read*, dan *DB per-user-read*
  sekaligus dalam satu run.
- Semua parameter diekspos via `-J` flag (`host`, `port`, `threads`, `ramp`,
  `duration`, `think_ms`, `user_id`) supaya skenario lain (50 user, 180s, dll)
  cukup mengganti flag tanpa edit `.jmx`.

### 2.2 Continuous Observability — Prometheus + Grafana

**Alat:** Spring Boot Actuator + Micrometer Prometheus registry → Prometheus
(scrape `/actuator/prometheus`) → dashboard Grafana ter-provision.

**Mengapa stack ini, bukan hanya report JMeter:**

- JMeter mengukur dari **sisi klien** — ia tidak tahu CPU JVM, GC pause,
  jumlah thread, koneksi HikariCP, atau memory area mana yang penuh. Tanpa
  metrik server-side, kita tidak tahu *kenapa* sebuah angka p95 bergerak.
- Micrometer + Prometheus adalah jalur metrik *de facto* untuk Spring Boot,
  cukup tambah dependency `micrometer-registry-prometheus` tanpa instrumentasi
  manual — *low overhead*, *push-free*, *pull-based scrape* tiap interval pendek.
- Dashboard Grafana `MySawit Pembayaran` di-provisioning lewat file (folder
  `monitoring/grafana/`), sehingga *reproducible* dan bukan setup manual di UI.
- Panel dashboard sengaja menutup 4 dimensi penting yang relevan saat load
  test: **HTTP** (throughput, error rate, p95), **JVM** (memory area, thread
  count, CPU), **DB pool** (active / idle / pending HikariCP). Tiga dimensi
  ini adalah penyebab umum bottleneck pada Spring Boot REST service.
- Alert rules dasar (service down, 5xx tinggi, p95 tinggi, heap tinggi) sudah
  disiapkan di `monitoring/prometheus/alert-rules.yml` agar metrik tidak
  hanya pasif tetapi juga *actionable*.

### 2.3 Code-level Profiler — Java Flight Recorder (JFR)

**Alat yang dipilih:** Java Flight Recorder (JFR) + Java Mission Control,
seperti diatur di [docs/observability.md §Profiling](observability.md#profiling).

**Mengapa JFR sebagai profiler default:**

- *Built-in* di OpenJDK ≥ 11, **tidak butuh agent tambahan** dan tidak butuh
  rebuild image — cukup `jcmd <pid> JFR.start ...` atau JVM flag
  `-XX:StartFlightRecording=...`.
- *Overhead sangat rendah* (umumnya < 2 % pada settings `profile`), aman
  untuk dijalankan paralel dengan load test JMeter tanpa mendistorsi hasil
  pengukuran latency.
- Menghasilkan *file `.jfr` self-contained* yang dapat dibuka offline di
  Java Mission Control — menampilkan CPU hot method, allocation hot spot,
  GC pause, lock contention, dan I/O blocking dalam satu tool.
- Asynchronous-profiler / py-spy / perf adalah alternatif yang lebih dalam,
  tetapi JFR sudah cukup untuk menemukan *low-hanging fruit*. Kita baru
  beralih ke async-profiler bila JFR tidak menunjukkan masalah yang jelas
  padahal metrik Grafana mengindikasikan kontensi.

### 2.4 Alur Profiling End-to-End

```
   ┌─────────┐   load    ┌──────────────────┐   /actuator/prometheus
   │ JMeter  │ ────────▶ │ mysawit-pembayaran│ ◀──── Prometheus (scrape)
   └─────────┘           └──────────────────┘             │
        │                       ▲                          ▼
        │ HTML report           │ JFR.start                Grafana dashboard
        ▼                       │                          (HTTP / JVM / DB pool)
   statistics.json         profiling/*.jfr
```

Urutan eksekusi: (1) start app + monitoring stack, (2) start JFR recording
pada PID app, (3) jalankan JMeter dengan parameter target, (4) selama run
amati dashboard Grafana, (5) setelah selesai dump JFR + buka HTML report
JMeter, lalu korelasi: angka p95 dari JMeter ↔ panel `HTTP p95 Latency`
Grafana ↔ hot method dari JFR.

## 3. Analisis Improvement

Beberapa observasi berikut **berasal dari kode aktual** dan dari karakteristik
beban baseline. Setiap item disertai justifikasi *kenapa* perlu diperbaiki.

### 3.1 Tambahkan Caching untuk Endpoint `GET /api/pembayaran/wage-config`

**Temuan.** [WageConfigServiceImpl.getWageConfig()](../src/main/java/com/mysawit/pembayaran/service/WageConfigServiceImpl.java#L22-L34)
mengeksekusi `findFirstByOrderByUpdatedAtDesc()` pada **setiap** request.
Pada baseline, endpoint ini sendiri menerima ~52 req/s — artinya ~52 query
`ORDER BY updated_at DESC LIMIT 1` per detik untuk data yang nyaris tidak
pernah berubah.

**Improvement.** Tambahkan Spring Cache (`@Cacheable`) dengan TTL pendek
(misal 60s) atau invalidasi eksplisit di `updateWageConfig`. Setelah caching,
endpoint ini menjadi memory lookup dan beban DB hilang sepenuhnya.

### 3.2 `getWalletByUserId` Memakai `@Transactional` Write, Bukan Read-Only

**Temuan.** [WalletServiceImpl.getWalletByUserId()](../src/main/java/com/mysawit/pembayaran/service/WalletServiceImpl.java#L26-L31)
dianotasi `@Transactional` (default writable). Untuk *read path*, ini lebih
mahal — Hibernate tetap melakukan *dirty-check flush* di akhir transaksi.

**Improvement.** Ubah menjadi `@Transactional(readOnly = true)` untuk path
baca. Method `createWallet` yang menulis sudah dilindungi `@Transactional`
sendiri sehingga *fallback* tetap aman.

### 3.3 Potensi Race Saat Wallet Otomatis Dibuat

**Temuan.** `getWalletByUserId` → `createWallet` → `findByUserId` lagi → save.
Saat beberapa request masuk paralel untuk user yang belum punya wallet, dua
thread bisa lolos cek dan dua-duanya memanggil `save`. Saat ini kemungkinan
diselamatkan oleh `unique constraint` di kolom `user_id`, tetapi salah satu
request akan gagal dengan `DataIntegrityViolationException`.

**Improvement.** Pakai *upsert*/`INSERT ... ON CONFLICT DO NOTHING` di
repository, atau tangkap `DataIntegrityViolationException` dan re-fetch.
Sebagai alternatif, pisahkan endpoint *get-only* dari endpoint
*provision-if-missing*.

### 3.4 JMeter Baseline Belum Mencerminkan Beban Saturasi

**Temuan.** Baseline 20 thread × 100ms think time hanya menghasilkan ~156
req/s total dan p95 12 ms — sistem belum dibuat kewalahan. Tanpa run
saturasi, kita tidak tahu *batas* kapasitas service.

**Improvement.** Tambahkan dua skenario tambahan menggunakan `.jmx` yang sama:

```bash
# Stress: 100 user, 5 menit
jmeter -n -t monitoring/jmeter/mysawit-load-test.jmx \
  -Jthreads=100 -Jramp=60 -Jduration=300 -Jthink_ms=0 \
  -l profiling/jmeter/stress/results.jtl \
  -e -o profiling/jmeter/stress/report

# Soak: 30 user, 30 menit, untuk mengecek leak
jmeter -n -t monitoring/jmeter/mysawit-load-test.jmx \
  -Jthreads=30 -Jramp=30 -Jduration=1800 \
  -l profiling/jmeter/soak/results.jtl \
  -e -o profiling/jmeter/soak/report
```

Soak test penting untuk membuktikan tidak ada *memory leak* atau *connection
leak* — sesuatu yang baseline 60 detik tidak bisa tangkap.

### 3.5 Run JFR Belum Tersedia Sebagai Artefak

**Temuan.** Folder `profiling/` saat ini hanya berisi hasil JMeter. Panduan
JFR sudah ada di `docs/observability.md` namun belum dieksekusi.

**Improvement.** Lakukan minimal satu recording JFR 120 detik **bersamaan
dengan run JMeter stress** dan simpan ke `profiling/jfr/`:

```bash
mkdir -p profiling/jfr
jcmd <pid> JFR.start name=mysawit settings=profile \
  filename=profiling/jfr/mysawit-pembayaran-$(date +%Y%m%d-%H%M%S).jfr \
  duration=120s
```

Hasilnya membuka analisis CPU hot method (misal: apakah `BigDecimal.setScale`
di `normalizeMoney` jadi hot), allocation rate, dan lock contention pada
`findWithLockingByUserId`.

### 3.6 Validasi HikariCP Sizing Lewat Dashboard

**Temuan.** Panel *Database Pool* sudah ada di dashboard (active / idle /
pending). Saat ini `pending` selalu nol karena beban ringan. Saat
stress/soak dijalankan, panel ini jadi acuan untuk menyesuaikan
`spring.datasource.hikari.maximum-pool-size`.

**Improvement.** Saat menjalankan skenario 3.4, capture nilai puncak
`hikaricp_connections_active` dan `hikaricp_connections_pending`. Jika
`pending > 0` muncul konsisten, naikkan ukuran pool secara bertahap;
sebaliknya, jika `active` jauh di bawah `max`, pool boleh diturunkan untuk
menghemat koneksi DB.

### 3.7 Lock Pesimistis Pada Path Tulis Wallet

**Temuan.** [getOrCreateWalletForUpdate](../src/main/java/com/mysawit/pembayaran/service/WalletServiceImpl.java#L74-L82)
memakai `findWithLockingByUserId` (pessimistic write lock). Untuk
*hot-key user* (misal satu user mendapat banyak callback top-up atau
deduct paralel), ini berpotensi menjadi titik serialisasi.

**Improvement.** Strategi pengukuran dulu, optimasi belakangan: jalankan
JMeter skenario yang *intentionally collide* pada satu `user_id` dan amati
`hikaricp_connections_pending` + p95 endpoint terkait. Bila terbukti
bottleneck, opsi mitigasi: (a) ganti ke *optimistic locking*
(`@Version`) dengan retry, atau (b) pindahkan akumulasi balance ke
update SQL ekspresif (`balance = balance + :amount`) yang menghilangkan
kebutuhan lock baris.

## 4. Ringkasan

- Bukti profiling sudah ada dalam repo (lihat tabel di §1).
- Pemilihan **JMeter + Prometheus/Grafana + JFR** mengikuti prinsip *layered
  profiling*: ukur dari klien, observasi di server, dan inspeksi di JVM.
- Tujuh poin improvement di §3 ditemukan dari kode dan konfigurasi aktual;
  prioritas tertinggi adalah caching `wage-config` (§3.1) dan menjalankan
  skenario stress + JFR recording (§3.4 dan §3.5) untuk mendapatkan data
  saturasi yang saat ini belum dimiliki.
