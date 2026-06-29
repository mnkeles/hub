# Performance Test Ekranı - Codex Agent Geliştirme Talepleri

## Amaç

Mevcut performans test ekranı JMeter entegrasyonu ile test başlatma, test geçmişi görüntüleme ve detay metriklerini gösterme işlevlerini sağlamaktadır. Bu geliştirme ile ekranın sadece sonuç gösteren bir rapor ekranı olmaktan çıkarılıp, performans problemini otomatik yorumlayan, filtrelenebilir, karşılaştırılabilir ve daha okunabilir bir analiz ekranına dönüştürülmesi hedeflenmektedir.

Mevcut ekranda şu bilgiler zaten bulunmaktadır:

- Test başlatma alanı
  - Ortam
  - Akış
  - Thread Sayısı
  - Ramp Up
- Performans geçmişi tablosu
  - Tarih
  - Durum
  - Thread Sayısı
  - Ramp Up
  - Toplam Sample
  - Başarı Oranı
  - Hata Oranı
  - Throughput
  - Ortalama
  - P95
  - P99
  - En Yavaş Adım
- Test detay modalı
  - Adım Özeti
  - Thread Detayı
  - Sample Sayısı
  - Başarılı / Hatalı
  - Hata Oranı
  - Throughput
  - Ortalama
  - Min / Max
  - P90 / P95 / P99
  - Standart Sapma
  - Süre Dağılımı
  - Son Hata

Bu nedenle aşağıdaki geliştirmeler mevcut yapının üzerine eklenmelidir.

---

## 1. Test sonucuna PASSED / FAILED değerlendirmesi eklenmeli

### Problem

Şu anda test sonucu `COMPLETED` olarak görünüyor. Ancak `COMPLETED` sadece testin teknik olarak tamamlandığını ifade eder. Test performans açısından başarılı mı başarısız mı belli değildir.

Örneğin bir test hata almadan tamamlanmış olabilir fakat P95 değeri 12 saniye ise performans açısından başarısız sayılmalıdır.

### Beklenen geliştirme

Test tamamlandıktan sonra threshold değerlerine göre performans sonucu hesaplanmalıdır.

Önerilen statüler:

- `RUNNING`
- `COMPLETED - PASSED`
- `COMPLETED - FAILED`
- `STOPPED`
- `ERROR`

### Varsayılan threshold değerleri

İlk aşamada default değerler kullanılabilir:

```text
errorRate <= 1%
averageResponseTime <= 1000 ms
p95 <= 3000 ms
p99 <= 5000 ms
throughput >= 20 req/s
```

Bu değerler ileride ortam veya akış bazlı konfigüre edilebilir olmalıdır.

### Başarısızlık nedeni gösterimi

Eğer test `FAILED` olursa kullanıcıya neden başarısız olduğu gösterilmelidir.

Örnek:

```text
COMPLETED - FAILED

Reasons:
- P95 threshold exceeded. Expected <= 3000 ms, actual: 12762 ms
- P99 threshold exceeded. Expected <= 5000 ms, actual: 14964 ms
- Slowest step: odf-createCustomerWithNatId
```

### Kabul kriterleri

- Test tamamlandıktan sonra sadece `COMPLETED` yazmamalı.
- Threshold değerleri kontrol edilmeli.
- Test sonucu `PASSED` veya `FAILED` olarak işaretlenmeli.
- Başarısızlık varsa sebep listesi gösterilmeli.
- Performans geçmişi tablosunda durum alanı bu sonucu göstermeli.

---

## 2. Detay modalının üstüne otomatik analiz özeti eklenmeli

### Problem

Detay ekranında birçok metrik var fakat kullanıcı problemi kendisi yorumlamak zorunda kalıyor. Özellikle performans testini bilmeyen kullanıcılar için P90, P95, P99, standart sapma gibi değerleri yorumlamak zor olabilir.

### Beklenen geliştirme

