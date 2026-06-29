# Omni Web Services Automation

Omni Web Services Automation, API tabanlı süreçleri tanımlamak, yönetmek, çalıştırmak ve gerektiğinde AI destekli sohbet üzerinden bu süreçlere erişmek için geliştirilmiş bir Spring Boot uygulamasıdır.

Bu proje temel olarak üç ana ihtiyacı çözer:

- API sistemlerinin ve uç noktalarının merkezi yönetimi
- Süreç akışlarının adım bazlı modellenmesi ve çalıştırılması
- Chat/MCP entegrasyonu ile doğal dilden sistem aksiyonlarına erişim

## Genel Bakış

Proje Java 21, Spring Boot 3.5.x ve Spring AI 1.1.x üzerine kuruludur.

Ana bileşenler:

- REST API katmanı
- Servis katmanı
- Veritabanı ve Liquibase yönetimi
- Cache altyapısı
- Process Flow çalıştırma motoru
- AI chat ve MCP tool entegrasyonu

Uygulama varsayılan olarak `4053` portunda çalışır.

## Teknoloji Yığını

- Java 21
- Spring Boot 3.5.7
- Spring Web / Spring MVC
- Spring WebFlux
- Spring Security
- Spring Data JPA
- Liquibase
- PostgreSQL
- Ehcache / JCache
- Spring AI
- OpenAI Chat Model
- MCP Client WebFlux
- Maven

## Proje Yapısı

Ana paket yapısı özetle şu şekildedir:

- `src/main/java/etiya/omniAutomation/controller`
  - Dış dünyaya açılan REST endpoint'ler
- `src/main/java/etiya/omniAutomation/service`
  - İş kuralları ve uygulama davranışları
- `src/main/java/etiya/omniAutomation/config`
  - Security, CORS, MVC async, MCP ve genel konfigürasyonlar
- `src/main/java/etiya/omniAutomation/business/dto`
  - Request/response modelleri
- `src/main/resources`
  - `application.yml`, Liquibase ve diğer konfigürasyon kaynakları

## Projenin Çalışma Prensibi

### 1. Konfigürasyon ve Başlatma

Uygulama başlarken aşağıdaki bileşenler hazırlanır:

- Spring context oluşturulur
- Veritabanı bağlantısı açılır
- Liquibase migration'ları uygulanır
- Cache sağlayıcısı ayağa kalkar
- Security katmanı yüklenir
- Spring AI + OpenAI + MCP client bean'leri oluşturulur
- MVC async executor devreye alınır

### 2. Veri Yönetimi Akışı

Sistem, proje ve entegrasyon tanımlarını veritabanında tutar.

Temel domain mantığı:

- **Project**
  - İş alanı veya müşteri/proje grubu
- **General Web System**
  - Dış sistem veya entegrasyon hedefi
- **API Information**
  - Çağrılacak API tanımı
- **Process Flow**
  - Bir iş akışı
- **Process Flow Step**
  - Akış içindeki tekil adım
- **Process Flow Step Parameter**
  - Adımın ihtiyaç duyduğu parametre, expression, SQL veya code bilgileri

Bu yapı sayesinde bir entegrasyon sadece tek bir API çağrısı olarak değil, sıralı ve bağımlı adımlardan oluşan bir süreç olarak tanımlanabilir.

### 3. Process Flow Çalıştırma Mantığı

Uygulamanın en kritik davranışlarından biri process flow çalıştırmadır.

Genel akış şu şekildedir:

- İlgili proje ve sistem belirlenir
- İstenen process flow bulunur
- Flow içindeki step'ler sıralı şekilde alınır
- Her step için gerekli request verileri hazırlanır
- Header, parameter, body ve extractor kuralları uygulanır
- Önceki step çıktılarından yeni değerler türetilir
- Gerekirse context bazlı parametre üretimi yapılır
- API çağrıları gerçekleştirilir
- Sonuçlar adım bazlı olarak toplanır

Bu yaklaşım sayesinde sistem hem tek bir step çağrısı hem de uçtan uca süreç çağrısı yapabilir.

### 4. Chat + AI + MCP Çalışma Mantığı

Chat katmanı artık stream odaklı çalışır.

Bileşenler:

- `ChatController`
- `ChatHistoryService`
- `OpenAiChatService`
- Spring AI `ChatClient`
- MCP tool callback provider

Akış:

- Kullanıcı `POST /api/chat/stream` ile mesaj gönderir
- Kullanıcı mesajı chat history içine eklenir
- History, Spring AI `Prompt` nesnesine çevrilir
- Model isteği streaming olarak başlatılır
- Model gerekiyorsa MCP tool callback'lerini çağırır
- Gelen içerik parçaları SSE olarak istemciye aktarılır
- Varsa token usage bilgisi ayrıca event olarak iletilir
- Sağlayıcı metadata verirse reasoning bilgisi de event olarak dönebilir
- Stream tamamlanınca assistant cevabı history'ye kaydedilir

