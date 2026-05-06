# Panduan Deployment ke Render

Dokumen ini menjelaskan prosedur deployment modul Manajemen Pembayaran MySawit
ke platform Render serta langkah pemulihan apabila terjadi kegagalan migrasi
skema basis data.

## 1. Ringkasan Konfigurasi

Aplikasi membaca konfigurasi runtime dari environment variable. Berkas
`src/main/resources/application.yml` telah diparametrisasi sehingga nilai
default cocok untuk pengembangan lokal sementara nilai produksi dapat
diinjeksikan oleh Render melalui dashboard.

| Environment Variable   | Default            | Keterangan                                                        |
|------------------------|--------------------|-------------------------------------------------------------------|
| `PORT`                 | `8085`             | Port HTTP yang dipakai Spring Boot. Render mengisi otomatis.      |
| `JDBC_DATABASE_URL`    | (lihat default)    | URL JDBC PostgreSQL Render (format `jdbc:postgresql://...`).      |
| `DATABASE_URL`         | (lihat default)    | Fallback bila `JDBC_DATABASE_URL` tidak tersedia.                 |
| `DB_USERNAME`          | `postgres`         | Username basis data.                                              |
| `DB_PASSWORD`          | `postgres`         | Password basis data.                                              |
| `JPA_DDL_AUTO`         | `update`           | Strategi DDL Hibernate. Set `create` sekali untuk reset skema.    |
| `XENDIT_API_KEY`       | (kosong)           | Kredensial Xendit untuk integrasi pembayaran.                     |

## 2. Konfigurasi Service di Render

1. Buat Web Service baru dari repositori GitHub (branch `staging` atau `main`).
2. Pilih environment **Docker**; Render akan otomatis membaca `Dockerfile`.
3. Tambahkan layanan PostgreSQL (Render Managed Postgres) pada region yang
   sama untuk menekan latensi koneksi.
4. Pada tab **Environment**, set variabel berikut:

   ```
   JDBC_DATABASE_URL=<JDBC URL dari panel Postgres Render>
   DB_USERNAME=<username Postgres Render>
   DB_PASSWORD=<password Postgres Render>
   JPA_DDL_AUTO=update
   XENDIT_API_KEY=<api key sandbox/production>
   ```

5. Render akan otomatis melakukan deploy setiap kali branch `staging` menerima
   push, dipicu melalui GitHub Actions (`ci.yml`) yang memanggil
   `RENDER_DEPLOY_HOOK_URL`.

## 3. Pemulihan Kegagalan Migrasi Skema (UUID Cast Error)

Hibernate dengan `ddl-auto=update` tidak dapat mengubah tipe kolom existing
menjadi `uuid` di PostgreSQL karena memerlukan klausa `USING id::uuid` yang
tidak digenerasi otomatis. Bila log Render menampilkan pesan berikut:

```
ERROR: column "id" cannot be cast automatically to type uuid
  Hint: You might need to specify "USING id::uuid".
```

maka skema yang ada tidak konsisten dengan entity terbaru dan perlu
direkonstruksi.

### 3.1 Opsi A — Reset Skema via Environment Variable (Direkomendasikan)

1. Buka dashboard Render → service backend → tab **Environment**.
2. Ubah nilai `JPA_DDL_AUTO` dari `update` menjadi `create`.
3. Klik **Save Changes**; Render akan men-trigger redeploy otomatis.
4. Tunggu hingga log menampilkan `Started PembayaranApplication ...` tanpa
   error DDL.
5. Kembalikan `JPA_DDL_AUTO` ke `update` lalu **Save Changes** sekali lagi
   supaya redeploy berikutnya tidak kembali menghapus tabel.

> **Catatan**: Strategi `create` akan menghapus seluruh data pada tabel yang
> dikelola Hibernate. Karena modul ini masih dalam tahap akademis dan tidak
> menyimpan data produksi, langkah tersebut aman dilakukan.

### 3.2 Opsi B — Drop Tabel Manual via psql Shell

1. Buka dashboard Render → service Postgres → tombol **Connect** → **PSQL
   Command**, lalu salin perintah `psql ...` ke terminal lokal.
2. Setelah masuk ke prompt `psql`, jalankan:

   ```sql
   DROP TABLE IF EXISTS payrolls CASCADE;
   DROP TABLE IF EXISTS wallets CASCADE;
   DROP TABLE IF EXISTS topup_transactions CASCADE;
   DROP TABLE IF EXISTS wage_config CASCADE;
   ```

3. Trigger redeploy manual dari dashboard Render (tombol **Manual Deploy**).
4. Hibernate akan membuat ulang seluruh tabel dengan tipe `uuid` yang benar.

## 4. Verifikasi Pasca Deploy

Setelah deployment selesai, lakukan verifikasi berikut:

1. **Health check** — pastikan endpoint berikut mengembalikan status `UP`:

   ```
   GET https://<service>.onrender.com/actuator/health
   ```

   Respon yang diharapkan:

   ```json
   { "status": "UP" }
   ```

2. **Verifikasi skema** — pada psql shell jalankan `\d payrolls` dan
   `\d wallets`. Kolom `id` harus bertipe `uuid` (bukan `bigint` ataupun
   `varchar`).

3. **Endpoint fungsional** — uji minimal endpoint berikut harus
   mengembalikan array kosong (bukan HTTP 500):

   ```
   GET /api/payrolls
   GET /api/wallets/{userId}
   ```

## 5. Pengembangan Lokal

Pengembangan lokal tidak terpengaruh perubahan ini. Jalankan service seperti
biasa, misalnya:

```bash
./gradlew bootRun
```

Nilai default `JPA_DDL_AUTO=update`, `PORT=8085`, dan koneksi PostgreSQL lokal
(`jdbc:postgresql://localhost:5432/mysawit_pembayaran`) sudah terisi otomatis
dari `application.yml`. Tidak diperlukan environment variable tambahan untuk
menjalankan aplikasi pada mesin pengembang.