Test detay modalının üst bölümüne `Performans Analizi` veya `Otomatik Yorum` adında bir özet kutusu eklenmelidir.

Bu kutuda sistem otomatik olarak şunları göstermelidir:

- Genel test sonucu: Passed / Failed
- En yavaş adım
- En yüksek P95 değerine sahip adım
- En yüksek P99 değerine sahip adım
- En çok hata alan adım
- En yüksek standart sapmaya sahip adım
- Kısa yorum

### Örnek görünüm

```text
Performans Analizi

Sonuç: COMPLETED - FAILED

En yavaş adım:
odf-createCustomerWithNatId

Ortalama süre: 4552 ms
P95: 12762 ms
P99: 14964 ms
Max: 18620 ms

Yorum:
Bu testte en problemli adım odf-createCustomerWithNatId olarak görünüyor.
Bu adımda isteklerin %95'i 12.7 saniyenin altında, %99'u ise 14.9 saniyenin altında tamamlanmış.
P95 ve P99 değerleri önerilen limitlerin üzerinde olduğu için bu adım optimizasyon için önceliklidir.
```

### Kabul kriterleri

- Detay modalında tablo başlamadan önce analiz kutusu görünmeli.
- En yavaş/problemli adım otomatik tespit edilmeli.
- P95/P99 threshold aşımı varsa yorumda belirtilmeli.
- Eğer hata varsa hata alan adımlar da özetlenmeli.
- Eğer test başarılıysa olumlu bir özet gösterilmeli.

---

## 3. Adım Özeti tablosunda görsel iyileştirme yapılmalı

### Problem

Adım Özeti tablosunda çok fazla kolon var. Bu teknik olarak doğru fakat okunabilirlik zorlaşıyor.

### Beklenen geliştirme

Kolonlar mantıksal gruplara ayrılmalıdır.

Önerilen kolon grupları:

```text
Test Sonucu:
- Sample Sayısı
- Başarılı
- Hatalı
- Hata Oranı

Performans:
- Throughput
- Ortalama
- Min
- Max

Percentile:
- P90
- P95
- P99

Analiz:
- Std. Sapma
- Süre Dağılımı
- Son Hata
```

Bu gruplama UI tarafında başlık, renk ayrımı veya sticky header ile yapılabilir.

### Ek iyileştirme

Problemli değerler görsel olarak vurgulanmalıdır.

Örnek kurallar:

- Hata oranı > 0 ise kırmızı göster.
- P95 threshold değerini geçerse kırmızı göster.
- P99 threshold değerini geçerse kırmızı göster.
- En yavaş adım satırını uyarı rengiyle işaretle.

### Kabul kriterleri

- Tablo daha okunabilir hale getirilmeli.
- Problemli metrikler görsel olarak ayırt edilmeli.
- En problemli satır kullanıcı tarafından hızlıca fark edilmeli.

---

## 4. Süre dağılımı metin yerine görsel olarak gösterilmeli

### Problem

Şu anda süre dağılımı şu şekilde metin olarak gösteriliyor:

```text
<500: 0, 500-1s: 0, 1-3s: 50, >3s: 50
```

Bu veri değerli fakat metin olarak okunması zor.

### Beklenen geliştirme

Süre dağılımı küçük bar/progress gösterimi ile görselleştirilmelidir.

Örnek:

```text
<500ms      0%
500ms-1s    0%
1s-3s       50%
>3s         50%
```

veya tablo içinde mini bar:

```text
>3s  ██████████ 50%
```

### Kabul kriterleri

- Süre dağılımı sadece düz metin olarak kalmamalı.
- Kullanıcı hangi aralıkta yoğunluk olduğunu hızlıca görebilmeli.
- Özellikle `>3s` oranı yüksekse görsel olarak dikkat çekmeli.

---

## 5. Thread Detayı alanına filtreleme eklenmeli

### Problem