### 5. Streaming Event Yapısı

Chat stream response tek bir düz string değil, yapılandırılmış event nesneleri olarak akar.

Örnek event tipleri:

- `content`
- `usage`
- `reasoning`
- `done`
- `error`

Bu yaklaşım frontend tarafında token kullanımı, reasoning bilgisi ve tamamlanma durumunun ayrı ayrı işlenmesini sağlar.

## Başlıca Endpoint Grupları

Projede öne çıkan controller grupları:

- `ProjectController`
- `GeneralWebSystemController`
- `ProcessFlowController`
- `ProcessFlowStepController`
- `ProcessFlowStepParmController`
- `ApiInformationController`
- `ApiCallController`
- `ChatController`
- `DatabaseConfigController`
- `CacheController`
- `AuthController`
- `UserController`

Detaylı endpoint listesi için mevcut `API_DOCUMENTATION.md` dosyasına bakılmalıdır.

## Kurulum

### Gereksinimler

- Java 21
- Maven 3.9+
- PostgreSQL
- OpenAI API erişimi
- MCP server erişimi

### Konfigürasyon

Temel ayarlar `src/main/resources/application.yml` içinde yer alır.

Önemli alanlar:

- `spring.datasource.*`
- `spring.ai.openai.*`
- `spring.ai.mcp.client.*`
- `jwt.auth.*`
- `server.port`

### Güvenlik Notu

Bu projede hassas bilgiler konfigürasyon içine yazılmamalıdır.

Özellikle aşağıdaki veriler environment variable veya secret manager üzerinden verilmelidir:

- OpenAI API key
- Database password
- JWT secret

Önerilen kullanım:

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
  datasource:
    password: ${DB_PASSWORD}
jwt:
  auth:
    secret: ${JWT_SECRET}
