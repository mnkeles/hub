# API Integration Guide

## 🚀 Yeni Eklenen Özellikler

### 1. Project Management
Proje yönetimi için tam CRUD desteği:

```typescript
// Tüm projeleri getir
const projects = await projectService.getAll();

// ID ile getir
const project = await projectService.getById(1);

// Short code ile getir
const project = await projectService.getByShortCode('PROJ_001');

// Kullanıcının projelerini getir
const myProjects = await projectService.getMyProjects();

// Kaydet/Güncelle/Sil
await projectService.save(projectData);
await projectService.update(projectData);
await projectService.delete(1);
```

### 2. Basit GET Endpoint'leri
Artık tüm kayıtları pagination olmadan çekebilirsiniz:

```typescript
// Projects
const projects = await projectService.getAll();

// General Web Systems
const systems = await generalWebSystemService.getAll();

// Process Flows
const flows = await processFlowService.getAll();

// Process Flow Steps
const steps = await processFlowStepService.getAll();
```

### 2. Optional Request Body
`list()` metodları artık parametre olmadan da çalışır:

```typescript
// Parametresiz kullanım (default: offset=0, limit=100)
const response = await generalWebSystemService.list();

// Parametreli kullanım
const response = await generalWebSystemService.list({
  offset: 0,
  limit: 10,
  filterList: []
});
```

### 3. API Executor İyileştirmeleri
- **Autocomplete Dropdown'lar**: Sistemler ve flow'lar artık dropdown'dan seçilebilir
- **Freesolo Mode**: İsterseniz manuel de girebilirsiniz
- **Otomatik Yükleme**: Sayfa açıldığında sistemler ve flow'lar otomatik yüklenir

## 📦 Servis Kullanımı

### Project Service

```typescript
import { projectService } from '@/services/projectService';

// Tüm projeleri getir
const allProjects = await projectService.getAll();

// ID ile getir
const project = await projectService.getById(1);

// Short code ile getir
const project = await projectService.getByShortCode('PROJ_001');

// Kullanıcı ID'sine göre getir
const userProjects = await projectService.getByUserId(123);

// Giriş yapan kullanıcının projelerini getir
const myProjects = await projectService.getMyProjects();

// Yeni proje kaydet
await projectService.save({
  projectId: null,
  name: 'New Project',
  description: 'Project description',
  shortCode: 'NEW_PROJ'
});

// Proje güncelle
await projectService.update({
  projectId: 1,
  name: 'Updated Project',
  description: 'Updated description',
  shortCode: 'UPD_PROJ'
});

// Proje sil
await projectService.delete(1);
```

### General Web System Service

```typescript
import { generalWebSystemService } from '@/services/generalWebSystemService';

// Tüm sistemleri getir
const allSystems = await generalWebSystemService.getAll();

// Sayfalama ile getir
const pagedSystems = await generalWebSystemService.list({
  offset: 0,
  limit: 10,
  filterList: []
});

// Sistem kaydet
await generalWebSystemService.save({
  gnlWebSysId: null,
  name: 'New System',
  shortCode: 'NEW_SYS',
  url: 'https://api.example.com',
  isActv: true,
  projectId: 1,
  isTokenUrl: false,
  baseUrlShortCode: 'BASE'
});
```

### Process Flow Service

```typescript
import { processFlowService } from '@/services/processFlowService';

// Tüm flow'ları getir
const allFlows = await processFlowService.getAll();

// Sayfalama ile getir
const pagedFlows = await processFlowService.list();

// ID ile getir
const flow = await processFlowService.getById(1);

// İlişkilerle birlikte getir
const flowWithSteps = await processFlowService.getWithRelations(1);

// Proje bazlı getir
const projectFlows = await processFlowService.getByProject(1);

// Kaydet/Güncelle/Sil
await processFlowService.save(flowData);
await processFlowService.update(flowData);
await processFlowService.delete(1);
```

### Process Flow Step Service

```typescript
import { processFlowStepService } from '@/services/processFlowStepService';

// Tüm adımları getir
const allSteps = await processFlowStepService.getAll();

// Filtreleme ile getir
const steps = await processFlowStepService.list({
  offset: 0,
  limit: 100,
  filterList: [
    {
      criteria: 'PROCESS_FLOW_ID',
      numberValue: 1
    }
  ]
});

// İlişkileri getir
const relations = await processFlowStepService.getRelations(stepId, projectId);

// Kaydet/Güncelle/Sil
await processFlowStepService.save(stepData);
await processFlowStepService.update(stepData);
await processFlowStepService.delete(stepId);
```

### API Call Service

```typescript
import { apiCallService } from '@/services/apiCallService';

// Tek adım çalıştır
const result = await apiCallService.callStep(
  'PROJECT1',
  'SYS_CODE',
  'LOGIN_STEP'
);

// Process flow çalıştır
const flowResult = await apiCallService.callProcess(
  'PROJECT1',
  'SYS_CODE',
  'USER_FLOW'
);

// Parametreli process flow çalıştır
const v2Result = await apiCallService.callProcessV2(
  'PROJECT1',
  'SYS_CODE',
  'USER_FLOW',
  {
    parameterContext: {
      userId: '123',
      userName: 'John'
    },
    globalHeaders: {
      'Authorization': 'Bearer token'
    },
    combination: false
  }
);
```