Thread Detayı alanında çok sayıda satır olabilir. Test büyüdükçe bu alanı elle incelemek zorlaşır.

### Beklenen geliştirme

Thread Detayı tablosuna filtreleme özellikleri eklenmelidir.

Gerekli filtreler:

- Adıma göre filtrele
- Thread numarasına göre filtrele
- Duruma göre filtrele
- Sadece hatalı kayıtları göster
- Belirli süreden uzun sürenleri göster

Önerilen hızlı filtreler:

```text
Sadece Hatalılar
Süre > 1000 ms
Süre > 3000 ms
Süre > 5000 ms
```

### Kabul kriterleri

- Kullanıcı Thread Detayı içinde adım adına göre arama yapabilmeli.
- Kullanıcı sadece hatalı requestleri listeleyebilmeli.
- Kullanıcı yavaş requestleri filtreleyebilmeli.
- Filtreler aynı anda uygulanabilir olmalı.

---

## 6. Hata analizi alanı eklenmeli

### Problem

Şu anda `Son Hata` alanı var. Ancak performans testlerinde sadece son hatayı görmek yeterli değildir. Hata tipleri, hata sayıları ve hangi adımda oluştukları da görülmelidir.

### Beklenen geliştirme

Test detay modalına veya ayrı bir sekmeye `Hata Analizi` bölümü eklenmelidir.

Bu bölümde şu bilgiler gösterilmelidir:

- Toplam hata sayısı
- Hata oranı
- Hata tiplerine göre dağılım
- Hangi adımda kaç hata oluştuğu
- Son hata mesajı
- Hatalı request detayları

Örnek:

```text
Hata Analizi

Toplam Hata: 17
Hata Oranı: 2.4%

Hata Tipleri:
- HTTP 500: 10
- Timeout: 5
- Assertion Failed: 2

Adım Bazlı Hatalar:
- odf-auth: 3
- odf-createCustomerWithNatId: 14
```

### Kabul kriterleri

- Hata yoksa `Hata bulunamadı` gibi net bir mesaj gösterilmeli.
- Hata varsa hata tipleri gruplanmalı.
- Hatalar adım bazlı listelenmeli.
- Kullanıcı hatalı request detayına ulaşabilmeli.

---

## 7. Grafikler eklenmeli veya mevcut grafik ekranı güçlendirilmeli

### Problem

Tablolar detaylı bilgi verir fakat performans problemlerinin zaman içindeki davranışını anlamak için grafik gereklidir.

### Beklenen geliştirme

`Grafik Göster` alanı aşağıdaki grafikleri içermelidir:

1. Response Time Over Time
2. Throughput Over Time
3. Error Rate Over Time
4. Active Threads Over Time
5. Adım Bazlı Ortalama Response Time
6. Adım Bazlı P95 / P99 karşılaştırması

En öncelikli grafikler:

- Response Time Over Time
- Throughput Over Time
- Error Rate Over Time
- Adım Bazlı P95/P99

### Kabul kriterleri

- Grafikler test sonucu üzerinden üretilebilmeli.
- Endpoint/adım bazlı P95/P99 grafiği olmalı.
- Test boyunca response time artıyor mu görülebilmeli.
- Hataların testin hangi anında arttığı görülebilmeli.

---

## 8. Test karşılaştırma özelliği eklenmeli

### Problem

Tek bir test sonucuna bakmak yeterli değildir. Performansın önceki teste göre iyileşti mi kötüleşti mi anlaşılmalıdır.

### Beklenen geliştirme

Performans geçmişi ekranında kullanıcı iki testi seçip karşılaştırabilmelidir.

Karşılaştırılacak metrikler:

- Ortalama response time
- P90
- P95
- P99
- Throughput
- Hata oranı
- Toplam sample
- En yavaş adım

Örnek:

