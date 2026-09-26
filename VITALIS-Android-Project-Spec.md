# VITALIS — Dietary & Activity Tracking App (Android)

> **Project Specification Document (PRD + Technical Design)**
> Versi dokumen: 1.0 · Status: Draft untuk personal project
> Platform: Android (Kotlin, Jetpack Compose) · Min SDK 26 · Target SDK 36

---

## Daftar Isi

1. [Ringkasan Proyek](#1-ringkasan-proyek)
2. [Scope & Roadmap Rilis](#2-scope--roadmap-rilis)
3. [Persona & User Stories](#3-persona--user-stories)
4. [Spesifikasi Fitur](#4-spesifikasi-fitur)
5. [Formula, Algoritma & Perhitungan](#5-formula-algoritma--perhitungan)
6. [Arsitektur Teknis](#6-arsitektur-teknis)
7. [Data Model & Skema Database](#7-data-model--skema-database)
8. [GPS Tracking Engine](#8-gps-tracking-engine)
9. [Sistem Gamifikasi](#9-sistem-gamifikasi)
10. [Design System — Liquid Glass UI](#10-design-system--liquid-glass-ui)
11. [Navigasi & UX Flow](#11-navigasi--ux-flow)
12. [Integrasi Eksternal & API](#12-integrasi-eksternal--api)
13. [Permission, Privacy & Compliance](#13-permission-privacy--compliance)
14. [Offline-First & Sinkronisasi](#14-offline-first--sinkronisasi)
15. [Testing & QA](#15-testing--qa)
16. [Analytics & KPI](#16-analytics--kpi)
17. [Estimasi Effort & Sprint Plan](#17-estimasi-effort--sprint-plan)
18. [Risiko & Mitigasi](#18-risiko--mitigasi)
19. [Referensi](#19-referensi)

---

## 1. Ringkasan Proyek

### 1.1 Visi

Satu aplikasi yang menutup **loop energi lengkap**: kalori masuk (makanan) → kalori keluar (BMR + aktivitas harian + olahraga terukur GPS) → dampak ke komposisi tubuh & indikator kesehatan → di-*gamify* supaya konsisten dipakai.

Kebanyakan aplikasi hanya kuat di satu sisi: MyFitnessPal kuat di food logging tapi lemah di tracking olahraga; Strava kuat di GPS tapi tidak punya nutrisi. VITALIS menggabungkan keduanya dengan UI yang lebih menyenangkan.

### 1.2 Problem Statement

| Masalah | Dampak ke User |
|---|---|
| Food logging membosankan, butuh 10+ tap per makanan | Drop-off tinggi setelah 7 hari |
| Angka "kalori terbakar" di kebanyakan app tidak akurat (gross vs net) | User over-eating karena merasa "sudah bakar 500 kkal" |
| Data olahraga & data nutrisi terpisah di app berbeda | Tidak ada gambaran defisit/surplus harian yang benar |
| Progress lambat & tak terlihat → motivasi hilang | Uninstall di minggu ke-3 |

### 1.3 Solusi / Value Proposition

1. **Energy Balance Ring** — satu widget utama: `Intake − (BMR + NEAT + Exercise) = Net Balance`.
2. **Logging cepat** — barcode scan, natural-language input ("2 piring nasi + ayam bakar"), meal template, recent/frequent list.
3. **GPS tracking multi-sport** dengan metrik selevel sport tracker (pace, split, elevation, cadence, GAP).
4. **Net calorie burn** (bukan gross) supaya defisit tidak dihitung dobel.
5. **Health Score** — satu angka 0–100 dari beberapa indikator kesehatan, mudah dipahami awam.
6. **Gamifikasi** — XP, level, streak, badge, quest mingguan, personal record.
7. **Liquid Glass UI** — estetika glassmorphism ala Apple, tapi tetap aksesibel dan hemat baterai.

### 1.4 Target Pengguna

- Usia 18–45, urban, punya smartphone mid-range ke atas.
- Ingin turun/naik berat badan atau maintenance.
- Suka olahraga outdoor: lari, sepeda, hiking, trekking, mendaki gunung, berkuda.
- Familiar aplikasi kesehatan tapi belum menemukan yang "all-in-one".

### 1.5 Non-Goals (v1)

- ❌ Bukan alat diagnosis medis
- ❌ Tidak ada meal delivery / e-commerce
- ❌ Tidak ada coaching manusia
- ❌ Tidak build iOS dulu (arsitektur disiapkan agar bisa KMP di masa depan)

---

## 2. Scope & Roadmap Rilis

### Fase 0 — Foundation (Sprint 1–2)

- Setup project, module structure, DI, CI/CD
- Design system dasar + komponen glass
- Onboarding + profil user + kalkulasi BMR/TDEE
- Room database + skema awal

### Fase 1 — MVP (Sprint 3–8) ⭐ *Target rilis internal*

| Modul | Fitur |
|---|---|
| Nutrition | Search makanan, log manual, barcode scan, custom food, water log |
| Energy | BMR/TDEE, target kalori, energy balance ring, makro (P/C/F) |
| Body | Log berat, tinggi, BMI, grafik tren |
| Activity | Step counter, log olahraga manual berbasis MET |
| GPS | Tracking Run, Walk, Cycling + peta rute + auto-pause |
| Gamification | XP, level, streak, 15 badge dasar |
| UI | Dashboard, Diary, Activity, Progress, Profile |

### Fase 2 — v1.1 (Sprint 9–12)

- Sport tambahan: Hiking, Trekking, Mountain Climbing, Horse Riding, Trail Run
- Health Score + indikator lanjutan (RHR, sleep, waist, body fat %)
- Health Connect integration (baca/tulis)
- Advanced metrics: split, GAP, elevation profile, cadence, VO₂max estimate
- Quest mingguan & challenge

### Fase 3 — v1.2+ (Backlog)

- Photo food recognition (AI)
- Wear OS companion
- Social feed, leaderboard teman, club
- Meal planning & resep
- Widget homescreen, Live Activity–style notification
- Export GPX/TCX, integrasi Strava

---

## 3. Persona & User Stories

### 3.1 Persona

**Rangga, 28 — "Si Defisit"**
Karyawan kantoran, BB 88 kg, ingin turun ke 75 kg. Lari 3×/minggu. Butuh: hitungan kalori jujur, motivasi harian.

**Sinta, 34 — "Si Konsisten"**
Ibu bekerja, target maintenance + fit. Suka sepeda weekend & hiking bulanan. Butuh: logging cepat, tracking rute, tidak ribet.

**Bayu, 22 — "Si Kompetitif"**
Mahasiswa, trail runner. Butuh: metrik detail (pace, elevation gain, PR), badge, leaderboard.

### 3.2 User Stories Prioritas

| ID | Sebagai… | Saya ingin… | Sehingga… | Prioritas |
|---|---|---|---|---|
| US-01 | pengguna baru | mengisi profil dan mendapat target kalori otomatis | tahu harus makan berapa | P0 |
| US-02 | pengguna | scan barcode makanan kemasan | logging < 5 detik | P0 |
| US-03 | pengguna | melihat sisa kalori hari ini dalam 1 layar | tahu masih boleh makan berapa | P0 |
| US-04 | pelari | menekan Start dan aplikasi merekam rute + jarak + pace | tahu performa saya | P0 |
| US-05 | pengguna | kalori olahraga otomatis masuk ke budget harian | tidak perlu hitung manual | P0 |
| US-06 | pengguna | mencatat berat badan dan lihat grafiknya | tahu progress saya | P0 |
| US-07 | pengguna | mendapat XP & badge dari aktivitas | termotivasi konsisten | P1 |
| US-08 | pendaki | tracking elevation gain dan kalori mendaki | tahu beban aktivitas | P1 |
| US-09 | pengguna | melihat Health Score | paham kondisi tubuh secara keseluruhan | P1 |
| US-10 | pengguna | app tetap merekam saat layar mati | rekaman tidak terputus | P0 |
| US-11 | pengguna | data tersimpan offline | bisa dipakai di gunung tanpa sinyal | P0 |
| US-12 | pengguna | mengulang meal kemarin dengan 1 tap | hemat waktu | P1 |

---

## 4. Spesifikasi Fitur

### 4.1 Onboarding & Profil

**Input wajib:** nama/nickname, jenis kelamin, tanggal lahir, tinggi (cm), berat (kg), level aktivitas, tujuan (turun/maintain/naik), kecepatan target (0.25/0.5/0.75/1.0 kg per minggu).

**Input opsional:** lingkar pinggang, lingkar pinggul, body fat %, resting heart rate, jam tidur target, preferensi diet (normal/vegetarian/vegan/keto/halal), alergi.

**Output onboarding:**
- BMR, TDEE, target kalori harian
- Target makro (protein/karbo/lemak) dalam gram
- Proyeksi tanggal pencapaian goal
- Target langkah harian & target air minum

**Rules:**
- Defisit maksimal dibatasi 25% dari TDEE
- Floor kalori: 1500 kkal (pria) / 1200 kkal (wanita) — tampilkan warning jika target menembus floor
- Umur < 18 tahun → mode "informational only", tidak memberikan target defisit

---

### 4.2 Calorie Intake — Food Logging

#### 4.2.1 Metode Input

| Metode | Deskripsi | Fase |
|---|---|---|
| Search database | Cari nama makanan, pilih porsi | MVP |
| Barcode scan | CameraX + ML Kit Barcode → lookup EAN/UPC | MVP |
| Recent & Frequent | Daftar makanan sering dipakai | MVP |
| Quick add | Input kalori + makro langsung tanpa nama | MVP |
| Custom food | Buat entri makanan sendiri (bisa dari label gizi) | MVP |
| Meal template | Simpan kombinasi makanan sebagai "Sarapan Rutin" | v1.1 |
| Copy from date | Salin diary dari tanggal lain | v1.1 |
| Recipe builder | Input bahan + porsi → kalori per serving | v1.1 |
| Natural language | "nasi goreng 1 piring + es teh manis" → parsed | v1.2 |
| Photo recognition | Foto makanan → estimasi | v1.2 |

#### 4.2.2 Struktur Meal

Sarapan · Makan Siang · Makan Malam · Snack · *(opsional user tambah: Pre-Workout, Post-Workout)*

#### 4.2.3 Data per Entri Makanan

**Wajib:** nama, jumlah, unit porsi, kalori, protein, karbo, lemak
**Tambahan:** serat, gula, lemak jenuh, sodium, kolesterol, kalium
**Mikro (v1.1):** Vit A, C, D, kalsium, zat besi

#### 4.2.4 Water Tracking

- Target default: `berat_kg × 30 ml` (dibulatkan ke 250 ml terdekat)
- Quick add: 250 ml / 500 ml / custom
- Visual: gelas terisi dengan animasi cairan, reminder tiap 2 jam (opsional)

#### 4.2.5 Sumber Database Makanan

Strategi **berlapis**, di-cache lokal:

| Layer | Sumber | Peran | Catatan |
|---|---|---|---|
| 1 | **USDA FoodData Central** | Makanan generik, referensi paling akurat | Gratis, butuh API key, laboratorium-verified |
| 2 | **Open Food Facts** | Produk kemasan + barcode global | Gratis, unlimited, crowdsourced — kualitas bervariasi |
| 3 | **Kurasi lokal Indonesia** | Nasi goreng, rendang, gado-gado, dll. | Seed manual ~500 item, TKPI/Nutrisurvey sebagai acuan |
| 4 | User-generated | Custom food & resep user | Private per user |

> **Penting:** selalu tampilkan sumber data & izinkan user meng-*override* nilai gizi. Data crowdsourced tidak boleh diperlakukan sebagai kebenaran absolut.

---

### 4.3 Calorie Outtake — Energy Expenditure

Total pengeluaran energi dipecah 4 komponen:

```
TDEE = BMR + TEF + NEAT + EAT
```

| Komponen | Cara Dihitung | Porsi Tipikal |
|---|---|---|
| **BMR** (basal) | Mifflin-St Jeor / Katch-McArdle | 60–70% |
| **TEF** (thermic effect of food) | 10% dari intake, atau weighted per makro | ~10% |
| **NEAT** (aktivitas non-olahraga) | Step counter + activity recognition | 15–30% |
| **EAT** (exercise) | MET × durasi, atau GPS-based | 0–30% |

#### 4.3.1 Step & NEAT Tracking

- `TYPE_STEP_COUNTER` sensor (hardware, hemat baterai) sebagai sumber utama
- Fallback: accelerometer + peak detection algorithm
- Activity Recognition API untuk klasifikasi still/walking/running/in_vehicle
- Panjang langkah default: `tinggi_cm × 0.415` (pria) / `× 0.413` (wanita)

#### 4.3.2 Exercise Logging Manual

Katalog aktivitas dengan MET value dari 2024 Adult Compendium of Physical Activities. User pilih aktivitas + durasi + intensitas → kalori otomatis.

#### 4.3.3 Anti Double-Counting ⚠️

Ini kesalahan paling umum di aplikasi kalori. Aturan VITALIS:

```
Net Exercise Calories = Gross MET Calories − BMR selama durasi olahraga
```

Karena BMR sudah dihitung dalam TDEE untuk 24 jam penuh, kalori istirahat selama 1 jam lari tidak boleh dihitung dua kali. Angka **net** inilah yang ditambahkan ke budget kalori harian. Angka gross tetap ditampilkan di ringkasan aktivitas (karena user familiar dengan angka itu), dengan label yang jelas.

---

### 4.4 Body Metrics & Health Indicators

#### 4.4.1 Metrik yang Dilacak

| Metrik | Input | Frekuensi | Auto-calc |
|---|---|---|---|
| Berat badan | Manual / smart scale via Health Connect | Harian | — |
| Tinggi badan | Manual | Jarang | — |
| BMI | — | Otomatis | ✅ |
| Body Fat % | Manual (caliper/BIA) atau estimasi | Mingguan | ✅ estimasi |
| Lean Body Mass | — | Otomatis | ✅ |
| Lingkar pinggang | Manual (pita ukur) | Mingguan | — |
| Lingkar pinggul, dada, lengan, paha | Manual | Mingguan | — |
| WHtR (waist-to-height) | — | Otomatis | ✅ |
| WHR (waist-to-hip) | — | Otomatis | ✅ |
| Resting Heart Rate | Wearable / manual | Harian | — |
| HRV | Wearable | Harian | — |
| Tekanan darah | Manual | Opsional | — |
| Durasi & kualitas tidur | Health Connect | Harian | — |
| VO₂max estimate | — | Dari data lari | ✅ |
| SpO₂ | Wearable | Opsional | — |
| Progress photo | Kamera | Mingguan | — |

#### 4.4.2 Health Score (0–100)

Skor komposit yang mudah dibaca, dipecah ke 5 pilar:

| Pilar | Bobot | Komponen |
|---|---|---|
| **Body Composition** | 25% | BMI, WHtR, body fat % vs rentang sehat |
| **Cardio Fitness** | 25% | RHR, VO₂max estimate, HRV trend |
| **Activity** | 20% | Menit aktivitas/minggu vs 150 menit WHO, langkah harian |
| **Nutrition** | 20% | Konsistensi log, kecukupan protein, serat, rasio makro, air |
| **Recovery** | 10% | Durasi tidur, konsistensi jam tidur, rest day |

**Aturan tampilan:**
- Tampilkan sebagai *trend*, bukan vonis. "Naik 4 poin dari minggu lalu" lebih berguna dari "Skor kamu 72".
- Selalu sertakan disclaimer: bukan alat diagnosis medis.
- Jangan tampilkan skor merah/menakutkan; gunakan bahasa netral dan actionable.

#### 4.4.3 Guardrail Kesehatan

- Peringatan jika target defisit > 25% TDEE
- Peringatan jika BMI hasil target < 18.5
- Blokir target berat badan yang menempatkan user di BMI < 17.5
- Deteksi pola logging ekstrem (intake < 800 kkal berulang) → tampilkan pesan supportif + saran konsultasi profesional
- Tidak ada fitur "fasting streak" atau leaderboard berbasis defisit terdalam

---

### 4.5 GPS Activity Tracking

#### 4.5.1 Sport yang Didukung

| Sport | GPS | Elevation | Cadence | Metrik Khusus |
|---|---|---|---|---|
| Running (Outdoor) | ✅ | ✅ | ✅ | Pace, split per km, GAP |
| Trail Running | ✅ | ✅ | ✅ | Elevation gain, vertical speed |
| Walking | ✅ | ✅ | ✅ | Langkah, pace |
| Hiking | ✅ | ✅ | ✅ | Ascent/descent, grade |
| Trekking | ✅ | ✅ | — | Durasi, beban ransel (input manual) |
| Mountain Climbing | ✅ | ✅ | — | Elevation gain, altitude, summit marker |
| Cycling (Road) | ✅ | ✅ | opsional (BLE) | Speed, max speed, elevation |
| Mountain Biking | ✅ | ✅ | opsional | Grade, descent |
| Horse Riding | ✅ | ✅ | — | Speed, gait estimate |
| Swimming (Open Water) | ✅ | — | — | Jarak, pace/100m |
| Rowing / Kayaking | ✅ | — | — | Speed, jarak |
| Skateboard / Inline | ✅ | — | — | Speed, jarak |
| Indoor (Treadmill, Gym) | ❌ | — | ✅ | Durasi + MET manual |

#### 4.5.2 Metrik yang Direkam

**Real-time (live screen):**
Durasi · Jarak · Pace/Speed saat ini · Pace rata-rata · Kalori · Elevation · Heart rate (jika ada) · Cadence

**Post-activity (summary):**
- Peta rute dengan gradient warna berdasarkan pace/elevation
- Split per km/mile dengan bar chart
- Elevation profile chart
- Grafik pace, HR zone distribution, cadence over time
- Total ascent / descent, max altitude, grade rata-rata
- Moving time vs elapsed time
- Kalori gross & net
- Perbandingan dengan aktivitas serupa sebelumnya
- Personal Record yang tercapai
- Efek pelatihan (Training Load / TRIMP)

#### 4.5.3 Kontrol Sesi

`Start` → `Pause / Auto-Pause` → `Resume` → `Lap` → `Stop` → `Save / Discard`

**Fitur pendukung:**
- **Auto-pause** — trigger jika speed < threshold selama > 5 detik (threshold berbeda per sport)
- **Auto-lap** — setiap 1 km / 1 mil, atau manual lap
- **Countdown 3-2-1** sebelum start
- **Audio cue** — TTS setiap 1 km: "Kilometer 3, waktu 18 menit 42 detik, pace 6 menit 14"
- **Crash recovery** — sesi di-persist tiap 5 detik; jika app di-kill, tawarkan resume saat dibuka lagi
- **Screen lock** — cegah tap tak sengaja saat aktivitas
- **Low battery mode** — turunkan sampling GPS otomatis di bawah 15% baterai

---

## 5. Formula, Algoritma & Perhitungan

### 5.1 Basal Metabolic Rate (BMR)

**Mifflin-St Jeor** (default — paling akurat untuk populasi umum, direkomendasikan Academy of Nutrition and Dietetics):

```
Pria   : BMR = (10 × berat_kg) + (6.25 × tinggi_cm) − (5 × umur) + 5
Wanita : BMR = (10 × berat_kg) + (6.25 × tinggi_cm) − (5 × umur) − 161
```

**Katch-McArdle** (dipakai otomatis jika body fat % diketahui — lebih akurat untuk yang berotot):

```
LBM = berat_kg × (1 − body_fat_percent/100)
BMR = 370 + (21.6 × LBM)
```

**Aturan pemilihan formula:**
- Body fat % tersedia → Katch-McArdle
- BMI > 35 → Mifflin-St Jeor + flag "estimasi mungkin tinggi" (formula cenderung *overestimate* pada obesitas berat karena tidak memisahkan lean mass dari fat mass)
- Umur < 19 → gunakan Schofield/FAO-WHO-UNU, tanpa saran defisit

### 5.2 Total Daily Energy Expenditure (TDEE)

```
TDEE = BMR × Activity Factor
```

| Level | Faktor | Deskripsi |
|---|---|---|
| Sedentary | 1.200 | Kerja duduk, hampir tidak olahraga |
| Lightly Active | 1.375 | Olahraga ringan 1–3×/minggu |
| Moderately Active | 1.550 | Olahraga sedang 3–5×/minggu |
| Very Active | 1.725 | Olahraga berat 6–7×/minggu |
| Extra Active | 1.900 | Atlet / pekerjaan fisik berat |

> **Mode Dinamis (direkomendasikan):** Alih-alih faktor statis, gunakan
> `TDEE_hari_ini = BMR × 1.2 + NEAT_dari_langkah + EAT_dari_aktivitas`.
> Ini lebih responsif dan menghindari double-count aktivitas yang sudah tercatat.

### 5.3 Target Kalori & Makro

```
Defisit/Surplus = target_kg_per_minggu × 7700 / 7   (kkal per hari)
Target Kalori   = TDEE − Defisit   (atau + Surplus)
```
*(1 kg lemak tubuh ≈ 7700 kkal)*

**Distribusi makro default:**

| Goal | Protein | Lemak | Karbo |
|---|---|---|---|
| Fat Loss | 1.8–2.2 g/kg BB | 0.8 g/kg BB | sisanya |
| Maintenance | 1.6 g/kg BB | 25–30% kalori | sisanya |
| Muscle Gain | 1.6–2.0 g/kg BB | 25% kalori | sisanya |

```
Protein: 4 kkal/g · Karbohidrat: 4 kkal/g · Lemak: 9 kkal/g · Alkohol: 7 kkal/g
```

### 5.4 Kalori Aktivitas (MET-based)

Rumus dasar dari Compendium of Physical Activities (1 MET = 3.5 ml O₂/kg/menit ≈ 1 kkal/kg/jam):

```
kkal/menit = (MET × 3.5 × berat_kg) / 200
kkal_total = MET × berat_kg × durasi_jam        (bentuk sederhana)
```

**Net calories (yang masuk ke budget harian):**

```
kkal_net = kkal_gross − (BMR / 1440 × durasi_menit)
```

**Tabel MET referensi (2024 Adult Compendium):**

| Aktivitas | Kecepatan | MET |
|---|---|---|
| Jalan santai | 3.2–4.0 km/h | 2.8–3.0 |
| Jalan cepat | 5.6–6.3 km/h | 4.8 |
| Lari | 8.0 km/h | 8.3 |
| Lari | 9.7–10.1 km/h | 9.3 |
| Lari | 11.3 km/h | 11.0 |
| Lari | 12.9 km/h | 12.8 |
| Sepeda santai | < 16 km/h | 4.0 |
| Sepeda sedang | 19–22 km/h | 8.0 |
| Sepeda cepat | 22.5–25.5 km/h | 10.0 |
| Hiking (cross country) | — | 6.0 |
| Hiking dengan beban 5–10 kg | — | 7.3 |
| Mendaki, batu/tebing | — | 8.0 |
| Berkuda, jalan | — | 3.8 |
| Berkuda, trot | — | 5.8 |
| Renang, gaya bebas sedang | — | 5.8 |
| Angkat beban, vigorous | — | 6.0 |

> Simpan tabel MET lengkap sebagai asset JSON lokal (`met_activities.json`), dengan kode 5-digit sesuai Compendium untuk traceability.

### 5.5 Kalori Berbasis GPS (lebih akurat dari MET statis)

Untuk lari & jalan, gunakan model biomekanik yang memperhitungkan kemiringan (ACSM metabolic equations):

```
# Berjalan (kecepatan 1.9–4.0 mph)
VO2 (ml/kg/min) = (0.1 × S) + (1.8 × S × G) + 3.5

# Berlari (kecepatan > 5.0 mph)
VO2 (ml/kg/min) = (0.2 × S) + (0.9 × S × G) + 3.5

S = kecepatan dalam m/menit
G = grade (kemiringan sebagai desimal, mis. 0.05 untuk 5%)

kkal/menit = (VO2 × berat_kg) / 200
```

Untuk bersepeda, gunakan model daya (power):

```
P_total = P_gravitasi + P_rolling + P_aero
P_gravitasi = m × g × sin(θ) × v
P_rolling   = Crr × m × g × cos(θ) × v
P_aero      = 0.5 × ρ × CdA × v³

kkal/jam = (P_watt × 3.6) / efisiensi   ; efisiensi ≈ 0.22–0.25
```

**Prioritas sumber kalori:**
`Heart Rate–based` (jika ada HR strap) → `GPS biomekanik` → `MET statis`

### 5.6 Kalori Berbasis Heart Rate (jika wearable tersedia)

```
Pria   : kkal/min = (−55.0969 + 0.6309×HR + 0.1988×BB + 0.2017×umur) / 4.184
Wanita : kkal/min = (−20.4022 + 0.4472×HR − 0.1263×BB + 0.074×umur) / 4.184
```

### 5.7 Metrik Tubuh

```
BMI  = berat_kg / (tinggi_m)²
WHtR = lingkar_pinggang_cm / tinggi_cm        # sehat jika < 0.5
WHR  = lingkar_pinggang_cm / lingkar_pinggul_cm
LBM  = berat_kg × (1 − BF%/100)
FM   = berat_kg − LBM

# Estimasi Body Fat (Deurenberg, dari BMI)
BF% = (1.20 × BMI) + (0.23 × umur) − (10.8 × jenis_kelamin) − 5.4
      # jenis_kelamin: pria = 1, wanita = 0

# Berat badan ideal (Devine)
Pria   : 50.0 + 2.3 × (tinggi_inch − 60)
Wanita : 45.5 + 2.3 × (tinggi_inch − 60)
```

**Klasifikasi BMI (dewasa):**

| Kategori | Umum (WHO) | Asia-Pasifik |
|---|---|---|
| Underweight | < 18.5 | < 18.5 |
| Normal | 18.5–24.9 | 18.5–22.9 |
| Overweight | 25.0–29.9 | 23.0–24.9 |
| Obese I | 30.0–34.9 | 25.0–29.9 |
| Obese II | ≥ 35.0 | ≥ 30.0 |

> Berikan toggle "Gunakan standar Asia-Pasifik" karena default WHO kurang sesuai untuk populasi Indonesia.

### 5.8 Metrik Performa Olahraga

```
Pace (min/km)    = durasi_menit / jarak_km
Speed (km/h)     = jarak_km / durasi_jam

# Grade Adjusted Pace — pace ekuivalen seandainya lari di jalan datar
GAP = pace_aktual × (1 + 0.03 × grade_naik% − 0.015 × grade_turun%)

# VO2max estimate dari lari (Daniels & Gilbert / Jack Daniels)
VDOT ≈ (−4.60 + 0.182258×v + 0.000104×v²) / 
       (0.8 + 0.1894393×e^(−0.012778×t) + 0.2989558×e^(−0.1932605×t))
       v = kecepatan (m/min), t = durasi (menit)

# VO2max sederhana dari RHR (Uth-Sørensen-Overgaard-Pedersen)
VO2max ≈ 15.3 × (HRmax / HRrest)

# TRIMP (Training Impulse) — beban latihan
TRIMP = durasi_menit × ΔHR_ratio × 0.64 × e^(1.92 × ΔHR_ratio)   # pria
ΔHR_ratio = (HR_avg − HR_rest) / (HR_max − HR_rest)

HRmax ≈ 208 − (0.7 × umur)     # Tanaka, lebih akurat dari 220−umur
```

**Heart Rate Zones (% HRmax):**

| Zona | Rentang | Fokus |
|---|---|---|
| Z1 Recovery | 50–60% | Pemulihan |
| Z2 Aerobic | 60–70% | Base building, fat burn |
| Z3 Tempo | 70–80% | Aerobic capacity |
| Z4 Threshold | 80–90% | Lactate threshold |
| Z5 VO₂max | 90–100% | Anaerobic power |

### 5.9 Elevation & Jarak

```
# Haversine — jarak antara dua koordinat GPS
a = sin²(Δφ/2) + cos(φ1) × cos(φ2) × sin²(Δλ/2)
c = 2 × atan2(√a, √(1−a))
d = R × c     ; R = 6371000 m

# Elevation gain — hanya hitung kenaikan di atas threshold noise
gain = Σ max(0, alt[i] − alt[i−1])  untuk |Δalt| > 3 m (setelah smoothing)
```

Barometer (`TYPE_PRESSURE`) jauh lebih akurat dari GPS altitude untuk elevation — gunakan jika tersedia, dengan kalibrasi awal dari GPS.

---

## 6. Arsitektur Teknis

### 6.1 Tech Stack

| Layer | Teknologi |
|---|---|
| Bahasa | Kotlin 2.x |
| UI | Jetpack Compose + Material 3 |
| Arsitektur | Clean Architecture + MVVM/MVI, unidirectional data flow |
| DI | Hilt (Dagger) |
| Async | Coroutines + Flow |
| Database | Room + SQLite (SQLCipher untuk enkripsi) |
| Preferences | DataStore (Proto) |
| Networking | Retrofit + OkHttp + Kotlinx Serialization |
| Image | Coil |
| Maps | Google Maps Compose / MapLibre (alternatif open source) |
| Location | FusedLocationProviderClient (Play Services Location) |
| Health | Health Connect (androidx.health.connect) |
| Charts | Vico / Compose Charts (custom untuk elevation profile) |
| Barcode | CameraX + ML Kit Barcode Scanning |
| Background | WorkManager + Foreground Service |
| Auth (opsional) | Firebase Auth / Credential Manager |
| Backend (opsional) | Firebase Firestore atau Supabase |
| Testing | JUnit5, MockK, Turbine, Compose UI Test, Robolectric |
| Build | Gradle Kotlin DSL + Version Catalog |
| CI/CD | GitHub Actions → Play Console internal track |

### 6.2 Struktur Modul

```
vitalis/
├── app/                          # Application, navigation host, DI setup
├── core/
│   ├── core-ui/                  # Design system, theme, glass components
│   ├── core-common/              # Utils, extensions, Result wrapper
│   ├── core-database/            # Room DB, DAO, entities, migrations
│   ├── core-datastore/           # Preferences
│   ├── core-network/             # Retrofit setup, interceptors
│   ├── core-model/               # Domain models (pure Kotlin)
│   └── core-testing/             # Test fixtures & fakes
├── feature/
│   ├── feature-onboarding/
│   ├── feature-dashboard/
│   ├── feature-nutrition/        # Diary, search, barcode, custom food
│   ├── feature-activity/         # Activity list, manual log, MET catalog
│   ├── feature-tracking/         # GPS live tracking screen + service
│   ├── feature-body/             # Weight, measurements, BMI, health score
│   ├── feature-progress/         # Charts, trends, reports
│   ├── feature-gamification/     # XP, badges, quests, leaderboard
│   └── feature-settings/
├── domain/
│   ├── domain-nutrition/         # Use cases nutrisi
│   ├── domain-energy/            # BMR, TDEE, MET calculators
│   ├── domain-tracking/          # Distance, pace, elevation processors
│   └── domain-health/            # Health score, indicators
└── data/
    ├── data-food/                # Food repository + remote/local sources
    ├── data-activity/
    ├── data-user/
    └── data-healthconnect/       # Health Connect bridge
```

### 6.3 Prinsip Arsitektur

- **Single source of truth** — Room adalah SoT; network hanya mengisi cache
- **Repository pattern** — `Flow<List<T>>` dari DB, network refresh terpisah
- **Domain layer bebas framework** — pure Kotlin, mudah di-test, siap KMP
- **UDF** — State turun, Event naik. Satu `UiState` data class per screen
- **Feature isolation** — feature module tidak saling depend, komunikasi lewat navigation route
- **Offline-first** — semua fitur inti berfungsi tanpa internet

### 6.4 Contoh Struktur State

```kotlin
data class DashboardUiState(
    val isLoading: Boolean = false,
    val date: LocalDate = LocalDate.now(),
    val energyBudget: EnergyBudget = EnergyBudget.EMPTY,
    val macros: MacroProgress = MacroProgress.EMPTY,
    val meals: List<MealSummary> = emptyList(),
    val steps: Int = 0,
    val waterMl: Int = 0,
    val activities: List<ActivitySummary> = emptyList(),
    val streak: Int = 0,
    val error: UiText? = null,
)

data class EnergyBudget(
    val targetKcal: Int,
    val consumedKcal: Int,
    val burnedNetKcal: Int,
    val bmrKcal: Int,
) {
    val remainingKcal get() = targetKcal - consumedKcal + burnedNetKcal
    val progress get() = (consumedKcal.toFloat() / targetKcal).coerceIn(0f, 1.5f)
}
```

---

## 7. Data Model & Skema Database

### 7.1 Entity Utama

```kotlin
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val sex: Sex,                        // MALE, FEMALE
    val birthDate: LocalDate,
    val heightCm: Float,
    val activityLevel: ActivityLevel,
    val goal: Goal,                      // LOSE, MAINTAIN, GAIN
    val goalRateKgPerWeek: Float,
    val targetWeightKg: Float?,
    val unitSystem: UnitSystem,          // METRIC, IMPERIAL
    val useAsiaPacificBmi: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Entity(tableName = "food_item", indices = [Index("barcode"), Index("name")])
data class FoodItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val brand: String?,
    val barcode: String?,
    val source: FoodSource,              // USDA, OFF, LOCAL, USER
    val servingSizeG: Float,
    val servingLabel: String,            // "1 piring", "100 g"
    val kcalPer100g: Float,
    val proteinPer100g: Float,
    val carbsPer100g: Float,
    val fatPer100g: Float,
    val fiberPer100g: Float?,
    val sugarPer100g: Float?,
    val sodiumMgPer100g: Float?,
    val satFatPer100g: Float?,
    val isVerified: Boolean,
    val usageCount: Int = 0,
)

@Entity(tableName = "food_log", indices = [Index("date"), Index("foodItemId")])
data class FoodLogEntity(
    @PrimaryKey val id: String,
    val foodItemId: String,
    val date: LocalDate,
    val mealType: MealType,              // BREAKFAST, LUNCH, DINNER, SNACK
    val quantity: Float,
    val unit: String,
    val kcal: Float,                     // snapshot, tidak ikut berubah jika master data berubah
    val proteinG: Float,
    val carbsG: Float,
    val fatG: Float,
    val loggedAt: Instant,
)

@Entity(tableName = "activity_session", indices = [Index("startTime")])
data class ActivitySessionEntity(
    @PrimaryKey val id: String,
    val sportType: SportType,
    val source: ActivitySource,          // GPS, MANUAL, HEALTH_CONNECT, IMPORT
    val startTime: Instant,
    val endTime: Instant,
    val elapsedSeconds: Long,
    val movingSeconds: Long,
    val distanceMeters: Double?,
    val avgSpeedMps: Double?,
    val maxSpeedMps: Double?,
    val elevationGainM: Double?,
    val elevationLossM: Double?,
    val maxAltitudeM: Double?,
    val avgHeartRate: Int?,
    val maxHeartRate: Int?,
    val avgCadence: Int?,
    val kcalGross: Int,
    val kcalNet: Int,
    val metValue: Float?,
    val trainingLoad: Float?,
    val title: String?,
    val note: String?,
    val encodedPolyline: String?,        // rute terkompresi untuk preview
    val isSynced: Boolean = false,
)

@Entity(
    tableName = "location_point",
    foreignKeys = [ForeignKey(
        entity = ActivitySessionEntity::class,
        parentColumns = ["id"], childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId")]
)
data class LocationPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val latitude: Double,
    val longitude: Double,
    val altitudeM: Double?,
    val accuracyM: Float,
    val speedMps: Float?,
    val bearing: Float?,
    val timestamp: Long,
    val isFiltered: Boolean = false,     // ditandai outlier
)

@Entity(tableName = "body_measurement", indices = [Index("date")])
data class BodyMeasurementEntity(
    @PrimaryKey val id: String,
    val date: LocalDate,
    val weightKg: Float?,
    val bodyFatPercent: Float?,
    val waistCm: Float?,
    val hipCm: Float?,
    val chestCm: Float?,
    val armCm: Float?,
    val thighCm: Float?,
    val restingHeartRate: Int?,
    val hrvMs: Float?,
    val systolic: Int?,
    val diastolic: Int?,
    val sleepMinutes: Int?,
    val photoUri: String?,
    val note: String?,
)

@Entity(tableName = "gamification_state")
data class GamificationStateEntity(
    @PrimaryKey val userId: String,
    val totalXp: Long,
    val level: Int,
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val lastActiveDate: LocalDate?,
    val streakFreezesRemaining: Int,
)

@Entity(tableName = "achievement")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val category: AchievementCategory,
    val tier: Tier,                      // BRONZE, SILVER, GOLD, PLATINUM
    val progress: Float,                 // 0.0 – 1.0
    val unlockedAt: Instant?,
)

@Entity(tableName = "personal_record", indices = [Index(value = ["sportType", "recordType"])])
data class PersonalRecordEntity(
    @PrimaryKey val id: String,
    val sportType: SportType,
    val recordType: RecordType,          // FASTEST_1K, FASTEST_5K, LONGEST_DISTANCE, MOST_ELEVATION
    val value: Double,
    val sessionId: String,
    val achievedAt: Instant,
)
```

### 7.2 Strategi Penyimpanan GPS

- Sampling 1 Hz saat aktif → ~3.600 titik per jam
- Simpan raw points selama sesi berjalan (Room, batched insert per 10 titik)
- Setelah sesi selesai: jalankan smoothing, simpan versi terfilter, encode polyline untuk preview peta
- Retensi: raw points bisa di-*downsample* (Douglas–Peucker) setelah 90 hari untuk hemat storage
- Estimasi ukuran: ~50 byte/titik → aktivitas 2 jam ≈ 360 KB (aman)

---

## 8. GPS Tracking Engine

### 8.1 Konfigurasi Location Request

```kotlin
val locationRequest = LocationRequest.Builder(
    Priority.PRIORITY_HIGH_ACCURACY,
    1000L                              // interval 1 detik
).apply {
    setMinUpdateIntervalMillis(500L)
    setMinUpdateDistanceMeters(0f)     // jangan filter di sini, filter di logic sendiri
    setWaitForAccurateLocation(true)
    setMaxUpdateDelayMillis(2000L)     // izinkan sedikit batching, hemat baterai
}.build()
```

**Profil sampling per sport:**

| Sport | Interval | Alasan |
|---|---|---|
| Running / Trail | 1 s | Butuh pace responsif |
| Cycling | 1 s | Kecepatan tinggi, perubahan cepat |
| Walking / Hiking | 3 s | Pergerakan lambat, hemat baterai |
| Trekking / Climbing | 5 s | Sesi sangat panjang, prioritas baterai |
| Horse Riding | 2 s | Menengah |

### 8.2 Foreground Service

Wajib untuk merekam saat layar mati atau app di background.

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
<uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<service
    android:name=".tracking.TrackingService"
    android:foregroundServiceType="location|health"
    android:exported="false" />
```

**Catatan penting (Android 14+):**
- `foregroundServiceType` wajib dideklarasikan, dan sistem memverifikasi runtime permission yang sesuai
- Service tipe `location` mensyaratkan `ACCESS_COARSE_LOCATION` atau `ACCESS_FINE_LOCATION` sudah diberikan
- Notifikasi persisten wajib — jadikan informatif: durasi, jarak, pace, tombol Pause/Stop
- Jangan start foreground service dari background tanpa user action

### 8.3 Pipeline Pemrosesan Lokasi

```
Raw GPS Fix
   ↓
[1] Accuracy Filter      → buang jika accuracy > 25 m (running) / > 50 m (hiking)
   ↓
[2] Warm-up Discard      → buang 3 fix pertama (GPS belum settle)
   ↓
[3] Speed Sanity Check   → buang jika implied speed > threshold sport
                           (running 8 m/s, cycling 25 m/s, hiking 4 m/s)
   ↓
[4] Kalman Filter        → smoothing posisi, kurangi jitter
   ↓
[5] Distance Accumulate  → Haversine antar titik berurutan
   ↓
[6] Altitude Smoothing   → moving average 5 titik / barometer jika ada
   ↓
[7] Elevation Gain       → hanya akumulasi Δ > 3 m
   ↓
[8] Auto-Pause Detection → speed < threshold selama > 5 s
   ↓
[9] Persist + Emit State → Room insert (batched) + StateFlow ke UI
```

**Implementasi Kalman sederhana (1D per koordinat):**

```kotlin
class KalmanLocationFilter(private val processNoise: Float = 3f) {
    private var variance = -1f
    private var lat = 0.0; private var lng = 0.0
    private var timestamp = 0L

    fun process(newLat: Double, newLng: Double, accuracy: Float, time: Long): Pair<Double, Double> {
        val acc = accuracy.coerceAtLeast(1f)
        if (variance < 0) {
            lat = newLat; lng = newLng; timestamp = time; variance = acc * acc
        } else {
            val dt = (time - timestamp) / 1000f
            if (dt > 0) { variance += dt * processNoise * processNoise; timestamp = time }
            val k = variance / (variance + acc * acc)
            lat += k * (newLat - lat)
            lng += k * (newLng - lng)
            variance *= (1 - k)
        }
        return lat to lng
    }
}
```

### 8.4 Auto-Pause Threshold

| Sport | Pause jika speed < | Resume jika speed > | Delay |
|---|---|---|---|
| Running | 0.5 m/s | 1.0 m/s | 5 s |
| Walking | 0.3 m/s | 0.6 m/s | 8 s |
| Cycling | 1.0 m/s | 2.0 m/s | 5 s |
| Hiking | 0.2 m/s | 0.5 m/s | 15 s |
| Climbing | disabled | — | — |

### 8.5 Optimasi Baterai

- Gunakan `setMaxUpdateDelayMillis` agar OS bisa *batch* fix
- Matikan animasi peta saat layar mati (gunakan `Lifecycle` observer)
- Wake lock **partial** saja, jangan `SCREEN_BRIGHT_WAKE_LOCK`
- Nonaktifkan network call selama sesi; upload ditunda sampai aktivitas selesai
- Turunkan sampling rate otomatis jika baterai < 15%
- Target: konsumsi < 8% baterai per jam tracking
- Test khusus di device dengan battery optimization agresif (Xiaomi MIUI, Oppo ColorOS, Samsung One UI) — arahkan user ke pengaturan "unrestricted battery" saat pertama kali tracking

### 8.6 Crash Recovery

```kotlin
// Persist state ringkas tiap 5 detik ke DataStore
data class TrackingSnapshot(
    val sessionId: String,
    val sportType: SportType,
    val startTime: Long,
    val lastPointTime: Long,
    val distanceMeters: Double,
    val movingSeconds: Long,
    val isPaused: Boolean,
)
// Saat app dibuka: cek snapshot. Jika ada & < 6 jam → tawarkan "Lanjutkan aktivitas?"
```

---

## 9. Sistem Gamifikasi

### 9.1 Prinsip Desain

Berbasis kerangka Octalysis, fokus pada core drive yang sehat:
- **Development & Accomplishment** — progress terlihat (XP, level, PR)
- **Ownership & Possession** — koleksi badge, statistik pribadi
- **Scarcity & Impatience** — quest mingguan terbatas waktu
- **Unpredictability** — bonus XP acak, badge tersembunyi

❌ **Dihindari:** loss aversion menyakitkan (kehilangan progress), tekanan sosial toksik, leaderboard berbasis defisit kalori terdalam.

### 9.2 XP System

| Aksi | XP |
|---|---|
| Log 1 meal | +10 |
| Log lengkap 1 hari (3 meal) | +30 bonus |
| Log berat badan | +15 |
| Capai target air minum | +20 |
| Capai target langkah | +25 |
| Selesaikan aktivitas GPS | +50 base |
| Per km jarak tempuh | +10 |
| Per 100 m elevation gain | +15 |
| Pecahkan personal record | +150 |
| Selesaikan quest mingguan | +200 |
| Dalam target kalori (±100 kkal) | +40 |
| Streak milestone (7/30/100/365 hari) | +100/500/2000/10000 |

**Kurva Level (progresif):**

```
XP_untuk_level(n) = 100 × n^1.5

Level 1 → 2 :   283 XP
Level 5 → 6 :  1.470 XP
Level 10→11 :  3.640 XP
Level 25→26 : 13.750 XP
Level 50    : 35.355 XP kumulatif
```

**Tier Level:**
Bronze (1–9) · Silver (10–24) · Gold (25–49) · Platinum (50–74) · Diamond (75–99) · Legend (100+)

### 9.3 Streak System

- Streak bertambah jika user log **minimal 1 meal ATAU 1 aktivitas** per hari
- **Streak Freeze** — 1 freeze gratis per 14 hari streak (maks. simpan 3). Melindungi dari 1 hari terlewat.
- **Grace period** — sampai jam 04:00 keesokan hari masih dihitung hari sebelumnya
- Visual: api yang membesar tiap milestone, dengan animasi glass shimmer

### 9.4 Achievement / Badge

**Kategori Jarak (per sport):**
`First Steps` 1 km · `Getting Started` 10 km · `Road Warrior` 100 km · `Century` 500 km · `Marathoner` 1000 km · `Ultra` 5000 km

**Kategori Elevation:**
`Hill Starter` 100 m · `Climber` 1.000 m · `Everest` 8.848 m kumulatif · `Above the Clouds` 29.029 m

**Kategori Konsistensi:**
`Week One` streak 7 · `Habit Formed` streak 30 · `Centurion` streak 100 · `Year of You` streak 365

**Kategori Nutrisi:**
`Macro Master` 7 hari makro dalam target · `Hydrated` 30 hari target air · `Protein Pro` 14 hari protein tercapai

**Kategori Tersembunyi (Easter Egg):**
`Night Owl` — aktivitas antara 00:00–04:00
`Sunrise Chaser` — 10 aktivitas mulai sebelum jam 6 pagi
`Rain or Shine` — aktivitas 7 hari berturut-turut
`Explorer` — aktivitas di 10 kecamatan berbeda
`Summit Seeker` — capai titik > 2.000 mdpl

Setiap badge punya tier Bronze → Platinum dengan progress bar.

### 9.5 Quest Mingguan

Digenerate tiap Senin 00:00 berdasarkan histori user (adaptif, tidak terlalu mudah/sulit):

```
Contoh quest:
□ Tempuh 15 km minggu ini              [ 8.4 / 15 km ]   +200 XP
□ Log makanan 5 hari                    [ 3 / 5 hari  ]   +150 XP
□ Naik total 300 m elevasi              [ 120 / 300 m ]   +180 XP
□ Capai target protein 4 hari           [ 2 / 4 hari  ]   +150 XP
```

Selesaikan semua quest → bonus chest +500 XP + badge mingguan.

### 9.6 Personal Records

Otomatis dideteksi per sport:
Fastest 1K · 5K · 10K · Half Marathon · Marathon · Longest Distance · Longest Duration · Most Elevation Gain · Fastest Average Pace · Highest Altitude

Saat PR pecah → animasi konfeti + kartu shareable.

### 9.7 Visualisasi Progress

- **Ring Dashboard** — 3 cincin konsentris ala Apple: Kalori · Aktivitas · Langkah
- **Heatmap Kalender** — kotak per hari, intensitas warna = level aktivitas
- **Journey Map** — total jarak dipetakan ke rute nyata ("Kamu sudah menempuh Jakarta–Bandung!")
- **Level Card** — kartu glass dengan gradient sesuai tier, bisa di-share ke IG Story

---

## 10. Design System — Liquid Glass UI

### 10.1 Filosofi Desain

Estetika **glassmorphism** ala Apple: lapisan translusen, blur backdrop, border tipis bercahaya, kedalaman berlapis. Tapi dengan tiga aturan tegas:

1. **Konten selalu menang atas efek** — jika kontras teks turun di bawah 4.5:1, tebalkan tint sampai lolos
2. **Glass hanya untuk permukaan mengambang** — bottom bar, top bar, modal, card utama. Bukan untuk semua elemen
3. **Selalu ada fallback** — perangkat lama & mode hemat baterai dapat solid surface

### 10.2 Color Tokens

```kotlin
// Base background — gradient mesh yang jadi "sumber cahaya" glass
val BgGradientDark = Brush.linearGradient(
    listOf(Color(0xFF0A0E1A), Color(0xFF131A2E), Color(0xFF0F1420))
)
val AccentMesh = listOf(
    Color(0xFF6C5CE7).copy(alpha = 0.35f),   // ungu
    Color(0xFF00D2FF).copy(alpha = 0.28f),   // cyan
    Color(0xFFFF6B9D).copy(alpha = 0.22f),   // pink
)

// Glass surface layers
object Glass {
    val Fill        = Color.White.copy(alpha = 0.08f)
    val FillStrong  = Color.White.copy(alpha = 0.14f)
    val FillSubtle  = Color.White.copy(alpha = 0.05f)
    val Border      = Color.White.copy(alpha = 0.18f)
    val BorderTop   = Color.White.copy(alpha = 0.32f)   // highlight sisi atas
    val Shadow      = Color.Black.copy(alpha = 0.35f)
}

// Semantic
val CalorieRing   = Color(0xFFFF6B4A)
val ProteinColor  = Color(0xFF4ADE80)
val CarbColor     = Color(0xFF60A5FA)
val FatColor      = Color(0xFFFBBF24)
val SuccessColor  = Color(0xFF34D399)
val WarningColor  = Color(0xFFFBBF24)
val DangerColor   = Color(0xFFF87171)
```

### 10.3 Typography

```kotlin
// Display metrik besar — angka harus dominan
val MetricHuge   = TextStyle(fontSize = 64.sp, fontWeight = FontWeight.Bold,     letterSpacing = (-2).sp)
val MetricLarge  = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1).sp)
val MetricMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
val LabelCaps    = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.2.sp)
```

Font: **Inter** atau **SF Pro–like** (Manrope / Plus Jakarta Sans). Angka wajib **tabular figures** (`FontFeatureSetting("tnum")`) supaya tidak "goyang" saat live update.

### 10.4 Elevation Layers

| Layer | Blur Radius | Fill Alpha | Border | Contoh Penggunaan |
|---|---|---|---|---|
| L0 Background | — | — | — | Gradient mesh dasar |
| L1 Card | 12 dp | 0.06 | 0.12 | Card statistik |
| L2 Elevated | 20 dp | 0.10 | 0.18 | Card utama, ring container |
| L3 Navigation | 32 dp | 0.14 | 0.22 | Bottom bar, top app bar |
| L4 Modal | 40 dp | 0.18 | 0.28 | Bottom sheet, dialog |

### 10.5 Implementasi Glass di Compose

**Pendekatan A — Native blur (Android 12+, API 31+):**

```kotlin
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    layer: GlassLayer = GlassLayer.L2,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val reduceMotion = LocalAccessibilityManager.current?.let { /* cek setting */ } ?: false

    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (supportsBlur && !reduceMotion) {
                    Modifier.graphicsLayer {
                        renderEffect = RenderEffect
                            .createBlurEffect(layer.blurPx, layer.blurPx, Shader.TileMode.CLAMP)
                            .asComposeRenderEffect()
                    }
                } else Modifier
            )
            .background(
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = layer.fillAlpha * 1.4f),
                        Color.White.copy(alpha = layer.fillAlpha * 0.6f),
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite,
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(Glass.BorderTop, Glass.Border, Color.Transparent)
                ),
                shape = shape,
            ),
        content = content,
    )
}
```

**Pendekatan B — Backdrop blur (efek "kaca sungguhan"):**

`Modifier.blur()` dan `RenderEffect` hanya mem-blur konten komposabel itu sendiri, bukan yang di belakangnya. Untuk *backdrop blur* sejati (seperti CSS `backdrop-filter`), gunakan library:

| Library | Kelebihan | Batasan |
|---|---|---|
| **Haze** (chrishorner/dev.chrisbanes.haze) | Paling matang, API bersih, backdrop blur asli | Optimal di Android 12+ |
| **Cloudy** (skydoves) | Blur + liquid glass, ada CPU fallback untuk device lama | Fallback lebih berat |
| **liquid-glass-compose** (Mortd3kay) | AGSL shader, distorsi & refraksi ala iOS 26 | Butuh Android 13+ |
| Manual AGSL | Kontrol penuh, efek refraksi custom | Android 13+, effort tinggi |

**Rekomendasi:** Haze sebagai fondasi + AGSL shader kustom untuk hero element (ring dashboard, level card).

**Pendekatan C — Fake glass (fallback semua device):**
Gradient semi-transparan + noise texture overlay + inner shadow + border highlight. Tanpa blur sama sekali, tapi tetap terlihat "kaca". Gunakan untuk API < 31 dan mode hemat baterai.

### 10.6 Aturan Performa Glass ⚠️

Blur itu **mahal secara GPU**. Aturan wajib:

- Maksimal **3 permukaan glass ter-blur** yang terlihat bersamaan
- **JANGAN** pakai glass di item `LazyColumn` yang di-scroll — gunakan solid/fake glass
- **JANGAN** blur pada layar tracking aktif (GPS + peta + blur = baterai habis & frame drop)
- Cache hasil blur untuk konten statis
- Target: 60 fps di device mid-range (Snapdragon 6-series / Helio G-series)
- Sediakan toggle Settings: **"Efek Visual: Penuh / Ringan / Mati"**
- Otomatis turun ke mode Ringan jika: API < 31, battery saver aktif, atau RAM < 4 GB

### 10.7 Motion & Animation

| Interaksi | Spesifikasi |
|---|---|
| Screen transition | Shared element transition, 300 ms, `FastOutSlowInEasing` |
| Card press | Scale 0.97, 120 ms spring |
| Ring progress | `animateFloatAsState`, spring `dampingRatio = 0.75`, `stiffness = Low` |
| Number counter | Rolling digit animation, 600 ms |
| Achievement unlock | Konfeti + scale bounce + haptic `LongPress` |
| Pull to refresh | Glass ripple menyebar dari titik tarik |
| Bottom sheet | `SwipeableV2` dengan overshoot ringan |

Semua animasi wajib menghormati sistem **"Remove animations"** (accessibility).

### 10.8 Komponen Inti

```
GlassSurface        — container dasar dengan blur + border
GlassCard           — GlassSurface + padding + elevation standar
GlassBottomBar      — navigasi bawah, blur L3, indicator pill
GlassTopBar         — app bar dengan blur progresif saat scroll
GlassBottomSheet    — modal, blur L4, drag handle
EnergyRing          — cincin kalori animasi (Canvas custom)
TripleRing          — 3 cincin konsentris: kalori/aktivitas/langkah
MacroBar            — bar makro dengan segmen P/C/F
MetricTile          — angka besar + label + trend indicator
RouteMapCard        — preview peta rute dengan overlay glass
ElevationChart      — area chart elevasi (Canvas custom)
SplitBarChart       — bar chart split per km
StreakFlame         — animasi api dengan intensitas sesuai streak
XpProgressBar       — bar XP dengan shimmer
AchievementBadge    — badge dengan tier ring + unlock animation
FoodLogRow          — row makanan dengan swipe-to-delete
QuickAddFab         — FAB expandable ke 4 aksi cepat
```

### 10.9 Aksesibilitas

- Kontras teks minimal **4.5:1** di atas semua permukaan glass — verifikasi dengan tint terberat *dan* teringan
- Touch target minimal **48×48 dp**
- Content description lengkap untuk semua chart & ring ("Kalori: 1450 dari 2100, tersisa 650")
- Dukung font scaling sampai 200% tanpa layout pecah
- Mode **High Contrast** — matikan semua transparansi, gunakan solid surface
- TalkBack diuji pada semua flow inti
- Jangan sampaikan informasi hanya lewat warna (macro bar butuh label juga)

---

## 11. Navigasi & UX Flow

### 11.1 Peta Navigasi

```
┌─ Onboarding (first launch only)
│   Welcome → Goal → Profil Fisik → Level Aktivitas
│   → Target → Permission Primer → Hasil Kalkulasi → Dashboard
│
└─ Main (Bottom Navigation, 5 tab)
    │
    ├─ 🏠 Today (Dashboard)
    │   ├─ Energy Ring + sisa kalori
    │   ├─ Ringkasan makro
    │   ├─ Meal cards (Sarapan/Siang/Malam/Snack)
    │   ├─ Water tracker
    │   ├─ Langkah & aktivitas hari ini
    │   └─ Streak + XP bar
    │
    ├─ 📖 Diary
    │   ├─ Date picker horizontal
    │   ├─ Detail per meal → Food Search → Food Detail → Add
    │   ├─ Barcode Scanner
    │   └─ Nutrition breakdown (harian/mingguan)
    │
    ├─ ▶️ Track  ← FAB tengah, menonjol
    │   ├─ Sport Selector (grid glass card)
    │   ├─ Pre-Start (GPS signal, target opsional, gembok layar)
    │   ├─ LIVE TRACKING (fullscreen, no bottom bar)
    │   │   ├─ Metrik utama (jarak besar)
    │   │   ├─ Swipe → Peta view
    │   │   ├─ Swipe → Metrik detail
    │   │   └─ Pause / Lap / Stop
    │   └─ Activity Summary → Save/Edit/Share/Discard
    │
    ├─ 📊 Progress
    │   ├─ Berat & body metrics + grafik
    │   ├─ Health Score breakdown
    │   ├─ Riwayat aktivitas (list + filter sport)
    │   ├─ Heatmap kalender
    │   ├─ Personal Records
    │   └─ Laporan mingguan/bulanan
    │
    └─ 👤 Profile
        ├─ Level, XP, badge collection
        ├─ Quest mingguan
        ├─ Edit profil & target
        ├─ Integrasi (Health Connect, wearable)
        ├─ Settings (unit, notifikasi, efek visual, privasi)
        └─ Export data / Hapus akun
```

### 11.2 Prinsip UX Kunci

| Prinsip | Implementasi |
|---|---|
| **Aksi utama ≤ 3 tap** | Dashboard → tap meal → tap makanan recent → tersimpan |
| **Progressive disclosure** | Metrik lanjutan disembunyikan di balik "Lihat detail" |
| **Permission on demand** | Minta izin lokasi saat user tekan Start, bukan saat onboarding |
| **Empty state yang mengajak** | Ilustrasi + CTA jelas, bukan layar kosong |
| **Optimistic UI** | Item muncul langsung sebelum DB confirm, rollback jika gagal |
| **Undo, bukan konfirmasi** | Hapus langsung + snackbar "Undo" 5 detik |
| **Satu angka dominan** | Tiap layar punya 1 metrik hero, sisanya sekunder |
| **Haptic feedback** | Setiap milestone, lap, PR, dan tombol utama |

### 11.3 Permission Priming

Jangan langsung tembak system dialog. Tampilkan layar penjelasan dulu:

```
[Ikon lokasi glass]

"Agar bisa merekam rutemu"

VITALIS butuh akses lokasi untuk mengukur jarak,
pace, dan elevasi saat kamu berolahraga.

Lokasimu hanya disimpan di perangkat ini dan
tidak pernah dibagikan tanpa izinmu.

        [ Lanjutkan ]    [ Nanti saja ]
```

Untuk Android 10+, `ACCESS_BACKGROUND_LOCATION` **tidak diperlukan** jika memakai foreground service dengan tipe `location` — jangan minta izin ini, akan menaikkan tingkat penolakan Play Store review.

---

## 12. Integrasi Eksternal & API

### 12.1 Health Connect

Pengganti resmi Google Fit (Google Fit API hanya didukung sampai akhir 2026).

```kotlin
dependencies {
    implementation("androidx.health.connect:connect-client:1.1.0")
}
```

**Data yang dibaca:**
`StepsRecord` · `WeightRecord` · `HeightRecord` · `BodyFatRecord` · `HeartRateRecord` · `RestingHeartRateRecord` · `SleepSessionRecord` · `ExerciseSessionRecord` · `TotalCaloriesBurnedRecord` · `HydrationRecord` · `OxygenSaturationRecord`

**Data yang ditulis:**
`ExerciseSessionRecord` (+ `ExerciseRoute` untuk rute GPS) · `ActiveCaloriesBurnedRecord` · `DistanceRecord` · `SpeedRecord` · `NutritionRecord` · `HydrationRecord` · `WeightRecord`

**Best practice:**
- Jaga `sessionId` konsisten di semua write dalam satu sesi olahraga
- Tulis data secara bertahap selama sesi berlangsung, bukan sekaligus di akhir (mencegah kehilangan data jika app di-kill)
- Deklarasikan data type yang diakses di Play Console
- Deduplikasi: tandai record buatan sendiri dengan `Metadata.clientRecordId` agar tidak dibaca balik sebagai aktivitas terpisah
- Background read butuh permission `READ_HEALTH_DATA_IN_BACKGROUND` (API 36+)

### 12.2 Food Database API

| Provider | Model | Kekuatan | Batasan |
|---|---|---|---|
| **USDA FoodData Central** | Gratis, butuh API key | Data laboratorium, paling akurat untuk makanan generik | Tidak ada barcode lookup, minim produk bermerek |
| **Open Food Facts** | Gratis, tanpa key, unlimited | 3 juta+ produk kemasan dari 180+ negara, barcode lengkap | Crowdsourced — kualitas bervariasi, banyak field kosong |
| **Nutritionix** | Freemium (±500 request/hari gratis) | NLP query, coverage restoran AS kuat | Enterprise mahal |
| **FatSecret** | Free tier lumayan | Database matang, barcode, multi-bahasa | Butuh OAuth |
| **Edamam** | Freemium | Analisis resep bagus | Butuh kartu kredit bahkan untuk free tier |

**Arsitektur repository yang direkomendasikan:**

```kotlin
class FoodRepository @Inject constructor(
    private val local: FoodDao,
    private val usda: UsdaApi,
    private val off: OpenFoodFactsApi,
) {
    fun search(query: String): Flow<List<FoodItem>> = flow {
        emit(local.search(query))                       // 1. instant dari cache
        val remote = coroutineScope {
            val a = async { runCatching { usda.search(query) }.getOrDefault(emptyList()) }
            val b = async { runCatching { off.search(query) }.getOrDefault(emptyList()) }
            (a.await() + b.await()).dedupeByNameAndBrand()
        }
        local.upsertAll(remote)                         // 2. cache untuk offline
        emit(local.search(query))
    }.flowOn(Dispatchers.IO)

    suspend fun lookupBarcode(code: String): FoodItem? =
        local.findByBarcode(code)
            ?: off.getProduct(code)?.also { local.upsert(it) }
}
```

> ⚠️ Open Food Facts mengembalikan HTTP 200 bahkan untuk barcode yang tidak ada — selalu cek field `status` di body, jangan hanya andalkan HTTP code.

### 12.3 Maps

| Opsi | Pertimbangan |
|---|---|
| **Google Maps Compose** | Kualitas terbaik, integrasi mulus, tapi berbayar setelah kuota gratis |
| **MapLibre GL Native** | Open source, gratis, tile dari MapTiler/Stadia, butuh setup lebih |
| **OSMDroid** | Gratis penuh, ringan, tapi tidak Compose-native |

Untuk personal project: mulai dengan **Google Maps** (kuota gratis $200/bulan sangat cukup), siapkan abstraksi `MapProvider` interface agar mudah pindah ke MapLibre jika biaya naik.

**Fitur peta yang diperlukan:**
- Polyline rute dengan gradient warna (pace/elevation/HR)
- Marker start/finish/lap/summit
- Camera follow mode saat tracking
- Snapshot statis untuk thumbnail di list aktivitas
- Offline map (v1.2, untuk hiking/climbing di area tanpa sinyal)

### 12.4 Sensor Tambahan

| Sensor | Kegunaan | Ketersediaan |
|---|---|---|
| `TYPE_STEP_COUNTER` | Langkah harian, hemat baterai | Umum |
| `TYPE_PRESSURE` (barometer) | Elevasi akurat | Flagship/mid-high |
| `TYPE_ACCELEROMETER` | Cadence, deteksi gerakan | Semua |
| `TYPE_HEART_RATE` | HR onboard | Jarang di HP |
| BLE Heart Rate Profile | HR dari chest strap / smartwatch | Via Bluetooth |
| BLE Cycling Speed & Cadence | Cadence sepeda | Via Bluetooth |

---

## 13. Permission, Privacy & Compliance

### 13.1 Daftar Permission

```xml
<!-- Lokasi -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

<!-- Foreground Service -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_HEALTH" />

<!-- Aktivitas & Sensor -->
<uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />
<uses-permission android:name="android.permission.BODY_SENSORS" />

<!-- Bluetooth (opsional, untuk HR strap) -->
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />

<!-- Lain-lain -->
<uses-permission android:name="android.permission.CAMERA" />          <!-- barcode -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WAKE_LOCK" />

<uses-feature android:name="android.hardware.location.gps" android:required="false" />
<uses-feature android:name="android.hardware.sensor.barometer" android:required="false" />
```

### 13.2 Prinsip Privasi

1. **Local-first** — semua data disimpan di perangkat secara default. Cloud sync opsional dan opt-in.
2. **Enkripsi at-rest** — Room + SQLCipher, kunci di Android Keystore.
3. **Tidak ada tracking pihak ketiga** untuk data kesehatan. Analytics hanya event agregat, tanpa PII.
4. **Data minimization** — jangan kumpulkan yang tidak dipakai.
5. **Full export** — user bisa export semua data (JSON + GPX) kapan saja.
6. **Hapus akun = hapus data** — permanen, dalam 30 hari, dengan konfirmasi jelas.

### 13.3 Play Store Compliance

- **Health Apps Declaration** wajib diisi di Play Console
- **Data safety form** harus akurat — Google mengaudit
- **Sensitive permission justification** untuk lokasi dan health data
- Privacy Policy publik wajib (URL harus aktif sebelum submit)
- Disclaimer medis di app dan di store listing:
  > "VITALIS adalah alat bantu kebugaran dan bukan perangkat medis. Informasi yang disediakan tidak menggantikan saran, diagnosis, atau perawatan dari tenaga kesehatan profesional."

### 13.4 Penanganan Data Sensitif

- Rute GPS bisa mengungkap alamat rumah → tawarkan **privacy zone** (radius 200 m di sekitar titik yang ditandai user disembunyikan dari rute yang dibagikan)
- Shared image tidak boleh menyertakan koordinat presisi
- Progress photo disimpan di internal storage app, tidak di MediaStore publik

---

## 14. Offline-First & Sinkronisasi

### 14.1 Strategi

```
UI  ←─ StateFlow ──  Repository  ←─ Room (Single Source of Truth)
                          ↑
                    SyncWorker (WorkManager)
                          ↓
                     Remote API
```

- Semua tulis operasi masuk Room **dulu**, ditandai `isSynced = false`
- `SyncWorker` periodik (15 menit) + trigger saat jaringan tersedia
- Constraint: `NetworkType.CONNECTED`, `requiresBatteryNotLow = true`
- Aktivitas GPS: upload ditunda sampai sesi selesai + terhubung Wi-Fi (opsional setting)

### 14.2 Conflict Resolution

| Tipe Data | Strategi |
|---|---|
| Food log | Last-write-wins per entry ID |
| Body measurement | Last-write-wins per (date, field) |
| Activity session | Immutable setelah save; edit membuat versi baru |
| Profile | Server timestamp menang |
| Gamification XP | Server-authoritative, client hanya optimistic display |

### 14.3 Fitur yang Wajib Jalan Offline

✅ Log makanan dari cache lokal
✅ Barcode scan (jika produk sudah pernah di-cache)
✅ GPS tracking penuh
✅ Semua kalkulasi kalori & metrik
✅ Melihat riwayat & grafik
✅ XP & badge (dihitung lokal, di-reconcile saat online)

❌ Search makanan baru dari server
❌ Peta area yang belum di-cache (kecuali offline map v1.2)

---

## 15. Testing & QA

### 15.1 Piramida Testing

| Level | Coverage Target | Fokus |
|---|---|---|
| Unit | 80%+ pada domain layer | Kalkulator BMR/TDEE/MET, distance, elevation, XP |
| Integration | Repository + DAO | Room queries, migrations, sync logic |
| UI | Flow kritis | Onboarding, log makanan, start-stop tracking |
| E2E | 5 skenario utama | Happy path lengkap |
| Manual | Device matrix | GPS akurasi, baterai, blur performance |

### 15.2 Test Case Kritis

**Kalkulasi:**
- BMR Mifflin-St Jeor untuk pria/wanita, edge case umur & berat ekstrem
- MET → kalori, verifikasi net vs gross
- Konversi unit metric ↔ imperial (round-trip tanpa drift)
- Distance Haversine vs referensi GPX yang diketahui
- Elevation gain dengan noise vs tanpa noise

**GPS Tracking:**
- Simulasi route dengan mock location provider
- Sesi 3 jam tanpa kehilangan data
- App di-kill saat tracking → recovery berhasil
- Kehilangan sinyal GPS (masuk terowongan) → jarak tidak melonjak
- Auto-pause di lampu merah
- Battery drain < 8%/jam

**Device Matrix Minimum:**

| Device | API | Alasan |
|---|---|---|
| Pixel (API 36) | 36 | Referensi Android murni terbaru |
| Samsung mid-range | 34 | One UI battery optimization |
| Xiaomi/Redmi | 33 | MIUI kill background agresif |
| Device API 26–29 | 26–29 | Fallback non-blur, no RenderEffect |
| Tablet | 34 | Layout responsif |

### 15.3 Performance Budget

| Metrik | Target |
|---|---|
| Cold start | < 1.5 s |
| Frame rendering | > 95% frame di bawah 16 ms |
| APK size | < 25 MB |
| Memory (tracking aktif) | < 180 MB |
| Battery (tracking) | < 8%/jam |
| Battery (idle harian) | < 2%/hari |

---

## 16. Analytics & KPI

### 16.1 Event yang Di-track (anonim, agregat)

```
app_open, onboarding_step_completed, onboarding_completed
food_logged (method: search|barcode|quick|recent|template)
meal_completed, day_fully_logged
activity_started (sport_type), activity_completed, activity_discarded
weight_logged, measurement_logged
achievement_unlocked, level_up, quest_completed, streak_milestone
permission_granted / denied (jenis)
error_occurred (kategori, tanpa PII)
```

### 16.2 KPI Utama

| Metrik | Target |
|---|---|
| D1 Retention | > 45% |
| D7 Retention | > 25% |
| D30 Retention | > 12% |
| Onboarding completion | > 75% |
| Avg. food log per hari (aktif) | > 2.5 |
| Avg. aktivitas GPS per minggu (aktif) | > 1.5 |
| Streak rata-rata | > 5 hari |
| Crash-free session rate | > 99.5% |
| Waktu logging 1 makanan | < 15 detik |

---

## 17. Estimasi Effort & Sprint Plan

Asumsi: 1 developer, part-time (~15 jam/minggu), 1 sprint = 2 minggu.

| Sprint | Fokus | Deliverable | Effort |
|---|---|---|---|
| 1 | Setup & fondasi | Project structure, DI, Room, CI | 30 h |
| 2 | Design system | Theme, glass components, komponen dasar | 30 h |
| 3 | Onboarding & profil | Flow lengkap + kalkulator BMR/TDEE | 30 h |
| 4 | Nutrition core | Food DAO, search, log manual, diary UI | 35 h |
| 5 | Food API & barcode | Integrasi USDA + OFF, CameraX + ML Kit | 35 h |
| 6 | Dashboard | Energy ring, macro bar, meal cards, water | 30 h |
| 7 | GPS engine | Foreground service, location pipeline, filter | 40 h |
| 8 | Tracking UI | Live screen, peta, summary, save | 35 h |
| 9 | Body & progress | Weight log, BMI, grafik, riwayat | 30 h |
| 10 | Gamification | XP, level, streak, badge, PR detection | 35 h |
| 11 | Health Connect | Read/write integration, dedup | 25 h |
| 12 | Sport tambahan | Hiking, climbing, riding + metrik khusus | 30 h |
| 13 | Health Score | Kalkulasi + UI breakdown | 25 h |
| 14 | Polish & QA | Bug fix, performance, accessibility, testing | 40 h |
| 15 | Release prep | Play Console, privacy policy, store assets | 20 h |

**Total estimasi: ~470 jam ≈ 8 bulan part-time**

---

## 18. Risiko & Mitigasi

| Risiko | Dampak | Probabilitas | Mitigasi |
|---|---|---|---|
| Blur menyebabkan frame drop di device murah | Tinggi | Tinggi | Tiered visual mode + fake glass fallback + budget 3 surface |
| GPS drift merusak akurasi jarak | Tinggi | Sedang | Kalman filter + accuracy gate + speed sanity check |
| Battery optimization OEM membunuh service | Tinggi | Tinggi | Crash recovery + edukasi user + deteksi kill + test di MIUI/ColorOS |
| Kualitas data Open Food Facts buruk | Sedang | Tinggi | Layered source + tampilkan sumber + izinkan user override |
| Play Store menolak karena health permission | Tinggi | Sedang | Isi deklarasi lengkap sejak awal, privacy policy siap, hindari background location |
| Scope creep (fitur terlalu banyak) | Tinggi | Tinggi | Kunci MVP scope, backlog terpisah, jangan mulai fitur di luar sprint |
| Kalori double-count membingungkan user | Sedang | Tinggi | Gunakan net calories + UI edukatif "apa itu net kalori?" |
| Biaya Google Maps naik | Rendah | Rendah | Abstraksi MapProvider, siap pindah ke MapLibre |
| Storage penuh karena GPS points | Rendah | Sedang | Downsampling setelah 90 hari + monitoring ukuran DB |
| Burnout personal project | Tinggi | Sedang | Rilis MVP kecil dulu, dogfood sendiri, iterasi berdasarkan pemakaian nyata |

---

## 19. Referensi

### Formula & Ilmu Olahraga
- Compendium of Physical Activities (MET values 2024) — https://pacompendium.com/
- 2024 Adult Compendium, Journal of Sport and Health Science — https://www.sciencedirect.com/science/article/pii/S2095254623001084
- Mifflin-St Jeor equation, Medscape calculator — https://reference.medscape.com/calculator/846/mifflin-st-jeor-equation
- ACSM Metabolic Equations (walking/running/cycling VO₂)
- Tanaka HRmax formula; Jack Daniels VDOT tables

### Android Development
- Health Connect overview — https://developer.android.com/health-and-fitness/health-connect
- Workout experiences with Health Connect — https://developer.android.com/health-and-fitness/health-connect/experiences/workouts
- Google Fit migration guide — https://developer.android.com/health-and-fitness/health-connect/migration/fit
- Foreground service types — https://developer.android.com/develop/background-work/services/fgs/service-types
- Optimize location use — https://developer.android.com/develop/sensors-and-location/location/battery/scenarios

### Food Database
- USDA FoodData Central — https://fdc.nal.usda.gov/
- Open Food Facts API — https://openfoodfacts.github.io/openfoodfacts-server/api/
- Food API comparison 2026 — https://blog.suggestic.com/food-api-ultimate-list

### UI / Glassmorphism
- Cloudy (skydoves) — https://github.com/skydoves/Cloudy
- liquid-glass-compose (AGSL shaders) — https://github.com/Mortd3kay/liquid-glass-compose
- RenderEffect & frosted glass in Compose — https://dev.to/myougatheaxo/blur-glassmorphism-effects-rendereffect-and-frosted-glass-ui-777
- Glassmorphic design with Jetpack Compose — https://proandroiddev.com/blurring-the-lines-how-to-achieve-a-glassmorphic-design-with-jetpack-compose-0225560c2d64

### Benchmark Aplikasi
Strava · MyFitnessPal · Nike Run Club · Komoot · AllTrails · Garmin Connect · Cronometer · Zepp

---

## Lampiran A — Checklist Pra-Rilis

```
[ ] Semua string di strings.xml (siap lokalisasi ID/EN)
[ ] Dark mode & light mode diuji
[ ] Font scaling 200% tidak memecah layout
[ ] TalkBack diuji di 5 flow inti
[ ] Kontras teks di atas glass lolos WCAG AA
[ ] Semua permission punya priming screen
[ ] Crash recovery tracking diuji (force stop mid-session)
[ ] Battery drain diukur di 3 device berbeda
[ ] Room migration diuji dari versi 1 → terbaru
[ ] ProGuard/R8 rules lengkap, release build diuji
[ ] Privacy Policy live & tertaut di app
[ ] Data Safety form Play Console diisi akurat
[ ] Health Apps Declaration disubmit
[ ] Disclaimer medis tampil di onboarding & settings
[ ] Export data (JSON + GPX) berfungsi
[ ] Hapus akun menghapus semua data lokal
[ ] Screenshot & feature graphic Play Store siap
[ ] Internal testing track dijalankan minimal 2 minggu
```

## Lampiran B — Struktur File JSON Aset

```
assets/
├── met_activities.json         # Katalog MET dari Compendium 2024
├── foods_id_seed.json          # ~500 makanan Indonesia kurasi manual
├── achievements.json           # Definisi semua badge & tier
├── quest_templates.json        # Template quest mingguan
└── sport_configs.json          # Konfigurasi GPS per sport type
```

**Contoh `met_activities.json`:**
```json
{
  "version": "2024-adult-compendium",
  "activities": [
    {
      "code": "12030",
      "category": "running",
      "nameId": "Lari, 8 km/jam",
      "nameEn": "Running, 5 mph",
      "met": 8.3,
      "speedRangeKmh": [7.8, 8.4]
    },
    {
      "code": "01050",
      "category": "bicycling",
      "nameId": "Sepeda, 19–22 km/jam",
      "nameEn": "Bicycling, 12–13.9 mph",
      "met": 8.0,
      "speedRangeKmh": [19.3, 22.4]
    },
    {
      "code": "17080",
      "category": "hiking",
      "nameId": "Hiking, lintas alam",
      "nameEn": "Hiking, cross country",
      "met": 6.0
    }
  ]
}
```

---

*Dokumen ini adalah living document. Update versi setiap kali scope atau keputusan arsitektur berubah.*