## 🎯 Kullanım Örnekleri

### Örnek 1: Sistemleri Dropdown'a Yükle

```typescript
const [systems, setSystems] = useState<GeneralWebSystemDto[]>([]);

useEffect(() => {
  const loadSystems = async () => {
    const data = await generalWebSystemService.getAll();
    setSystems(data);
  };
  loadSystems();
}, []);

// Autocomplete ile kullan
<Autocomplete
  options={systems}
  getOptionLabel={(option) => `${option.shortCode} - ${option.name}`}
  onChange={(_, newValue) => setSelectedSystem(newValue)}
  renderInput={(params) => <TextField {...params} label="System" />}
/>
```

### Örnek 2: Flow Adımlarını Listele

```typescript
const [steps, setSteps] = useState<ProcessFlowStepDto[]>([]);

const loadSteps = async (flowId: number) => {
  const response = await processFlowStepService.list({
    offset: 0,
    limit: 100,
    filterList: [
      {
        criteria: 'PROCESS_FLOW_ID',
        numberValue: flowId
      }
    ]
  });
  setSteps(response.data);
};
```

### Örnek 3: API Çağrısı Yap ve Sonucu Göster

```typescript
const [response, setResponse] = useState<any>(null);
const [loading, setLoading] = useState(false);
const [error, setError] = useState<string | null>(null);

const executeFlow = async () => {
  try {
    setLoading(true);
    setError(null);
    const result = await apiCallService.callProcess(
      project,
      systemCode,
      flowCode
    );
    setResponse(result);
  } catch (err: any) {
    setError(err.message);
  } finally {
    setLoading(false);
  }
};
```

## 🔧 Sorun Giderme

### Problem: "Cannot find module" hatası
**Çözüm**: Import path'lerini kontrol edin:
```typescript
import { generalWebSystemService } from '@/services/generalWebSystemService';
```

### Problem: API çağrısı 404 döndürüyor
**Çözüm**: Backend'in çalıştığından ve base URL'in doğru olduğundan emin olun:
```typescript
// services/api.ts
baseURL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:4053'
```

### Problem: CORS hatası
**Çözüm**: Backend CORS ayarlarını kontrol edin. Tüm origin'lere izin verilmeli.

## 📊 API Endpoint Özeti

| Kaynak | GET All | POST List | GET By ID | Save | Update | Delete |
|--------|---------|-----------|-----------|------|--------|--------|
| Project | ✅ `/all` | ❌ | ✅ `/{id}` | ✅ | ✅ | ✅ |
| General Web System | ✅ `/all` | ✅ `/list` | ❌ | ✅ | ❌ | ❌ |
| Process Flow | ✅ `/all` | ✅ `/list` | ✅ `/{id}` | ✅ | ✅ | ✅ |
| Process Flow Step | ✅ `/all` | ✅ `/list` | ❌ | ✅ | ✅ | ✅ |

## 🎨 UI Bileşenleri

### Yeni Eklenen Sayfalar
1. **Projects** (`/dashboard/projects`) - Proje yönetimi
2. **Web Systems** (`/dashboard/systems`) - Sistem yönetimi
3. **Process Flows** (`/dashboard/process-flows`) - Flow yönetimi
4. **Process Flow Steps** (`/dashboard/process-flows/[id]/steps`) - Adım yönetimi
5. **API Executor** (`/dashboard/api-executor`) - API test aracı

### Ortak Özellikler
- ✅ Pagination
- ✅ CRUD işlemleri
- ✅ Error handling
- ✅ Loading states
- ✅ Success messages
- ✅ Responsive design
- ✅ Material-UI components

## 🚀 Performans İpuçları

1. **İlk yükleme için** `/all` endpoint'lerini kullanın (max 1000 kayıt)
2. **Büyük veri setleri için** `/list` endpoint'i ile pagination kullanın
3. **Sık kullanılan verileri** client-side cache'leyin
4. **Filtreleme** sadece gerektiğinde kullanın

## 📝 Type Definitions

Tüm type tanımlamaları `types/api.ts` dosyasında bulunur:
- `ProjectDto`
- `GeneralWebSystemDto`
- `ProcessFlowDto`
- `ProcessFlowStepDto`
- `ProcessFlowStepParmDto`
- `ParameterRequestDto`
- `GeneralPageRequest`
- `PagedResponse<T>`
- `Result<T>`

## 🔗 Faydalı Linkler

- Backend API Docs: `http://localhost:4053/swagger-ui.html`
- Frontend: `http://localhost:3000`
- Dashboard: `http://localhost:3000/dashboard`

---

**Version**: 1.2.0  
**Last Updated**: November 14, 2024  
**New in 1.2.0**: Project Management API ve UI eklendi
