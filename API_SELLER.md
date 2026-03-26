# API Seller – Dokumentasi untuk Aplikasi Mobile Android

API ini digunakan untuk komunikasi antara **aplikasi mobile Android** dengan backend OBill (seller). Semua endpoint (kecuali login) memerlukan **Bearer token** yang didapat dari endpoint login.

---

## Daftar Isi

1. [Base URL & Autentikasi](#base-url--autentikasi)
2. [Login & Logout](#login--logout)
3. [Dashboard](#dashboard)
4. [Paket & Perangkat](#paket--perangkat)
5. [Transaksi (Submit Sale)](#transaksi-submit-sale)
6. [History & Receipt](#history--receipt)
7. [Laporan](#laporan)
8. [Kuota & Remove Expired](#kuota--remove-expired)
9. [Profil](#profil)
10. [Kode HTTP & Error](#kode-http--error)
11. [Troubleshooting](#troubleshooting)

---

## Base URL & Autentikasi

- **Base URL:** `{{base_url}}api/seller/`  
  Contoh: `https://sln.onesky.id/api/seller/` (sesuaikan dengan `application/config/config.php`).

- **Autentikasi:**  
  - **Login:** tidak butuh token. Kirim `email` dan `password`.  
  - **Semua endpoint lain:** wajib header:
    ```http
    Authorization: Bearer <token>
    ```
    Token didapat dari response **login**.

- **CORS:** API mengizinkan request dari domain lain (termasuk aplikasi Android / React Native). Header yang diizinkan: `Content-Type`, `Authorization`, `X-API-Key`.

- **Format respons:** JSON. Content-Type: `application/json`.

### Jika server mengembalikan HTML (bukan JSON)

Beberapa server/rewrite bisa membuat URL `api/seller/...` tidak ter-rute. Gunakan **format URL alternatif** (controller/method langsung):

- Seharusnya: `{{base_url}}api/seller/submit_sale`  
- Alternatif: `{{base_url}}api_seller/submit_sale`

Contoh: `https://sln.onesky.id/api_seller/submit_sale`  
Semua endpoint sama: ganti `api/seller/<method>` dengan `api_seller/<method>` (underscore, tanpa garis miring di tengah).

---

## Persiapan Database

Sebelum menggunakan API, jalankan skrip SQL untuk menambah kolom token:

```sql
-- File: sql/user_add_api_token.sql
ALTER TABLE `user` ADD COLUMN `api_token` VARCHAR(64) NULL DEFAULT NULL;
CREATE INDEX `idx_user_api_token` ON `user` (`api_token`);
```

Jika kolom/index sudah ada, abaikan error duplicate.

---

## Login & Logout

### POST `api/seller/login`

Login seller (role_id = 9). Mengembalikan token untuk dipakai di header request berikutnya.

**Request**

- **Method:** POST  
- **Body (JSON):**
  ```json
  {
    "email": "seller@example.com",
    "password": "password_seller"
  }
  ```
  Atau form-urlencoded: `email=...&password=...`

**Response sukses (200)**

```json
{
  "success": true,
  "token": "64_character_hex_string",
  "user": {
    "id": 1,
    "name": "Nama Seller",
    "email": "seller@example.com",
    "deviceId": "ROUTER-01",
    "saldo": 150000
  }
}
```

**Response error**

- **400** – Email/password kosong:
  ```json
  { "success": false, "message": "Email dan password wajib." }
  ```
- **401** – Kredensial salah:
  ```json
  { "success": false, "message": "Email atau password salah." }
  ```
- **403** – Bukan seller / akun nonaktif:
  ```json
  { "success": false, "message": "Akses hanya untuk akun seller." }
  ```
- **503** – Kolom `api_token` belum ada di tabel `user`:
  ```json
  { "success": false, "message": "Fitur API seller belum diaktifkan (kolom api_token belum ada). Jalankan sql/user_add_api_token.sql." }
  ```

---

### POST `api/seller/logout`

Membatalkan token di server (opsional; client bisa cukup membuang token).

**Request**

- **Method:** POST  
- **Header:** `Authorization: Bearer <token>`

**Response sukses (200)**

```json
{
  "success": true,
  "message": "Berhasil logout."
}
```

---

## Dashboard

### GET `api/seller/dashboard`

Ringkasan dashboard seller: tagihan, penjualan bulan/hari ini, komisi, jumlah transaksi, dan 10 transaksi terakhir.

**Request**

- **Method:** GET  
- **Header:** `Authorization: Bearer <token>`

**Response sukses (200)**

```json
{
  "success": true,
  "data": {
    "tagihan": 0,
    "tagihan_bulan": 450000,
    "penjualan_bulan": 500000,
    "komisi": 120000,
    "komisi_bulan": 50000,
    "count_transaksi": 25,
    "count_transaksi_bulan": 10,
    "jml_penjualan_hari_ini": 3,
    "hpp_hari_ini": 15000,
    "komisi_hari_ini": 5000,
    "penjualan": [
      {
        "id": "123",
        "buyer": "08123456789",
        "kode_voucher": "A1B2",
        "reporter": "Nama Seller",
        "profil": "1 Hari",
        "tanggal": "2025-03-14 10:30:00",
        "selling_price": "5000",
        "komisi": "500",
        "hpp": "5000"
      }
    ]
  }
}
```

- **jml_penjualan_hari_ini** (int): Jumlah transaksi hari ini.
- **hpp_hari_ini** (float): Akumulasi HPP hari ini = SUM(selling_price − komisi) dari tb_penjualan — untuk card "Penjualan hari ini".
- **komisi** (float): Total komisi semua waktu.
- **komisi_bulan** (float): Komisi bulan ini (SUM komisi transaksi bulan berjalan).
- **komisi_hari_ini** (float): Komisi hari ini (SUM komisi transaksi hari ini).
- Setiap item di **penjualan** menyertakan **hpp** (string) = selling_price − komisi per transaksi.

**Debug struktur response penjualan hari ini:** panggil `GET api/seller/debug_penjualan_hari_ini` (dengan Bearer token). Response berisi hanya data penjualan hari ini plus key `_structure` yang menjelaskan tiap field. Lihat [API_SELLER_RESPONSE_PENJUALAN_HARI_INI.md](API_SELLER_RESPONSE_PENJUALAN_HARI_INI.md) untuk struktur lengkap.

---

## Paket & Perangkat

### GET `api/seller/paket`

Daftar paket hotspot (is_sync = 1) dan harga (dari `tb_harga` jika ada).

**Request**

- **Method:** GET  
- **Header:** `Authorization: Bearer <token>`

**Response sukses (200)**

```json
{
  "success": true,
  "data": {
    "paket": [
      {
        "id": "1",
        "nama_bandwidth": "1 Hari",
        "time_limit": "24h",
        "data_limit": "",
        "selling_price": "5000",
        "hpp": "500",
        "is_sync": "1"
      }
    ],
    "harga": [
      {
        "id": "1",
        "kode_profil": "1 Hari",
        "harga": "5000"
      }
    ]
  }
}
```

---

### GET `api/seller/devices`

Daftar router/perangkat (is_remove = 0) untuk dropdown transaksi.

**Request**

- **Method:** GET  
- **Header:** `Authorization: Bearer <token>`

**Response sukses (200)**

```json
{
  "success": true,
  "data": [
    {
      "id": "1",
      "deviceId": "ROUTER-01",
      "host": "192.168.88.1",
      "user": "admin",
      "pass": "***",
      "port": "8728",
      "is_remove": "0"
    }
  ]
}
```

---

## Transaksi (Submit Sale)

### POST `api/seller/submit_sale` atau `api_seller/submit_sale`

Membuat satu transaksi: simpan penjualan, kurangi saldo seller, push user hotspot ke router, kirim bukti ke WhatsApp pembeli.

**Request**

- **Method:** POST  
- **URL:** `api/seller/submit_sale` atau (jika dapat HTML) `api_seller/submit_sale`
- **Header:** `Authorization: Bearer <token>`, `Content-Type: application/json` (jika body JSON)
- **Body (JSON atau form-urlencoded):**
  - `paket_id` (int) – ID paket dari `tb_hotspot_bandwidth_profile`
  - `device_id` (string) – deviceId dari `tb_device` (dropdown devices)
  - `no_wa` (string) – nomor WA pembeli (min 10 digit, akan dinormalisasi ke 62xxx)
  - `kode_voucher` (string) – **4 karakter** (username hotspot di router)

**Contoh body JSON**

```json
{
  "paket_id": 1,
  "device_id": "ROUTER-01",
  "no_wa": "081234567890",
  "kode_voucher": "A1B2"
}
```

**Response sukses (200)**

```json
{
  "success": true,
  "message": "Transaksi disimpan dan user hotspot telah dibuat di router. Bukti transaksi telah dikirim ke WhatsApp pembeli.",
  "sale_id": 456,
  "receipt_preview": "No. Nota: 456/TRX/2025\nTanggal: 14 Mar 2025 10:30\n..."
}
```

**Response error (200, success: false)**

- Data tidak lengkap / tidak valid:
  ```json
  { "success": false, "message": "Data tidak lengkap atau tidak valid." }
  ```
- Paket tidak ditemukan:
  ```json
  { "success": false, "message": "Paket tidak ditemukan." }
  ```
- Saldo tidak cukup:
  ```json
  { "success": false, "message": "Saldo tidak cukup. Isi saldo terlebih dahulu." }
  ```
- Router tidak ditemukan:
  ```json
  { "success": false, "message": "Router tidak ditemukan." }
  ```

**Jika respons berupa HTML (404/500):**

1. Coba URL alternatif: **POST** `https://sln.onesky.id/api_seller/submit_sale` (pakai `api_seller`, bukan `api/seller`).
2. Pastikan header: `Authorization: Bearer <token>` dan (jika pakai JSON) `Content-Type: application/json`.
3. Di server pastikan routing CodeIgniter untuk `api/seller/(:any)` mengarah ke `api_seller/$1` (lihat `application/config/routes.php`).

---

## History & Receipt

### GET `api/seller/history`

Daftar transaksi seller dengan paginasi.

**Request**

- **Method:** GET  
- **Header:** `Authorization: Bearer <token>`  
- **Query (opsional):**
  - `page` (int, default 1)
  - `per_page` (int, default 50, max 100)

**Contoh:** `GET api/seller/history?page=1&per_page=20`

**Response sukses (200)**

```json
{
  "success": true,
  "data": [
    {
      "id": "123",
      "buyer": "08123456789",
      "kode_voucher": "A1B2",
      "reporter": "Nama Seller",
      "profil": "1 Hari",
      "tanggal": "2025-03-14 10:30:00",
      "selling_price": "5000",
      "komisi": "500",
      "seller_id": "ROUTER-01",
      "device_id": "ROUTER-01"
    }
  ],
  "pagination": {
    "page": 1,
    "per_page": 20,
    "total": 25,
    "total_pages": 2
  }
}
```

---

### GET `api/seller/receipt?id=<id>`

Data struk transaksi (bukan file PDF). Berguna untuk tampilan di app. Juga mengembalikan URL ke PDF jika ingin dibuka di browser.

**Request**

- **Method:** GET  
- **Header:** `Authorization: Bearer <token>`  
- **Query:** `id` (int) – ID `tb_penjualan`

**Response sukses (200)**

```json
{
  "success": true,
  "data": {
    "sale": { ... },
    "receipt_preview": "No. Nota: 123/TRX/2025\n...",
    "receipt_url": "https://sln.onesky.id/seller/receipt_pdf/123"
  }
}
```

**Response error**

- **400** – Parameter `id` tidak ada.
- **404** – Transaksi tidak ditemukan atau bukan milik seller.

---

### POST `api/seller/resend_receipt`

Kirim ulang bukti transaksi ke WhatsApp pembeli.

**Request**

- **Method:** POST  
- **Header:** `Authorization: Bearer <token>`  
- **Body:** `id` (int) – ID `tb_penjualan`

**Response sukses (200)**

```json
{
  "success": true,
  "message": "Bukti transaksi telah dikirim ke WhatsApp."
}
```

```json
{
  "success": false,
  "message": "Pengiriman ke WhatsApp gagal."
}
```

---

## Laporan

### GET `api/seller/laporan`

Ringkasan laporan penjualan per rentang tanggal.

**Request**

- **Method:** GET  
- **Header:** `Authorization: Bearer <token>`  
- **Query (wajib):**
  - `date_dari` (string) – format **Y-m-d** (contoh: 2025-03-01)
  - `date_sampai` (string) – format **Y-m-d** (contoh: 2025-03-14)

**Contoh:** `GET api/seller/laporan?date_dari=2025-03-01&date_sampai=2025-03-14`

**Response sukses (200)**

```json
{
  "success": true,
  "data": {
    "date_dari": "2025-03-01",
    "date_sampai": "2025-03-14",
    "jml_penjualan": 15,
    "total_tagihan": 67500,
    "total_komisi": 7500
  }
}
```

**Response error (400)**

- Parameter tanggal kurang atau format salah:
  ```json
  { "success": false, "message": "Parameter date_dari dan date_sampai wajib (format Y-m-d)." }
  ```

### GET `api/seller/laporan_pdf` — Download PDF (sama seperti web seller)

Generate dan **download file PDF** laporan penjualan (isi sama dengan fitur "Generate PDF" di web seller: ringkasan jumlah transaksi, total tagihan, total komisi).

**Request**

- **Method:** GET  
- **Header:** `Authorization: Bearer <token>`  
- **Query (wajib):** `date_dari` (Y-m-d), `date_sampai` (Y-m-d)

**Contoh:** `GET api/seller/laporan_pdf?date_dari=2025-03-01&date_sampai=2025-03-14`

**Response sukses (200)**

- **Content-Type:** `application/pdf`
- **Content-Disposition:** attachment; filename=`laporan-penjualan-{date_dari}-sampai-{date_sampai}.pdf`
- Body: binary PDF (bukan JSON). Di aplikasi mobile bisa dibuka/dishare setelah download.

**Response error (400)** — JSON (jika validasi gagal):

- Parameter kurang / format salah / date_dari > date_sampai: body JSON dengan `success: false` dan `message`.

**Perbandingan**

| Endpoint        | Output   | Kegunaan                                      |
|-----------------|----------|-----------------------------------------------|
| `api/seller/laporan`     | JSON     | Data ringkasan untuk tampil di app            |
| `api/seller/laporan_pdf` | File PDF | Download/cetak laporan (sama seperti web)     |

---

## Kuota & Remove Expired

### POST `api/seller/history_quota`

Mengambil sisa kuota (bytes) dari router untuk beberapa transaksi (berguna di layar history).

**Request**

- **Method:** POST (atau GET dengan array ids)  
- **Header:** `Authorization: Bearer <token>`  
- **Body:** `ids` (array of int) – ID-ID `tb_penjualan`  
  Contoh JSON: `{ "ids": [101, 102, 103] }`

**Response sukses (200)**

Object dengan key = id transaksi, value = info kuota:

```json
{
  "101": {
    "remaining": 52428800,
    "limit": 104857600,
    "remaining_fmt": "50.0 MB",
    "limit_fmt": "100.0 MB",
    "valid": true
  },
  "102": {
    "remaining": 0,
    "limit": 0,
    "remaining_fmt": "Expired",
    "limit_fmt": "—",
    "valid": false
  }
}
```

Jika tidak ada data dari router: `remaining_fmt` bisa "—", `valid: false`.

---

### POST `api/seller/remove_expired_user`

Menghapus user hotspot yang sudah melewati masa berlaku dari router (satu transaksi per request).

**Request**

- **Method:** POST  
- **Header:** `Authorization: Bearer <token>`  
- **Body:** `sale_id` (int) – ID `tb_penjualan`

**Response sukses (200)**

```json
{
  "success": true,
  "message": "User hotspot telah dihapus dari router."
}
```

**Response error (200, success: false)**

- Belum ada callback / tidak ada time limit:
  ```json
  { "success": false, "message": "Belum ada callback / tidak ada time limit." }
  ```
- User belum expired:
  ```json
  { "success": false, "message": "Belum expired." }
  ```

---

## Profil

### GET `api/seller/profile`

Data profil seller yang sedang login.

**Request**

- **Method:** GET  
- **Header:** `Authorization: Bearer <token>`

**Response sukses (200)**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "Nama Seller",
    "email": "seller@example.com",
    "deviceId": "ROUTER-01",
    "hp": "081234567890",
    "saldo": 150000
  }
}
```

Kolom `hp` dan `saldo` ada jika tabel `user` memiliki kolom tersebut.

---

## Kode HTTP & Error

| Kode | Arti |
|------|------|
| **200** | Sukses (periksa `success` di body untuk kasus bisnis seperti submit_sale yang bisa sukses/fail). |
| **400** | Bad Request – parameter kurang atau format salah. |
| **401** | Unauthorized – token tidak ada, tidak valid, atau kedaluwarsa. |
| **403** | Forbidden – akun bukan seller atau nonaktif. |
| **404** | Not Found – resource (mis. transaksi) tidak ditemukan atau bukan milik seller. |
| **503** | Service Unavailable – fitur API belum diaktifkan (mis. kolom `api_token` belum ada). |

Respons error umum (401):

```json
{
  "success": false,
  "message": "Token tidak valid atau kedaluwarsa.",
  "code": "UNAUTHORIZED"
}
```

---

## Troubleshooting

| Masalah | Solusi |
|--------|--------|
| Respons HTML, bukan JSON (404/500) | Gunakan URL **tanpa** garis miring di tengah: `api_seller/submit_sale` bukan `api/seller/submit_sale`. Contoh: `https://sln.onesky.id/api_seller/submit_sale`. |
| POST body tidak terbaca | Kirim header `Content-Type: application/json` dan body JSON; API sudah mendukung pembacaan dari `php://input`. |
| Token invalid (401) | Login ulang; simpan `token` dari response login dan kirim di header `Authorization: Bearer <token>`. |
| Cek di Postman | POST ke `.../api_seller/submit_sale`, header `Authorization: Bearer <token>`, body raw JSON dengan `paket_id`, `device_id`, `no_wa`, `kode_voucher`. Pastikan respons JSON, bukan halaman error. |

---

## Ringkasan Endpoint

| Method | Endpoint | Auth | Keterangan |
|--------|----------|------|------------|
| POST | `api/seller/login` atau `api_seller/login` | Tidak | Login, dapat token |
| POST | `api/seller/logout` atau `api_seller/logout` | Ya | Hapus token di server |
| GET | `api/seller/dashboard` atau `api_seller/dashboard` | Ya | Ringkasan dashboard |
| GET | `api/seller/debug_penjualan_hari_ini` atau `api_seller/debug_penjualan_hari_ini` | Ya | Debug: struktur + data penjualan hari ini saja |
| GET | `api/seller/paket` atau `api_seller/paket` | Ya | Daftar paket + harga |
| GET | `api/seller/devices` atau `api_seller/devices` | Ya | Daftar router |
| POST | `api/seller/submit_sale` atau `api_seller/submit_sale` | Ya | Submit transaksi (body JSON atau form) |
| GET | `api/seller/history` | Ya | History transaksi (paginated) |
| GET | `api/seller/receipt` | Ya | Data struk + URL PDF |
| POST | `api/seller/resend_receipt` | Ya | Kirim ulang bukti WA |
| GET | `api/seller/laporan` | Ya | Laporan per rentang (JSON) |
| GET | `api/seller/laporan_pdf` | Ya | Download PDF laporan (sama seperti web seller) |
| POST | `api/seller/remove_expired_user` | Ya | Hapus user expired dari router |
| POST | `api/seller/history_quota` | Ya | Sisa kuota per transaksi |
| GET | `api/seller/profile` | Ya | Profil seller |

---

*Dokumentasi ini mengacu pada controller `Api_seller`. Jika server mengembalikan HTML untuk `api/seller/...`, gunakan format URL `api_seller/<method>` (mis. `api_seller/submit_sale`). Jika base_url atau index.php diubah, sesuaikan URL di aplikasi Android.*
