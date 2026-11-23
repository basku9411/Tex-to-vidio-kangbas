# Kangbas Text to Video — Cloudflare Pages (1 File HTML Tunggal)

`cloudflare-pages/index.html` adalah **1 file HTML tunggal yang sudah lengkap** (seluruh kode CSS tema Hitam-Merah-Putih, 28 efek Animasi Foto + 14 font teks manual, Teks Otomatis AI dengan 8 gaya subtitle, Kie.ai, memori riwayat per email, dan gambar QRIS sudah tertanam di dalamnya).

## Fitur Lengkap di `index.html`
1. **Buat Video (Kie.ai)**: Text-to-Video & Image-to-Video, multi-API Key, dan total saldo kredit gabungan dari semua kunci yang aktif.
2. **Animasi (Animasi Foto & Teks Otomatis Terintegrasi)**:
   - **Animasi Foto (0 Kredit)**: 28 efek/karakter bergerak (*Burung Terbang*, *Merpati Putih*, *Not Doremi*, *Sakura*, *Salju*, dll.), 14 font teks manual, musik latar (auto-durasi), dan opsi **Buat Teks Otomatis dari Audio Latar (AI)** langsung di panel Teks.
   - **Teks Otomatis**: Transkripsi suara video otomatis dengan AI, 8 bentuk teks, edit baris teks, dan ekspor video berteks.
3. **Galeri**: Menyimpan riwayat video Kie.ai, hasil Animasi Foto, & hasil Teks Otomatis lengkap dengan opsi unduh dan edit ulang, dipisahkan per profil email.
4. **Pengaturan**: Kelola profil email untuk memori riwayat, multi-API Key Kie.ai, dan kunci Gemini opsional.
5. **QRIS Instan**: Tombol **LANJUTKAN** pada dialog QRIS langsung aktif tanpa menunggu hitungan mundur.

## Cara Upload ke Cloudflare Pages

### Opsi A — Direct Upload (Tinggal Upload `index.html`)
1. Unduh file **`cloudflare-pages/index.html`** (dari File Explorer atau Export ZIP AI Studio).
2. Di **Cloudflare Dashboard → Workers & Pages → Create → Pages → Direct Upload**, unggah folder berisi **`index.html`** tersebut.
3. Untuk memakai fitur AI **Teks Otomatis** di mode Direct Upload, masukkan **Gemini API Key** Anda di tab **Pengaturan** pada webnya.

### Opsi B — Connect to Git (Otomatis dengan Server Function `/api/transcribe`)
1. **Push to GitHub** dari menu AI Studio.
2. Hubungkan repo di **Cloudflare Pages** dengan:
   - **Framework preset**: `None`
   - **Build command**: *(kosongkan)*
   - **Build output directory**: `cloudflare-pages`
3. Tambahkan **`GEMINI_API_KEY`** di **Settings → Environment variables** Cloudflare Pages lalu **Retry deployment**.
