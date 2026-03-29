# Release notes — OBill

## 2.0.4

### Ringkasan
Peningkatan alur penjualan dan laporan berbasis modal, cek kuota pelanggan, avatar profil, perilaku dialog yang lebih aman, serta dialog update yang menampilkan catatan rilis dari server.

### Perbaikan & fitur
- **Saldo sebelum Jual:** Pengecekan saldo dengan ambang Rp 10.000 / Rp 30.000; dialog peringatan dan arahan ke Beranda atau halaman Jual sesuai aturan.
- **Kuota (API publik):** Modal cek sisa kuota (voucher + nomor pembeli), hasil di modal terpisah, kirim ringkasan via WhatsApp, modal tidak tertutup ketuk luar.
- **Laporan:** Modal pemilih tanggal dengan Tutup (kiri) serta Download & Lihat (kanan); tab Laporan di bottom bar membuka modal yang sama; ringkasan tanpa baris Total Komisi.
- **Transaksi paket (Jual):** Seluruh wizard dalam modal (pilih paket → lokasi → nomor WA → Generate → ringkasan dengan kode voucher → Proses); bottom nav Jual membuka modal setelah cek saldo; tab Jual menampilkan petunjuk singkat.
- **Router / lokasi:** Hanya menampilkan nama lokasi; ringkasan transaksi memakai baris label–nilai kiri–kanan (responsif).
- **Modal:** Dialog utama (transaksi, laporan, kuota) tidak tertutup oleh ketukan di luar atau tombol kembang sistem; struk PDF memiliki tombol Tutup.
- **Avatar:** Gambar avatar dari DiceBear (CC0) di kartu beranda dan profil seller; fallback huruf inisial jika gagal memuat.
- **Update wajib:** Endpoint JSON dapat menyertakan `release_notes`; dialog “Update Diperlukan” menampilkan ringkasan versi baru.

### Teknis
- **Gradle:** `org.gradle.java.home` disarankan JDK 17–21 (Java 25 tidak kompatibel dengan toolchain Gradle saat ini).
- Dependensi: Coil untuk memuat gambar avatar.

---

Versi terdahulu: lihat commit dan tag di repositori.