```text
Test Karşılaştırma

Metrik              Önceki Test     Son Test      Fark
Ortalama            406 ms          1852 ms       +1446 ms
P95                 2233 ms         6938 ms       +4705 ms
P99                 2275 ms         9749 ms       +7474 ms
Throughput          12.22 req/s     22.08 req/s   +9.86 req/s
Hata Oranı          0.00%           0.00%         0.00%
```

### Kabul kriterleri

- Kullanıcı geçmişten iki test seçebilmeli.
- Metrik farkları hesaplanmalı.
- Kötüleşen değerler vurgulanmalı.
- İyileşen değerler vurgulanmalı.

---

## 9. Test başlatma parametreleri genişletilmeli

### Problem

Şu anda test başlatma alanında temel parametreler var:

- Ortam
- Akış
- Thread Sayısı
- Ramp Up

Ancak toplam sample sayısının nasıl oluştuğu kullanıcı için net değil. Test süresi, loop count gibi bilgiler de kontrol edilebilir olmalıdır.

### Beklenen geliştirme

Test başlatma alanına aşağıdaki opsiyonel parametreler eklenmelidir:

- Duration: Test kaç saniye/dakika çalışacak
- Loop Count: Her thread kaç kez çalışacak
- Think Time: Kullanıcı aksiyonları arası bekleme süresi
- Timeout: Request timeout süresi
- Test Data seçimi: Kullanılacak veri seti
- Environment Base URL: Testin hangi URL'e gittiği

### Kabul kriterleri

- Duration veya Loop Count kullanıcı tarafından belirlenebilmeli.
- Toplam sample hesabı kullanıcıya daha net gösterilmeli.
- Yanlışlıkla çok yüksek yük verilmesini önlemek için validasyon olmalı.

### Validasyon önerileri

```text
Thread Sayısı > 500 ise kullanıcıdan onay iste.
Ramp Up = 0 ve Thread Sayısı yüksekse uyarı göster.
Duration boşsa default değer göster.
Loop Count boşsa default değer göster.
```

---

## 10. Testi durdurma özelliği eklenmeli

### Problem

Performans testi yanlış parametrelerle başlatılırsa ortamı gereksiz yere yorabilir. Bu nedenle çalışan testi durdurma imkanı olmalıdır.

### Beklenen geliştirme

Test çalışırken `Başlat` butonunun yanında veya yerine şu butonlar gösterilmelidir:

- Durdur
- Zorla Durdur

### Kabul kriterleri

- Çalışan test güvenli şekilde durdurulabilmeli.
- Durdurulan testin durumu `STOPPED` olmalı.
- Eğer zorla durdurma yapılırsa loglarda belirtilmeli.

---

## 11. Rapor indirme özelliği eklenmeli

### Problem

Test sonuçlarının ekip içinde paylaşılması veya saklanması gerekebilir.

### Beklenen geliştirme

Test sonucundan rapor indirilebilmelidir.

Desteklenmesi önerilen formatlar:

- CSV
- Excel
- JSON
- JMeter JTL
- HTML Report
- PDF Report, opsiyonel

### Kabul kriterleri

- Kullanıcı test sonucunu dışa aktarabilmeli.
- En az CSV veya Excel export desteklenmeli.
- JSON export teknik analiz için eklenmeli.
- Eğer JMeter HTML report üretiliyorsa indirilebilir olmalı.

---

## 12. Backend / servis tarafı için beklenen veri modeli

Aşağıdaki alanlar test sonucu modelinde bulunmalıdır veya hesaplanabilir olmalıdır.

### TestRun

