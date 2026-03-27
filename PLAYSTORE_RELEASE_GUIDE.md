# Panduan Rilis Play Store (AAB)

Dokumen ini untuk menyiapkan upload aplikasi ke Google Play Console.

## 1) Siapkan signing key upload

Isi di `local.properties` (disarankan, jangan di-commit):

```properties
obill.playstore.storeFile=/ABSOLUTE/PATH/ke/upload-keystore.jks
obill.playstore.storePassword=***
obill.playstore.keyAlias=***
obill.playstore.keyPassword=***
```

Alternatif lewat environment variable:

- `OBILL_PLAYSTORE_STORE_FILE`
- `OBILL_PLAYSTORE_STORE_PASSWORD`
- `OBILL_PLAYSTORE_KEY_ALIAS`
- `OBILL_PLAYSTORE_KEY_PASSWORD`

## 2) Build App Bundle

Jalankan:

```bash
./scripts/gradle.sh bundlePlaystoreRelease
```

Output `.aab`:

- `app/build/outputs/bundle/playstore/app-playstore.aab`

## 3) Checklist sebelum upload

- Version naik (`versionCode` harus lebih besar dari rilis sebelumnya)
- Nama aplikasi, icon launcher, splash, dan screenshot final
- Privacy Policy URL valid (wajib untuk banyak kategori)
- Data safety form di Play Console terisi
- Target API sudah sesuai kebijakan Play terbaru
- Izin aplikasi benar-benar sesuai fitur
- Testing internal/closed track sudah lolos smoke test login, dashboard, jual, riwayat, laporan, profil

## 4) Upload ke Play Console

1. Masuk ke Play Console
2. Pilih app `com.obill.app`
3. Buat rilis di track (Internal/Closed/Production)
4. Upload file `.aab`
5. Isi catatan rilis
6. Review & Rollout

## 5) Catatan penting proyek ini

- Build type `release` dipakai untuk alur distribusi internal APK (`updates/`) dan masih debug signing.
- Untuk Play Store gunakan **khusus** build type `playstore` melalui task `bundlePlaystoreRelease`.