```

## Uygulamayı Çalıştırma

### Maven ile

```bash
mvn clean spring-boot:run
```

### Jar ile

```bash
mvn clean package
java -jar target/omniAutomation.jar
```

### Docker

Projede `Dockerfile` bulunduğu için container bazlı çalıştırma da mümkündür. Gerekiyorsa image oluşturulup container içinde ayağa kaldırılabilir.

## Asenkron Çalışma

Streaming endpoint'lerde Spring MVC varsayılan executor yerine özel async executor kullanılır.

Bu sayede:

- SSE altında daha kontrollü thread kullanımı sağlanır
- `SimpleAsyncTaskExecutor` kaynaklı üretim riskleri azaltılır
- Uzun süren stream işlemleri için daha uygun bir yürütme modeli sunulur

## Cache ve Performans

Sistem belirli veri tiplerinde cache desteği kullanır.

Performans açısından dikkat edilmesi gerekenler:

- Büyük veri setlerinde `/list` endpoint'leri tercih edilmelidir
- `/all` endpoint'leri kontrollü kullanılmalıdır
- Chat stream altında gereksiz uzun history birikimi önlenmelidir
- Tool çağrıları gecikme yaratabileceği için MCP server erişimi stabil olmalıdır

## MCP Tool Bu Projeye Nasıl Uygun Olmalı?

Bu projeye eklenecek MCP tool, sistemin veri modeli ve process flow mantığı ile uyumlu olmalıdır.

### Temel Tasarım Prensipleri

- Tool, sahte veri üretmemelidir
- Tool, mevcut sistem short code / process flow / step mantığını bozmayacak şekilde çalışmalıdır
- Tool çıktısı AI tarafından yorumlanabilir ama backend mantığıyla tutarlı olmalıdır
- Tool mümkünse idempotent veya açık etkili olmalıdır
- Tool response'ları küçük, yapısal ve parse edilebilir olmalıdır

### Uygun MCP Tool Kategorileri

Bu proje için en uygun tool türleri şunlardır:

- **Sorgulama tool'ları**
  - proje listeleme
  - sistem listeleme
  - process flow bulma
  - step detaylarını getirme
- **Doğrulama tool'ları**
  - bir flow'un mevcut olup olmadığını kontrol etme
  - bir sistem short code'unun geçerli olup olmadığını doğrulama
  - step bağımlılıklarını analiz etme
- **Çalıştırma tool'ları**
  - belirli bir step'i çalıştırma
  - belirli bir process flow'u çalıştırma
  - parameter context ile test amaçlı akış çalıştırma
- **Analiz tool'ları**
  - flow bağımlılık özeti çıkarma
  - step parametre eksiklerini raporlama
  - hangi sistemlerin hangi projeye bağlı olduğunu özetleme

### Önerilen MCP Tool Davranışı

Bir MCP tool aşağıdaki özellikleri taşımalıdır:

- Girdi açık ve dar kapsamlı olmalı
- Gerekli alanlar net tanımlanmalı
- Dönüş tipi JSON benzeri stabil bir yapı olmalı
- Hata mesajları kullanıcı dostu ama teknik olarak anlamlı olmalı
- Çok büyük payload yerine özet bilgi dönmeli

### Bu Projeye Uygun Örnek Tool Fikirleri

#### 1. `get_process_flow_summary`

Amaç:

- Bir process flow'un üst düzey özetini döndürmek

Girdi:

- `projectShortCode`
- `systemShortCode`
- `processFlowShortCode`

Örnek çıktı:

```json
{
  "processFlowShortCode": "USER_CREATE_FLOW",
  "stepCount": 5,
  "steps": ["LOGIN", "CREATE_USER", "ASSIGN_ROLE"],
  "active": true
}
```

#### 2. `get_step_requirements`

Amaç:

- Bir step'in ihtiyaç duyduğu parametreleri, header bilgilerini ve extractor yapısını döndürmek

Girdi:

- `projectShortCode`
- `systemShortCode`
- `stepShortCode`

Örnek çıktı:

```json
{
  "stepShortCode": "LOGIN",
  "headers": ["Content-Type"],
  "parameters": ["username", "password"],
  "extractors": ["token"],
  "hasSql": false,
  "hasCode": false
}
```

#### 3. `run_process_flow_preview`

Amaç:

- Gerçek çalıştırma öncesi flow validasyon/ön izleme yapmak

Girdi:

- `projectShortCode`
- `systemShortCode`
- `processFlowShortCode`
- `parameterContext`

Örnek çıktı:

```json
{
  "valid": true,
  "resolvedSteps": 4,
  "missingParameters": [],
  "warnings": []
}
```

## MCP Tool Geliştirirken Dikkat Edilecek Kurallar

- Tool isimleri kısa ve anlamlı olmalı
- Domain dili kullanılmalı
  - `project`, `system`, `processFlow`, `step`, `parameterContext`
- Tool response içinde mümkünse şu alanlar standart tutulmalı:
  - `success`
  - `message`
  - `data`
- Yan etki oluşturan tool'lar ayrıca açıkça belirtilmeli
- Delete/update yapan tool'larda güvenlik ve yetki kontrolü zorunlu olmalı
- Chat tarafından çağrılan tool'lar zaman aşımı ve kesilme durumlarına dayanıklı olmalı
- Çok uzun süren işlemler için özet veya job tabanlı yaklaşım düşünülmeli

## MCP Tool İçin Önerilen Entegrasyon Yaklaşımı

Bu projede tool'ların aşağıdaki mantıkla ilerlemesi uygundur:

- Önce sorgulama/özet tool'ları eklenmeli
- Sonra validasyon tool'ları eklenmeli
- En son çalıştırma yapan tool'lar devreye alınmalı

Böylece AI önce sistemi anlayabilir, sonra doğrulama yapabilir, en son aksiyon alabilir.

Önerilen sıra:

- `get_project_list`
- `get_systems_by_project`
- `get_process_flows_by_system`
- `get_process_flow_summary`
- `get_step_requirements`
- `validate_process_flow_inputs`
- `run_process_flow_preview`
- `run_process_flow`

## Chat Katmanı ile MCP Tool İlişkisi

Bu projede AI chat katmanı tool kullanımında şu ilkeye göre davranmalıdır:

- Bilgi gerekiyorsa tool çağrılmalı
- Emin olunmayan durumda tahmin üretilmemeli
- Kullanıcı bir flow, sistem veya step hakkında soru soruyorsa önce sistem verisi okunmalı
- Çalıştırma aksiyonu gerekiyorsa kullanıcı niyeti yeterince net olmalı

Bu davranış, `OpenAiChatService` içindeki sistem prompt yaklaşımıyla uyumludur.

## README Kullanım Amacı

Bu README özellikle şu kişiler için hazırlanmıştır:

- projeye yeni başlayan backend geliştirici
- frontend geliştirici
- MCP tool geliştirecek ekip üyesi
- AI/chat entegrasyonu yapacak geliştirici
- operasyon ve entegrasyon tarafında sistemi anlamak isteyen ekip üyeleri

## İlgili Dokümanlar

- `API_DOCUMENTATION.md`
- `API_INFORMATION_FRONTEND_GUIDE.md`
- `DATABASE_CONFIG_FRONTEND_GUIDE.md`
- `BACKEND_DELETE_FIX_REQUIREMENTS.md`
- `BACKEND_DEVELOPER_MESAJI.md`

## Son Notlar

Bu proje klasik CRUD uygulamasından daha fazlasıdır. Esas gücü, entegrasyon tanımlarını veriye dönüştürmesi ve bunları tekrar kullanılabilir süreç akışları halinde çalıştırabilmesidir.

AI ve MCP entegrasyonu eklenirken temel hedef şudur:

- Sistemi doğal dilden erişilebilir hale getirmek
- Ancak domain kurallarını ve gerçek veri kaynağını korumak

Bu nedenle eklenecek her yeni MCP tool, proje domain modeline ve process flow çalışma mantığına uyumlu tasarlanmalıdır.