```json
{
  "id": 204,
  "environment": "test",
  "flow": "createCustomer",
  "status": "COMPLETED_FAILED",
  "threadCount": 100,
  "rampUpSeconds": 0,
  "durationSeconds": 300,
  "loopCount": 1,
  "totalSamples": 1300,
  "successCount": 1300,
  "errorCount": 0,
  "successRate": 100.0,
  "errorRate": 0.0,
  "throughput": 22.08,
  "averageResponseTimeMs": 1852,
  "p90Ms": 0,
  "p95Ms": 6938,
  "p99Ms": 9749,
  "slowestStep": "odf-createCustomerWithNatId",
  "startedAt": "2026-06-25T14:00:00",
  "finishedAt": "2026-06-25T14:05:00",
  "thresholdResult": {
    "passed": false,
    "reasons": [
      "P95 threshold exceeded. Expected <= 3000 ms, actual: 6938 ms"
    ]
  }
}
```

### StepSummary

```json
{
  "stepName": "odf-createCustomerWithNatId",
  "sampleCount": 100,
  "successCount": 100,
  "errorCount": 0,
  "errorRate": 0.0,
  "throughput": 1.70,
  "averageMs": 4552,
  "minMs": 1002,
  "maxMs": 18620,
  "p90Ms": 11061,
  "p95Ms": 12762,
  "p99Ms": 14964,
  "stdDeviationMs": 4045,
  "durationDistribution": {
    "lt500ms": 0,
    "between500msAnd1s": 0,
    "between1sAnd3s": 50,
    "gt3s": 50
  },
  "lastError": null
}
```

### ThreadDetail

```json
{
  "threadNo": 0,
  "stepName": "odf-auth",
  "status": "COMPLETED",
  "durationMs": 1140,
  "error": null,
  "startedAt": "2026-06-25T14:00:01",
  "finishedAt": "2026-06-25T14:00:02"
}
```

---

## 13. Önceliklendirme

Geliştirmeler aşağıdaki sırayla yapılmalıdır.

### Yüksek öncelik

1. Threshold bazlı `PASSED / FAILED` sonucu
2. Otomatik performans analiz kutusu
3. Thread Detayı filtreleme
4. Problemli metriklerin görsel olarak vurgulanması
5. Hata analizi bölümü

### Orta öncelik

6. Adım bazlı P95/P99 grafiği
7. Test karşılaştırma özelliği
8. Süre dağılımının görselleştirilmesi
9. Test başlatma parametrelerinin genişletilmesi

### Düşük öncelik

10. PDF/HTML rapor indirme
11. Canlı test takip ekranı
12. Ortam sağlığı metrikleri

---



---

## 15. Canlı test takip ekranı eklenmeli

### Problem

Performans testi çalışırken kullanıcı çoğunlukla sadece testin bitmesini beklemektedir. Test sırasında sistemin nasıl davrandığı, hata oranının ne zaman yükseldiği veya response time değerlerinin hangi noktada bozulduğu anlık olarak görülememektedir.

Bu durum özellikle yanlış parametreyle başlatılan testlerde risklidir. Örneğin yüksek thread sayısı veya `Ramp Up = 0` ile başlatılan bir test ortamı gereksiz yere yorabilir. Kullanıcı test bitmeden problemi fark edebilmelidir.

### Beklenen geliştirme

Test çalışırken canlı metriklerin gösterildiği bir takip alanı eklenmelidir.

Bu alan test başlatıldıktan sonra otomatik görünür olabilir veya `Canlı Takip` / `Live Monitor` gibi ayrı bir butonla açılabilir.

Gösterilmesi gereken canlı bilgiler:

- Test durumu: `RUNNING`, `STOPPING`, `COMPLETED`, `ERROR`
- Aktif thread sayısı
- Toplam thread sayısı
- Tamamlanan sample sayısı
- Başarılı request sayısı
- Hatalı request sayısı
- Anlık hata oranı
- Anlık throughput
- Anlık ortalama response time
- Anlık P90 / P95, hesaplanabiliyorsa
- Çalışma süresi
- Tahmini kalan süre, hesaplanabiliyorsa
- En son tamamlanan adım
- Son hata, varsa

### Örnek görünüm

