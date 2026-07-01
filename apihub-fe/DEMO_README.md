# 🚀 APIHUB - API Test Otomasyon Platformu

## 📋 İçindekiler
- [Genel Bakış](#genel-bakış)
- [Temel Özellikler](#temel-özellikler)
- [Çalışma Prensibi](#çalışma-prensibi)
- [Kurulum](#kurulum)
- [Kullanım Kılavuzu](#kullanım-kılavuzu)
- [Modüller](#modüller)
- [Teknoloji Stack](#teknoloji-stack)
- [Demo için Hazırlık](#demo-için-hazırlık)

---

## 🎯 Genel Bakış

**APIHUB**, API test süreçlerini otomatikleştiren, performans analizleri yapan ve AI destekli bir yönetim platformudur. Geliştiricilerin ve test ekiplerinin API'leri kolayca test etmesini, izlemesini ve yönetmesini sağlar.

### Ana Amaç
- API test süreçlerini otomatikleştirmek
- Performans metriklerini gerçek zamanlı izlemek
- Test senaryolarını görsel olarak yönetmek
- AI destekli API analizi ve dokümantasyon

---

## ✨ Temel Özellikler

### 1. **Process Flow Yönetimi** 🔄
- API test akışlarını görsel olarak oluşturma
- Adım adım test senaryoları tanımlama
- Akışları kopyalama ve düzenleme
- Parametrik test yapılandırması

### 2. **Performans Testleri** ⚡
- Çoklu thread ile yük testleri
- Gerçek zamanlı performans metrikleri
- Geçmiş performans karşılaştırmaları
- Grafik ve chart'larla görselleştirme
- Min/Max/Ortalama süre analizleri

### 3. **API Executor** 🎯
- Canlı API test etme
- Request/Response görüntüleme
- Farklı ortamlarda test yapma
- HAR dosyası desteği

### 4. **AI Destekli Chat Bot** 🤖
- API dokümantasyonu analizi
- Akıllı soru-cevap sistemi
- HAR dosyası yükleme ve analiz
- Streaming yanıtlar

### 5. **Çoklu Proje Desteği** 📁
- Proje bazlı organizasyon
- Ortam yönetimi (Dev, Test, Prod)
- Sistem entegrasyonları
- Kullanıcı yetkilendirme

### 6. **Modern UI/UX** 🎨
- Tema özelleştirme (6+ renk teması)
- Açık/Koyu mod desteği
- Çoklu dil desteği (TR/EN)
- Responsive tasarım
- Material-UI bileşenleri

---

## 🔧 Çalışma Prensibi

### Mimari Yapı

```
┌─────────────────────────────────────────────────────────┐
│                    APIHUB Frontend                       │
│                    (Next.js 16)                          │
└─────────────────────┬───────────────────────────────────┘
                      │
                      │ REST API
                      │
┌─────────────────────▼───────────────────────────────────┐
│                  Backend Services                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ Process Flow │  │ Performance  │  │   AI Chat    │  │
│  │   Service    │  │   Service    │  │   Service    │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────┬───────────────────────────────────┘
                      │
                      │
┌─────────────────────▼───────────────────────────────────┐
│                    Database                              │
│         (Test Verileri, Metrikler, Geçmiş)              │
└─────────────────────────────────────────────────────────┘
```

### Veri Akışı

1. **Test Akışı Oluşturma**
   ```
   Kullanıcı → Process Flow Tanımla → Adımları Ekle → Parametreleri Ayarla
   ```

2. **Performans Testi Çalıştırma**
   ```
   Test Başlat → Thread Pool Oluştur → API'leri Çağır → 
   Metrikleri Topla → Sonuçları Görselleştir
   ```

3. **AI Chat Etkileşimi**
   ```
   Soru Sor → RAG Sistemi → Vektör Arama → 
   LLM İşleme → Streaming Yanıt
   ```

### Temel Kavramlar

#### **Process Flow (Akış)**
Bir API test senaryosunu temsil eder. Her akış:
- Benzersiz bir ID ve kısa kod içerir
- Birden fazla adımdan oluşur
- Belirli bir projeye aittir
- Aktif/Pasif durumu vardır

#### **Step (Adım)**
Bir akış içindeki tekil API çağrısıdır:
- HTTP method (GET, POST, PUT, DELETE)
- Endpoint URL
- Headers ve Body parametreleri
- Beklenen response validasyonları
- Sıralama bilgisi

#### **Performance Test**
Bir akışın performans analizi:
- Thread sayısı (eş zamanlı istek)
- Ramp-up period (yüklenme süresi)
- Ortam seçimi (Dev/Test/Prod)
- Gerçek zamanlı durum takibi

#### **Environment (Ortam)**
API'lerin çalıştığı sistemler:
- Base URL
- Authentication bilgileri
- Ortam tipi (Development, Testing, Production)

---

## 🚀 Kurulum

### Gereksinimler
- Node.js 20+
- npm veya yarn
- Backend API servisi (ayrı çalışmalı)

### Adımlar

1. **Projeyi Klonlayın**
```bash
git clone <repository-url>
cd apihub-frontend
```

2. **Bağımlılıkları Yükleyin**
```bash
npm install
# veya
yarn install
```

3. **Ortam Değişkenlerini Ayarlayın**
`.env.local` dosyası oluşturun:
```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
NEXT_PUBLIC_CHAT_API_URL=http://localhost:8081
```

4. **Geliştirme Sunucusunu Başlatın**
```bash
npm run dev
```

Uygulama `http://localhost:4054` adresinde çalışacaktır.

5. **Production Build**
```bash
npm run build
npm start
```

---

## 📖 Kullanım Kılavuzu

### 1. İlk Giriş

1. Uygulamayı açın: `http://localhost:4054`
2. Dashboard'a yönlendirileceksiniz
3. Sağ üstten proje seçin

### 2. Process Flow Oluşturma

**Adım 1: Yeni Akış Ekle**
- Process Flows sayfasına gidin
- "Yeni Akış Ekle" butonuna tıklayın
- Kısa kod girin (örn: "UserLoginFlow")
- Aktif/Pasif durumunu seçin
- Kaydet

**Adım 2: Adımları Tanımlayın**
- Oluşturulan akışın "Detay" butonuna tıklayın
- "Yeni Adım Ekle" butonuna tıklayın
- API detaylarını girin:
  - Method: GET, POST, PUT, DELETE
  - Endpoint: `/api/users/login`
  - Headers: `Content-Type: application/json`
  - Body: JSON formatında request body
- Sıralama numarasını belirleyin
- Kaydet

**Adım 3: Parametreleri Ayarlayın**
- Her adım için parametreler ekleyin
- Key-Value çiftleri tanımlayın
- Dinamik değerler için placeholder kullanın

### 3. Performans Testi Çalıştırma

**Test Başlatma:**
1. Performance sayfasına gidin
2. Ortam seçin (Dev/Test/Prod)
3. Test edilecek akışı seçin
4. Thread sayısını belirleyin (örn: 10)
5. Ramp-up period'u ayarlayın (örn: 5 saniye)
6. "Başlat" butonuna tıklayın

**Sonuçları İzleme:**
- Test durumu gerçek zamanlı güncellenir
- "Detay" butonuyla adım bazlı sonuçları görün
- Min/Max/Ortalama süreleri inceleyin

**Geçmiş Analizi:**
- "Geçmiş" butonuna tıklayın
- Önceki testleri karşılaştırın
- Grafik görünümüne geçin
- Trend analizleri yapın

### 4. API Executor Kullanımı

1. API Executor sayfasına gidin
2. Akış ve adım seçin
3. Parametreleri düzenleyin
4. "Çalıştır" butonuna tıklayın
5. Request/Response detaylarını inceleyin
6. Hata durumlarını kontrol edin

### 5. AI Chat Bot

**Soru Sorma:**
- Chat sayfasına gidin
- Mesaj kutusuna sorunuzu yazın
- Enter'a basın veya gönder butonuna tıklayın
- AI streaming yanıt verecektir

**HAR Dosyası Yükleme:**
- Ataşman butonuna tıklayın
- .har dosyası seçin
- Dosya otomatik analiz edilir
- AI dosya içeriği hakkında bilgi verir

**Örnek Sorular:**
- "Bu API'nin authentication mekanizması nedir?"
- "Performance test nasıl çalıştırılır?"
- "Yeni bir akış nasıl oluşturulur?"

### 6. Tema ve Dil Ayarları

**Tema Değiştirme:**
- Sağ üst köşedeki palet ikonuna tıklayın
- 6 farklı renk temasından birini seçin:
  - Indigo (Varsayılan)
  - Blue
  - Purple
  - Green
  - Orange
  - Pink

**Açık/Koyu Mod:**
- Güneş/Ay ikonuna tıklayın
- Mod otomatik değişir

**Dil Değiştirme:**
- Dil seçici butonuna tıklayın
- Türkçe (TR) veya İngilizce (EN) seçin

---

## 📦 Modüller

### 1. Dashboard
- Genel istatistikler
- Son aktiviteler
- Hızlı erişim kartları
- Sistem durumu

### 2. API Information
- API listesi
- Endpoint detayları
- Dokümantasyon
- Versiyon yönetimi

### 3. Process Flows
- Akış listesi
- Akış oluşturma/düzenleme
- Adım yönetimi
- Parametre konfigürasyonu

### 4. Systems (Ortamlar)
- Sistem listesi
- Ortam tanımlama
- Base URL yönetimi
- Authentication ayarları

### 5. Data Connections
- Veritabanı bağlantıları
- Connection string yönetimi
- Test bağlantıları

### 6. Performance
- Test başlatma
- Gerçek zamanlı izleme
- Geçmiş analizi
- Grafik görünümleri

### 7. API Executor
- Manuel test etme
- Request builder
- Response viewer
- Debug araçları

### 8. Documents
- API dokümantasyonu
- Kullanım kılavuzları
- Örnek kodlar
- Best practices

### 9. Chat (AI Bot)
- Soru-cevap
- HAR analizi
- Dokümantasyon arama
- Yardım sistemi

---

## 🛠️ Teknoloji Stack

### Frontend
- **Framework:** Next.js 16 (React 19)
- **UI Library:** Material-UI (MUI) v7
- **Styling:** TailwindCSS v4
- **State Management:** React Context API
- **HTTP Client:** Axios
- **Charts:** Recharts
- **Markdown:** React-Markdown
- **Drag & Drop:** @dnd-kit
- **i18n:** next-intl

### Backend (Ayrı Proje)
- Spring Boot
- PostgreSQL
- Redis
- OpenAI API
- RAG (Retrieval Augmented Generation)

### DevOps
- Git
- npm/yarn
- ESLint
- TypeScript

---

## 🎬 Demo için Hazırlık

### Demo Senaryosu

#### 1. Giriş ve Genel Bakış (2 dk)
- Dashboard'u göster
- Proje seçimini göster
- Tema değiştirmeyi göster

#### 2. Process Flow Oluşturma (5 dk)
- Yeni akış oluştur: "DemoUserFlow"
- 3 adım ekle:
  1. GET /api/users
  2. POST /api/users/login
  3. GET /api/users/profile
- Parametreleri ayarla

#### 3. Performans Testi (5 dk)
- Test başlat (10 thread, 5 sn ramp-up)
- Gerçek zamanlı sonuçları göster
- Geçmiş testlerle karşılaştır
- Grafikleri göster

#### 4. API Executor (3 dk)
- Manuel test çalıştır
- Request/Response göster
- Hata senaryosu göster

#### 5. AI Chat Bot (3 dk)
- Soru sor: "Bu sistemde performans testi nasıl yapılır?"
- HAR dosyası yükle ve analiz et
- Streaming yanıtı göster

#### 6. Özellikler (2 dk)
- Çoklu dil desteği
- Responsive tasarım
- Floating chat widget
- Tema özelleştirme

### Demo Öncesi Kontrol Listesi

- [ ] Backend servisleri çalışıyor
- [ ] Veritabanı bağlantısı aktif
- [ ] En az 1 proje tanımlı
- [ ] En az 2-3 test akışı hazır
- [ ] Performans test geçmişi var
- [ ] AI chat servisi çalışıyor
- [ ] Örnek HAR dosyası hazır
- [ ] Tarayıcı cache temizlendi
- [ ] Network bağlantısı stabil

### Demo İpuçları

1. **Hız:** İşlemleri hızlı göstermek için hazır veriler kullanın
2. **Görsellik:** Grafiklerin dolu olduğundan emin olun
3. **Hata Yönetimi:** Hata senaryolarını da gösterin
4. **Responsive:** Mobil görünümü de gösterin
5. **AI:** Chat bot'un akıllı yanıtlarını vurgulayın

---

## 🐛 Bilinen Sorunlar ve Çözümler

### Sorun: Backend'e bağlanamıyor
**Çözüm:** `.env.local` dosyasındaki API URL'leri kontrol edin

### Sorun: Performans testi başlamıyor
**Çözüm:** Ortam ve akış seçimini kontrol edin, backend loglarına bakın

### Sorun: Chat bot yanıt vermiyor
**Çözüm:** Chat API servisinin çalıştığından emin olun

### Sorun: Tema değişmiyor
**Çözüm:** Tarayıcı cache'ini temizleyin, sayfayı yenileyin

---

## 📞 Destek ve İletişim

- **Dokümantasyon:** `/dashboard/documents`
- **AI Yardım:** `/dashboard/chat`
- **GitHub Issues:** [Repository Issues]
- **Email:** support@apihub.com

---

## 📄 Lisans

Bu proje özel bir lisans altındadır. Detaylar için LICENSE dosyasına bakın.

---

## 🎉 Teşekkürler

APIHUB'ı kullandığınız için teşekkür ederiz! API test süreçlerinizi kolaylaştırmak için buradayız.

**Happy Testing! 🚀**