```text
Canlı Test Durumu

Durum: RUNNING
Aktif Thread: 87 / 100
Tamamlanan Sample: 750
Başarılı: 742
Hatalı: 8
Anlık Hata Oranı: 1.06%
Anlık Throughput: 18.4 req/s
Anlık Ortalama Süre: 1340 ms
Anlık P95: 4200 ms
Çalışma Süresi: 02:15
Tahmini Kalan Süre: 00:45
Son Adım: odf-createCustomerWithNatId
Son Hata: Timeout, varsa
```

### Uyarı mantığı

Canlı takip ekranında threshold aşımı olduğunda kullanıcıya uyarı gösterilmelidir.

Örnek kurallar:

```text
errorRate > 1% ise uyarı göster.
p95 > 3000 ms ise uyarı göster.
p99 > 5000 ms ise uyarı göster.
throughput beklenen değerin altına düşerse uyarı göster.
```

Örnek uyarı mesajı:

```text
Uyarı: Test devam ederken P95 değeri threshold değerini aştı.
Beklenen <= 3000 ms, mevcut: 4200 ms
```

### Durdurma aksiyonu ile bağlantı

Canlı takip ekranında `Durdur` ve gerekiyorsa `Zorla Durdur` butonları bulunmalıdır.

Bu sayede kullanıcı test devam ederken performansın bozulduğunu görürse testi bitmesini beklemeden durdurabilir.

### Teknik notlar

Canlı metrikler aşağıdaki yöntemlerden biriyle güncellenebilir:

- Polling: belirli aralıklarla backend endpoint çağrısı
- WebSocket / Server-Sent Events
- JMeter test sürecinden ara sonuçların backend'e aktarılması

İlk aşamada polling yeterlidir. Önerilen yenileme aralığı 2-5 saniye olabilir.

### Kabul kriterleri

- Test çalışırken canlı takip alanı görüntülenebilmeli.
- Canlı metrikler belirli aralıklarla güncellenmeli.
- Kullanıcı test bitmeden hata oranı, throughput ve response time değişimini görebilmeli.
- Threshold aşımı olursa kullanıcıya uyarı gösterilmeli.
- Canlı takip alanında testi durdurma aksiyonu bulunmalı.
- Canlı metrikler alınamazsa ekran hata vermemeli; kullanıcıya `Canlı metrik bilgisi alınamadı` gibi net bir mesaj gösterilmeli.

---

## 16. Ortam sağlığı metrikleri eklenmeli

### Problem

Performans testinde response time yüksek çıktığında problemin neden kaynaklandığı her zaman JMeter sonuçlarından anlaşılamaz.

Örneğin yavaşlık şu kaynaklardan biri nedeniyle oluşabilir:

- Uygulama sunucusunda CPU kullanımı yükselmiştir.
- RAM veya JVM heap kullanımı artmıştır.
- Garbage Collection süresi uzamıştır.
- Veritabanı bağlantı havuzu dolmuştur.
- DB query süreleri yükselmiştir.
- Pod/container restart olmuştur.
- Servis loglarında hata artışı vardır.

Bu nedenle sadece JMeter metriklerini göstermek yeterli değildir. Test sırasında sistem kaynaklarının da izlenmesi performans analizini daha anlamlı hale getirir.

### Beklenen geliştirme

Test sonucu detayına veya ayrı bir `Ortam Metrikleri` sekmesine ortam sağlığı metrikleri eklenmelidir.

Gösterilmesi önerilen metrikler:

- CPU kullanımı
- RAM kullanımı
- JVM Heap kullanımı
- Garbage Collection süresi
- DB connection pool kullanımı
- DB query latency
- HTTP 4xx / 5xx sayısı
- Pod/container restart sayısı
- Servis log hata sayısı
- Network latency, mevcutsa

Java / Spring Boot uygulamaları için özellikle aşağıdaki metrikler değerlidir:

- JVM Heap Usage
- JVM Non-Heap Usage
- GC Count
- GC Time
- HikariCP active connection count
- HikariCP idle connection count
- HikariCP pending connection count
- Slow SQL count
- HTTP 5xx count
- Request queue size, mevcutsa

### Örnek görünüm

```text
Ortam Sağlığı

Test Aralığı: 14:00:00 - 14:05:00

CPU Ortalama: 72%
CPU Max: 94%
RAM Ortalama: 68%
RAM Max: 81%
JVM Heap Max: 78%
GC Time: 2.4 sn
DB Active Connection Max: 48 / 50
Slow SQL Count: 12
HTTP 5xx Count: 0
Pod Restart: 0

Yorum:
Test sırasında DB connection pool kullanımı limite yaklaşmış.
Yüksek P95 değerleri DB bağlantı bekleme süresiyle ilişkili olabilir.
```

### Beklenen entegrasyon kaynakları

Bu metrikler mevcut altyapıya göre aşağıdaki kaynaklardan alınabilir:

- Prometheus
- Grafana
- Spring Boot Actuator
- Kubernetes Metrics API
- Application logs
- Database monitoring
- APM aracı, mevcutsa

İlk aşamada bu entegrasyonlardan biri yoksa geliştirme opsiyonel tasarlanmalıdır. Ortam metriği alınamadığında test detay ekranı bozulmamalıdır.

### Backend veri modeli önerisi

Test sonucu modeline opsiyonel olarak aşağıdaki gibi bir alan eklenebilir:

```json
{
  "environmentMetrics": {
    "cpuAvgPercent": 72,
    "cpuMaxPercent": 94,
    "memoryAvgPercent": 68,
    "memoryMaxPercent": 81,
    "jvmHeapMaxPercent": 78,
    "gcTimeMs": 2400,
    "dbActiveConnectionMax": 48,
    "dbConnectionPoolSize": 50,
    "slowSqlCount": 12,
    "http5xxCount": 0,
    "podRestartCount": 0,
    "metricsAvailable": true
  }
}
```

Metrik kaynağı yoksa:

```json
{
  "environmentMetrics": {
    "metricsAvailable": false,
    "message": "Ortam metrikleri için entegrasyon bulunamadı."
  }
}
```

### Kabul kriterleri

- Test sonucunda sadece JMeter metrikleri değil, ortam sağlığı metrikleri de gösterilebilmeli.
- Response time yükselişi ile CPU/RAM/DB bağlantısı arasında ilişki kurulabilmeli.
- Metrik kaynağı yoksa kullanıcıya `Ortam metriği bulunamadı` veya benzer net bir mesaj gösterilmeli.
- Bu alan opsiyonel olmalı; metrik entegrasyonu yoksa ekran hata vermemeli.
- Ortam metrikleri test başlangıç ve bitiş zamanı aralığına göre alınmalı.
- Kritik eşik aşımı varsa kullanıcıya uyarı gösterilmeli.

---

## 17. Nihai hedef

Bu geliştirmeler tamamlandığında kullanıcı sadece performans test sonucunu görmemeli, aynı zamanda şu soruların cevabını ekrandan doğrudan alabilmelidir:

- Test performans açısından başarılı mı?
- Başarısızsa neden başarısız?
- En problemli API/adım hangisi?
- Problem ortalama sürede mi, P95/P99 değerlerinde mi, hata oranında mı?
- Önceki teste göre performans iyileşti mi kötüleşti mi?
- Hatalar hangi adımlarda oluştu?
- Hangi requestler yavaş çalıştı?
- Test çalışırken anlık hata oranı, throughput ve response time nasıl değişti?
- Yavaşlık uygulama, veritabanı veya sunucu kaynaklarından hangisiyle ilişkili olabilir?

Bu ekranın amacı sadece JMeter çıktısını göstermek değil, JMeter çıktısını anlaşılır bir performans analizine dönüştürmek olmalıdır.
