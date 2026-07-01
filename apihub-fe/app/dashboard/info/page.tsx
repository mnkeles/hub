'use client';

import React from 'react';
import { useLocale } from 'next-intl';
import {
    Box,
    Chip,
    Paper,
    Typography,
} from '@mui/material';
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome';
import DescriptionIcon from '@mui/icons-material/Description';
import AccountTreeIcon from '@mui/icons-material/AccountTree';
import SpeedIcon from '@mui/icons-material/Speed';
import ApiIcon from '@mui/icons-material/Api';
import StorageIcon from '@mui/icons-material/Storage';
import AssessmentIcon from '@mui/icons-material/Assessment';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import SmartToyIcon from '@mui/icons-material/SmartToy';
import DashboardLayout from '@/components/DashboardLayout';

export default function DashboardInfoPage() {
    const locale = useLocale();
    const isTurkish = locale === 'tr';

    const content = isTurkish ? {
        pageTitle: 'APIHUB Bilgi Ekranı',
        heroChip: 'Platform Dokümantasyonu',
        heroOverline: 'APIHUB Platformu',
        heroTitle: 'API yaşam döngüsünü daha görünür, ölçülebilir ve yönetilebilir hale getirin',
        heroDescription: 'APIHUB; API bilgileri, akış yönetimi, ortam bağlantıları, performans testleri, dokümantasyon ve yapay zeka destekli analiz araçlarını tek bir çalışma alanında bir araya getirir.',
        heroTags: ['API Yönetimi', 'Performans İzleme', 'Akış Otomasyonu', 'AI Destekli Analiz'],
        quickGuideTitle: 'Kullanım Rehberi',
        quickGuideDescription: 'Platformu verimli kullanmak için aşağıdaki sıralamayı takip edebilirsiniz. Bu yapı, ekip içi standardizasyon ve daha okunabilir dokümantasyon sağlar.',
        principlesTitle: 'Dokümantasyon İlkeleri',
        summaryTitle: 'Platform Özeti',
        whatYouCanDoTitle: 'Neler Yapabilirsiniz?',
        whatYouCanDoDescription: 'API endpoint detaylarını inceleyin, süreç akışları tanımlayın, ortam ve veri bağlantılarını yönetin, yük ve performans testlerini çalıştırın, sonuçları değerlendirin ve bilgi tabanınızı güncel tutun.',
        whatIsThisScreenTitle: 'Bu Ekran Ne İçin?',
        whatIsThisScreenDescription: 'Bu sayfa, platformu ilk kez kullanan ya da modüller arası ilişkiyi hızlıca anlamak isteyen kullanıcılar için hazırlanmış görsel dokümantasyon ekranıdır.',
        flowsDetailTitle: 'Akışları Nasıl Detaylandırıyoruz?',
        speedTitle: 'Hız Avantajı',
        speedDescription: 'API Executor üzerinden yapılan çağrılar sayesinde ekipler, yanıt süresi, cevap boyutu ve HTTP durumunu tek bakışta görebilir. Bu yapı özellikle geliştirme ve test döngülerinde bekleme süresini azaltır.',
        speedExampleTitle: 'Örnek Hız Göstergesi',
        speedExampleDescription: 'Bu görünüm; isteğin başarılı dönüp dönmediğini, ne kadar sürede cevap verdiğini ve response boyutunu birlikte değerlendirmenize yardım eder.',
        dataTitle: 'Sürekli Veri Üretme Avantajı',
        dataDescription: 'Özellikle createCustomer benzeri endpoint’lerde aynı akışı farklı parametrelerle tekrar çalıştırmak, test ortamında sürdürülebilir veri üretimi sağlar. Böylece hem entegrasyon testleri hem de performans senaryoları sürekli beslenebilir.',
        responseExampleTitle: 'Örnek Response Yapısı',
        benefitsTitle: 'Ne Kazandırır?',
        allureTitle: 'Allure Report Dokümantasyonu',
        allureDescription: 'Allure Report bölümü; test koşularını, başarı oranını, defect kategorilerini, servis bazlı kırılımları ve geçmiş trendleri görsel olarak takip etmenizi sağlar. Bu alan hem operasyonel izleme hem de teknik dokümantasyon amacıyla kullanılabilir.',
        allureVisualTitle: 'Görsel Rapor Özeti',
        successRateTitle: 'Başarı Oranı',
        successRateDescription: 'Geçen testler, toplam koşular içindeki başarı dağılımını verir. Kırmızı alanlar hata ve broken sonuçları temsil eder.',
        suitesTitle: 'Suites Görünümü',
        categoriesTrendTitle: 'Categories ve Trend Alanı',
        meaningsTitle: 'Ne Anlama Geliyor?',
        allureFlowTitle: 'Allure Akışı Nasıl Çalışır?',
        allureSetupTitle: 'Allure Nasıl Eklenir?',
        testCases: 'test cases',
        apiService: 'API Service',
        passed: 'Passed',
        broken: 'Broken',
        failed: 'Failed',
        stepsLabel: 'Adım',
        modules: [
            { title: 'API Bilgileri', description: 'Endpoint detaylarını, teknik açıklamaları ve servis bilgisini merkezi olarak görüntüleyin.' },
            { title: 'Akışlar', description: 'İş akışlarını modelleyin, test senaryolarını düzenleyin ve otomasyon süreçlerini yönetin.' },
            { title: 'Performans', description: 'Yük testleri, geçmiş sonuçlar ve kritik performans metriklerini tek ekranda izleyin.' },
            { title: 'Dokümantasyon', description: 'Bilgi tabanınızı oluşturun, sürdürün ve ekip içinde ortak referans noktası sağlayın.' },
        ],
        quickStartSteps: [
            'İlgili projeyi üst bardan seçin ve çalışma bağlamınızı netleştirin.',
            'Soldaki menüden API, akış veya performans modülüne geçin.',
            'Dokümantasyon alanında süreçlerinizi ve teknik notlarınızı kayıt altına alın.',
            'AI destekli analiz ile test çıktılarınızı yorumlayıp aksiyon planı oluşturun.',
        ],
        principles: [
            { title: 'Açık ve güncel içerik', description: 'Her doküman, güncel iş akışını ve gerçek sistem davranışını yansıtmalıdır.' },
            { title: 'Bağlam odaklı yapı', description: 'Ortam, veri kaynağı ve akış bağımlılıkları birlikte ele alınmalıdır.' },
            { title: 'Aksiyon üretilebilir içerik', description: 'Dokümantasyon sadece bilgi vermemeli; sonraki adımı da net göstermelidir.' },
            { title: 'AI ile desteklenebilirlik', description: 'Yapılandırılmış ve net içerikler, AI destekli analizlerin doğruluğunu artırır.' },
        ],
        flowDetailCards: [
            { title: 'İş Akışı Katmanı', description: 'Senaryonun hangi servisleri, hangi sırayla ve hangi veri bağımlılıklarıyla çalıştırdığını anlatır.' },
            { title: 'Test Sonucu Katmanı', description: 'Passed, failed ve broken dağılımları ile problemli adımların hangi aşamada yoğunlaştığını gösterir.' },
            { title: 'İyileştirme Katmanı', description: 'Elde edilen rapor üzerinden kök neden analizi, yeniden çalışma planı ve kalite hedefleri oluşturulur.' },
        ],
        speedHighlights: [
            'Tek ekranda request gönderimi ile yanıt süresini anında gözlemleme',
            'Header, body ve test sonucu alanlarını aynı akışta izleme',
            'Hızlı tekrar eden çağrılar ile regresyon farklarını erken tespit etme',
        ],
        dataGenerationHighlights: [
            'Sürekli yeni müşteri, işlem veya test verisi üretme',
            'Aynı akışın farklı input setleriyle tekrar çalıştırılması',
            'Test ortamlarında canlıya benzer veri akışı oluşturarak doğrulama yapılması',
        ],
        benefitCards: [
            { title: 'Daha hızlı geri bildirim', description: 'Geliştirici ve test ekipleri response süresi ile içerik kalitesini aynı anda kontrol ederek daha hızlı karar verir.' },
            { title: 'Sürekli test beslemesi', description: 'Otomatik veya yarı otomatik çağrılarla test ortamı sürekli yeni veri ile beslenebilir.' },
            { title: 'Daha güçlü raporlama', description: 'Sürekli oluşan veri; Allure, performans ve servis bazlı kalite raporlarının daha anlamlı hale gelmesini sağlar.' },
        ],
        allureMeaningItems: [
            { title: 'Success Rate', description: 'Toplam testlerin ne kadarının başarılı geçtiğini gösterir. Hızlı sağlık göstergesi olarak yorumlanır.' },
            { title: 'Suites', description: 'Testlerin servis, modül veya senaryo bazında nasıl dağıldığını ve hangi gruplarda hata yoğunlaştığını gösterir.' },
            { title: 'Categories', description: 'Defect sınıflandırmasını sunar. Örneğin product defect, test defect veya broken senaryoları ayırmak için kullanılır.' },
            { title: 'Trend Grafiği', description: 'Zaman içerisindeki kalite değişimini gösterir. Kırmızı alanların yükselmesi regresyon veya stabilite problemi işaret edebilir.' },
        ],
        allureSteps: [
            'Test senaryoları çalıştırılır ve sonuçlar Allure uyumlu çıktı üretir.',
            'Rapor verileri proje bazlı olarak toplanır ve APIHUB tarafından okunur.',
            'Dashboard ve analiz ekranları özet metrikleri, hata kategorilerini ve servis bazlı sonuçları gösterir.',
            'Ekip, başarısız testleri ve defect dağılımını izleyerek iyileştirme aksiyonlarını belirler.',
        ],
        allureSetupSteps: [
            { title: '1. Test çıktısı üretin', description: 'Test framework’ünüzde Allure result dosyalarını üretin ve her koşu sonunda ilgili klasöre yazdırın.' },
            { title: '2. Proje bazlı rapor dizini tanımlayın', description: 'Her proje için ayrı rapor kaynağı kullanın. Böylece dashboard içinde proje seçimine göre doğru sonuçlar gösterilir.' },
            { title: '3. APIHUB proxy veya servis katmanına bağlayın', description: 'Allure rapor verileri JSON endpoint’leri üzerinden okunur ve arayüzde özet kartlar ile servis edilir.' },
            { title: '4. Görselleştirme ve aksiyon', description: 'Başarısız servisler, kategori kırılımları ve trend alanları üzerinden iyileştirme maddeleri çıkarın.' },
        ],
    } : {
        pageTitle: 'APIHUB Information Screen',
        heroChip: 'Platform Documentation',
        heroOverline: 'APIHUB Platform',
        heroTitle: 'Make the API lifecycle more visible, measurable and manageable',
        heroDescription: 'APIHUB brings API information, flow management, environment connections, performance testing, documentation and AI-assisted analysis tools together in a single workspace.',
        heroTags: ['API Management', 'Performance Monitoring', 'Flow Automation', 'AI Assisted Analysis'],
        quickGuideTitle: 'Usage Guide',
        quickGuideDescription: 'You can follow the sequence below to use the platform efficiently. This structure provides team standardization and more readable documentation.',
        principlesTitle: 'Documentation Principles',
        summaryTitle: 'Platform Summary',
        whatYouCanDoTitle: 'What Can You Do?',
        whatYouCanDoDescription: 'Review API endpoint details, define process flows, manage environments and data connections, run load and performance tests, evaluate results, and keep your knowledge base up to date.',
        whatIsThisScreenTitle: 'What Is This Screen For?',
        whatIsThisScreenDescription: 'This page is a visual documentation screen prepared for users who are new to the platform or want to quickly understand the relationship between modules.',
        flowsDetailTitle: 'How Do We Detail Flows?',
        speedTitle: 'Speed Advantage',
        speedDescription: 'With calls made through API Executor, teams can see response time, payload size and HTTP status at a glance. This structure reduces waiting time especially in development and test cycles.',
        speedExampleTitle: 'Sample Speed Indicator',
        speedExampleDescription: 'This view helps you evaluate together whether the request succeeded, how long it took to respond and the response size.',
        dataTitle: 'Continuous Data Generation Advantage',
        dataDescription: 'Especially for endpoints such as createCustomer, running the same flow repeatedly with different parameters provides sustainable data generation in test environments. This continuously feeds both integration tests and performance scenarios.',
        responseExampleTitle: 'Sample Response Structure',
        benefitsTitle: 'What Does It Provide?',
        allureTitle: 'Allure Report Documentation',
        allureDescription: 'The Allure Report section lets you visually track test runs, success rate, defect categories, service-based breakdowns and historical trends. This area can be used for both operational monitoring and technical documentation.',
        allureVisualTitle: 'Visual Report Summary',
        successRateTitle: 'Success Rate',
        successRateDescription: 'Shows the success distribution of passed tests within total runs. Red areas represent failed and broken results.',
        suitesTitle: 'Suites View',
        categoriesTrendTitle: 'Categories and Trend Area',
        meaningsTitle: 'What Does It Mean?',
        allureFlowTitle: 'How Does the Allure Flow Work?',
        allureSetupTitle: 'How Is Allure Added?',
        testCases: 'test cases',
        apiService: 'API Service',
        passed: 'Passed',
        broken: 'Broken',
        failed: 'Failed',
        stepsLabel: 'Step',
        modules: [
            { title: 'API Information', description: 'View endpoint details, technical explanations and service information from a central location.' },
            { title: 'Flows', description: 'Model business flows, organize test scenarios and manage automation processes.' },
            { title: 'Performance', description: 'Track load tests, historical results and critical performance metrics on one screen.' },
            { title: 'Documentation', description: 'Build and maintain your knowledge base and provide a shared reference point across the team.' },
        ],
        quickStartSteps: [
            'Select the relevant project from the top bar and clarify your working context.',
            'Move to the API, flow or performance module from the left menu.',
            'Record your processes and technical notes in the documentation area.',
            'Interpret test outputs and build an action plan with AI-assisted analysis.',
        ],
        principles: [
            { title: 'Clear and up-to-date content', description: 'Each document should reflect the current workflow and actual system behavior.' },
            { title: 'Context-oriented structure', description: 'Environment, data source and flow dependencies should be handled together.' },
            { title: 'Actionable content', description: 'Documentation should not only inform; it should also clearly show the next step.' },
            { title: 'AI-friendly structure', description: 'Structured and clear content increases the accuracy of AI-assisted analysis.' },
        ],
        flowDetailCards: [
            { title: 'Workflow Layer', description: 'Explains which services the scenario runs, in what order and with which data dependencies.' },
            { title: 'Test Result Layer', description: 'Shows at which stage problematic steps are concentrated with passed, failed and broken distributions.' },
            { title: 'Improvement Layer', description: 'Builds root cause analysis, rework planning and quality targets based on the report.' },
        ],
        speedHighlights: [
            'Observe response time instantly by sending requests from a single screen',
            'Monitor headers, body and test result areas in the same flow',
            'Detect regression differences early with rapid repeated calls',
        ],
        dataGenerationHighlights: [
            'Continuously generate new customer, transaction or test data',
            'Run the same flow repeatedly with different input sets',
            'Create production-like data streams in test environments for better validation',
        ],
        benefitCards: [
            { title: 'Faster feedback', description: 'Development and test teams make faster decisions by checking response speed and content quality together.' },
            { title: 'Continuous test feeding', description: 'Test environments can be continuously fed with new data through automated or semi-automated calls.' },
            { title: 'Stronger reporting', description: 'Continuously generated data makes Allure, performance and service-based quality reports more meaningful.' },
        ],
        allureMeaningItems: [
            { title: 'Success Rate', description: 'Shows how many of the total tests passed. It is interpreted as a quick health indicator.' },
            { title: 'Suites', description: 'Shows how tests are distributed by service, module or scenario and where failures are concentrated.' },
            { title: 'Categories', description: 'Presents defect classification. For example, it is used to separate product defect, test defect or broken scenarios.' },
            { title: 'Trend Chart', description: 'Shows quality change over time. Rising red areas may indicate regression or stability problems.' },
        ],
        allureSteps: [
            'Test scenarios are executed and generate Allure-compatible output.',
            'Report data is collected on a per-project basis and read by APIHUB.',
            'Dashboard and analysis screens display summary metrics, error categories and service-based results.',
            'The team identifies improvement actions by tracking failed tests and defect distribution.',
        ],
        allureSetupSteps: [
            { title: '1. Generate test output', description: 'Produce Allure result files in your test framework and write them to the relevant folder after each run.' },
            { title: '2. Define a project-based report directory', description: 'Use a separate report source for each project. This ensures the correct results are displayed according to project selection.' },
            { title: '3. Connect it to the APIHUB proxy or service layer', description: 'Allure report data is read through JSON endpoints and served in the UI with summary cards.' },
            { title: '4. Visualize and act', description: 'Derive improvement items from failed services, category breakdowns and trend areas.' },
        ],
    };

    const moduleCards = [
        {
            title: content.modules[0].title,
            description: content.modules[0].description,
            icon: <ApiIcon sx={{ fontSize: 26 }} />,
        },
        {
            title: content.modules[1].title,
            description: content.modules[1].description,
            icon: <AccountTreeIcon sx={{ fontSize: 26 }} />,
        },
        {
            title: content.modules[2].title,
            description: content.modules[2].description,
            icon: <SpeedIcon sx={{ fontSize: 26 }} />,
        },
        {
            title: content.modules[3].title,
            description: content.modules[3].description,
            icon: <DescriptionIcon sx={{ fontSize: 26 }} />,
        },
    ];

    const quickStartSteps = content.quickStartSteps;
    const allureSteps = content.allureSteps;
    const speedHighlights = content.speedHighlights;
    const dataGenerationHighlights = content.dataGenerationHighlights;

    return (
        <DashboardLayout>
            <Box>
                <Typography variant="h4" sx={{ mb: 3, fontWeight: 700 }}>
                    {content.pageTitle}
                </Typography>

                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                    <Paper
                        sx={{
                            p: { xs: 3, md: 5 },
                            borderRadius: 4,
                            background: 'linear-gradient(135deg, rgba(102, 126, 234, 0.18) 0%, rgba(118, 75, 162, 0.18) 100%)',
                            border: '1px solid',
                            borderColor: 'divider',
                            position: 'relative',
                            overflow: 'hidden',
                        }}
                    >
                        <Box sx={{ position: 'absolute', top: -40, right: -40, width: 180, height: 180, borderRadius: '50%', backgroundColor: 'rgba(255,255,255,0.14)' }} />
                        <Box sx={{ position: 'relative', zIndex: 1 }}>
                            <Chip
                                icon={<AutoAwesomeIcon />}
                                label={content.heroChip}
                                sx={{ mb: 2, fontWeight: 700, backgroundColor: 'rgba(255,255,255,0.5)' }}
                            />
                            <Typography variant="overline" sx={{ color: 'primary.main', fontWeight: 700, letterSpacing: 1.2, display: 'block' }}>
                                {content.heroOverline}
                            </Typography>
                            <Typography variant="h3" sx={{ mt: 1, mb: 2, fontWeight: 800, fontSize: { xs: '2rem', md: '3rem' }, maxWidth: 800 }}>
                                {content.heroTitle}
                            </Typography>
                            <Typography variant="h6" color="text.secondary" sx={{ maxWidth: 900, lineHeight: 1.7, fontWeight: 400, mb: 3 }}>
                                {content.heroDescription}
                            </Typography>
                            <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                                {content.heroTags.map((tag) => (
                                    <Chip key={tag} label={tag} color="primary" variant="outlined" />
                                ))}
                            </Box>
                        </Box>
                    </Paper>

                    <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 3 }}>
                        {moduleCards.map((item) => (
                            <Paper
                                key={item.title}
                                sx={{
                                    p: 3,
                                    borderRadius: 3,
                                    border: '1px solid',
                                    borderColor: 'divider',
                                    transition: 'all 0.2s ease',
                                    '&:hover': {
                                        transform: 'translateY(-4px)',
                                        boxShadow: 4,
                                    },
                                }}
                            >
                                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 2 }}>
                                    <Box sx={{ width: 44, height: 44, borderRadius: 2, display: 'flex', alignItems: 'center', justifyContent: 'center', backgroundColor: 'rgba(102, 126, 234, 0.12)', color: 'primary.main' }}>
                                        {item.icon}
                                    </Box>
                                    <Typography variant="h6" sx={{ fontWeight: 700 }}>
                                        {item.title}
                                    </Typography>
                                </Box>
                                <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.8 }}>
                                    {item.description}
                                </Typography>
                            </Paper>
                        ))}
                    </Box>

                    <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', xl: '1.2fr 0.8fr' }, gap: 3 }}>
                        <Paper sx={{ p: 3.5, borderRadius: 3 }}>
                            <Typography variant="h5" sx={{ mb: 2, fontWeight: 700 }}>
                                {content.quickGuideTitle}
                            </Typography>
                            <Typography variant="body2" color="text.secondary" sx={{ mb: 3, lineHeight: 1.8 }}>
                                {content.quickGuideDescription}
                            </Typography>
                            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                                {quickStartSteps.map((step, index) => (
                                    <Box key={step} sx={{ display: 'flex', gap: 2, alignItems: 'flex-start' }}>
                                        <Box sx={{ minWidth: 36, width: 36, height: 36, borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', backgroundColor: 'primary.main', color: 'primary.contrastText', fontWeight: 700 }}>
                                            {index + 1}
                                        </Box>
                                        <Box>
                                            <Typography variant="subtitle1" sx={{ fontWeight: 600, mb: 0.5 }}>
                                                {content.stepsLabel} {index + 1}
                                            </Typography>
                                            <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.8 }}>
                                                {step}
                                            </Typography>
                                        </Box>
                                    </Box>
                                ))}
                            </Box>
                        </Paper>

                        <Paper sx={{ p: 3.5, borderRadius: 3 }}>
                            <Typography variant="h5" sx={{ mb: 2, fontWeight: 700 }}>
                                {content.principlesTitle}
                            </Typography>
                            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                                <Box sx={{ display: 'flex', gap: 1.5, alignItems: 'flex-start' }}>
                                    <DescriptionIcon color="primary" />
                                    <Box>
                                        <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 0.5 }}>
                                            {content.principles[0].title}
                                        </Typography>
                                        <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.7 }}>
                                            {content.principles[0].description}
                                        </Typography>
                                    </Box>
                                </Box>
                                <Box sx={{ display: 'flex', gap: 1.5, alignItems: 'flex-start' }}>
                                    <StorageIcon color="primary" />
                                    <Box>
                                        <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 0.5 }}>
                                            {content.principles[1].title}
                                        </Typography>
                                        <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.7 }}>
                                            {content.principles[1].description}
                                        </Typography>
                                    </Box>
                                </Box>
                                <Box sx={{ display: 'flex', gap: 1.5, alignItems: 'flex-start' }}>
                                    <PlayArrowIcon color="primary" />
                                    <Box>
                                        <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 0.5 }}>
                                            {content.principles[2].title}
                                        </Typography>
                                        <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.7 }}>
                                            {content.principles[2].description}
                                        </Typography>
                                    </Box>
                                </Box>
                                <Box sx={{ display: 'flex', gap: 1.5, alignItems: 'flex-start' }}>
                                    <SmartToyIcon color="primary" />
                                    <Box>
                                        <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 0.5 }}>
                                            {content.principles[3].title}
                                        </Typography>
                                        <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.7 }}>
                                            {content.principles[3].description}
                                        </Typography>
                                    </Box>
                                </Box>
                            </Box>
                        </Paper>
                    </Box>

                    <Paper sx={{ p: 3.5, borderRadius: 3, border: '1px dashed', borderColor: 'divider' }}>
                        <Typography variant="h5" sx={{ mb: 2, fontWeight: 700 }}>
                            {content.summaryTitle}
                        </Typography>
                        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, gap: 2.5 }}>
                            <Box>
                                <Typography variant="subtitle1" sx={{ fontWeight: 600, mb: 1 }}>
                                    {content.whatYouCanDoTitle}
                                </Typography>
                                <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.9 }}>
                                    {content.whatYouCanDoDescription}
                                </Typography>
                            </Box>
                            <Box>
                                <Typography variant="subtitle1" sx={{ fontWeight: 600, mb: 1 }}>
                                    {content.whatIsThisScreenTitle}
                                </Typography>
                                <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.9 }}>
                                    {content.whatIsThisScreenDescription}
                                </Typography>
                            </Box>
                        </Box>
                    </Paper>

                        <Paper variant="outlined" sx={{ p: 2.5, borderRadius: 3 }}>
                            <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                                {content.flowsDetailTitle}
                            </Typography>
                            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(3, 1fr)' }, gap: 2 }}>
                                <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, backgroundColor: 'background.default' }}>
                                    <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1 }}>
                                        {content.flowDetailCards[0].title}
                                    </Typography>
                                    <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.7 }}>
                                        {content.flowDetailCards[0].description}
                                    </Typography>
                                </Paper>
                                <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, backgroundColor: 'background.default' }}>
                                    <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1 }}>
                                        {content.flowDetailCards[1].title}
                                    </Typography>
                                    <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.7 }}>
                                        {content.flowDetailCards[1].description}
                                    </Typography>
                                </Paper>
                                <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, backgroundColor: 'background.default' }}>
                                    <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1 }}>
                                        {content.flowDetailCards[2].title}
                                    </Typography>
                                    <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.7 }}>
                                        {content.flowDetailCards[2].description}
                                    </Typography>
                                </Paper>
                            </Box>
                        </Paper>

                        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', xl: '1fr 1fr' }, gap: 3, mt: 3 }}>
                            <Paper variant="outlined" sx={{ p: 2.5, borderRadius: 3 }}>
                                <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                                    {content.speedTitle}
                                </Typography>
                                <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.8, mb: 2 }}>
                                    {content.speedDescription}
                                </Typography>
                                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5, mb: 2.5 }}>
                                    {speedHighlights.map((item, index) => (
                                        <Box key={item} sx={{ display: 'flex', gap: 1.25, alignItems: 'flex-start' }}>
                                            <Box sx={{ minWidth: 26, width: 26, height: 26, borderRadius: '50%', backgroundColor: 'rgba(102, 126, 234, 0.14)', color: 'primary.main', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 700, fontSize: '0.75rem' }}>
                                                {index + 1}
                                            </Box>
                                            <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.8 }}>
                                                {item}
                                            </Typography>
                                        </Box>
                                    ))}
                                </Box>

                                <Paper variant="outlined" sx={{ p: 2, borderRadius: 3, backgroundColor: 'background.default' }}>
                                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1.5, flexWrap: 'wrap', gap: 1 }}>
                                        <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>
                                            {content.speedExampleTitle}
                                        </Typography>
                                        <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                                            <Chip size="small" label="200 OK" sx={{ backgroundColor: 'rgba(76, 175, 80, 0.18)', color: '#2e7d32', fontWeight: 700 }} />
                                            <Chip size="small" label="9.04 s" sx={{ backgroundColor: 'rgba(255, 152, 0, 0.16)' }} />
                                            <Chip size="small" label="3.58 KB" sx={{ backgroundColor: 'rgba(102, 126, 234, 0.14)' }} />
                                        </Box>
                                    </Box>
                                    <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.7 }}>
                                        {content.speedExampleDescription}
                                    </Typography>
                                </Paper>
                            </Paper>

                            <Paper variant="outlined" sx={{ p: 2.5, borderRadius: 3 }}>
                                <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                                    {content.dataTitle}
                                </Typography>
                                <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.8, mb: 2 }}>
                                    {content.dataDescription}
                                </Typography>
                                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5, mb: 2.5 }}>
                                    {dataGenerationHighlights.map((item, index) => (
                                        <Box key={item} sx={{ display: 'flex', gap: 1.25, alignItems: 'flex-start' }}>
                                            <Box sx={{ minWidth: 26, width: 26, height: 26, borderRadius: '50%', backgroundColor: 'rgba(76, 175, 80, 0.14)', color: '#2e7d32', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 700, fontSize: '0.75rem' }}>
                                                {index + 1}
                                            </Box>
                                            <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.8 }}>
                                                {item}
                                            </Typography>
                                        </Box>
                                    ))}
                                </Box>

                                <Paper variant="outlined" sx={{ p: 2, borderRadius: 3, backgroundColor: '#0f172a', color: '#e2e8f0', overflow: 'hidden' }}>
                                    <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1.5, color: 'white' }}>
                                        {content.responseExampleTitle}
                                    </Typography>
                                    <Box component="pre" sx={{ m: 0, fontFamily: 'monospace', fontSize: '0.78rem', lineHeight: 1.7, whiteSpace: 'pre-wrap', color: '#cbd5e1' }}>
{`{
  "result": {
    "odf-auth": "SUCCESS",
    "odf-checkBlackGreyList": true,
    "odf-validateCustMinutesInfo": {
      "resultCode": "SUCCESS"
    },
    "odf-sendSms": {
      "resultCode": "SUCCESS"
    }
  }
}`}
                                    </Box>
                                </Paper>
                            </Paper>
                        </Box>

                        <Paper variant="outlined" sx={{ p: 2.5, borderRadius: 3, mt: 3 }}>
                            <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                                {content.benefitsTitle}
                            </Typography>
                            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(3, 1fr)' }, gap: 2 }}>
                                <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, backgroundColor: 'background.default' }}>
                                    <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1 }}>
                                        {content.benefitCards[0].title}
                                    </Typography>
                                    <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.7 }}>
                                        {content.benefitCards[0].description}
                                    </Typography>
                                </Paper>
                                <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, backgroundColor: 'background.default' }}>
                                    <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1 }}>
                                        {content.benefitCards[1].title}
                                    </Typography>
                                    <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.7 }}>
                                        {content.benefitCards[1].description}
                                    </Typography>
                                </Paper>
                                <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, backgroundColor: 'background.default' }}>
                                    <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1 }}>
                                        {content.benefitCards[2].title}
                                    </Typography>
                                    <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.7 }}>
                                        {content.benefitCards[2].description}
                                    </Typography>
                                </Paper>
                            </Box>
                        </Paper>

                        <Paper sx={{ p: 3.5, borderRadius: 3, mt: 3 }}>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 2 }}>
                                <AssessmentIcon color="primary" />
                                <Typography variant="h5" sx={{ fontWeight: 700 }}>
                                    {content.allureTitle}
                                </Typography>
                            </Box>
                            <Typography variant="body1" color="text.secondary" sx={{ lineHeight: 1.9, mb: 3 }}>
                                {content.allureDescription}
                            </Typography>

                            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', lg: '1.15fr 0.85fr' }, gap: 3, mb: 3 }}>
                                <Paper variant="outlined" sx={{ p: 2.5, borderRadius: 3, backgroundColor: 'background.default' }}>
                                    <Typography variant="subtitle1" sx={{ fontWeight: 700, mb: 2 }}>
                                        {content.allureVisualTitle}
                                    </Typography>

                                    <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '160px 1fr' }, gap: 2, mb: 2 }}>
                                        <Paper variant="outlined" sx={{ p: 2, borderRadius: 3, textAlign: 'center' }}>
                                            <Typography variant="h3" sx={{ fontWeight: 800, mb: 0.5 }}>
                                                160
                                            </Typography>
                                            <Typography variant="body2" color="text.secondary">
                                                {content.testCases}
                                            </Typography>
                                        </Paper>
                                        <Paper variant="outlined" sx={{ p: 2, borderRadius: 3 }}>
                                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                                                <Box
                                                    sx={{
                                                        width: 92,
                                                        height: 92,
                                                        borderRadius: '50%',
                                                        background: 'conic-gradient(#8bc34a 0deg 306deg, #ff7043 306deg 360deg)',
                                                        display: 'flex',
                                                        alignItems: 'center',
                                                        justifyContent: 'center',
                                                        flexShrink: 0,
                                                    }}
                                                >
                                                    <Box sx={{ width: 66, height: 66, borderRadius: '50%', backgroundColor: 'background.paper', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                                                        <Typography variant="h5" sx={{ fontWeight: 700 }}>
                                                            85%
                                                        </Typography>
                                                    </Box>
                                                </Box>
                                                <Box>
                                                    <Typography variant="subtitle1" sx={{ fontWeight: 700, mb: 0.5 }}>
                                                        {content.successRateTitle}
                                                    </Typography>
                                                    <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.7 }}>
                                                        {content.successRateDescription}
                                                    </Typography>
                                                </Box>
                                            </Box>
                                        </Paper>
                                    </Box>

                                    <Paper variant="outlined" sx={{ p: 2, borderRadius: 3, mb: 2 }}>
                                        <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1.5 }}>
                                            {content.suitesTitle}
                                        </Typography>
                                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                            <Typography variant="caption" sx={{ minWidth: 80, color: 'text.secondary' }}>
                                                {content.apiService}
                                            </Typography>
                                            <Box sx={{ flex: 1, height: 12, borderRadius: 999, overflow: 'hidden', display: 'flex', backgroundColor: 'action.hover' }}>
                                                <Box sx={{ width: '15%', backgroundColor: '#ff7043' }} />
                                                <Box sx={{ width: '85%', backgroundColor: '#8bc34a' }} />
                                            </Box>
                                        </Box>
                                    </Paper>

                                    <Paper variant="outlined" sx={{ p: 2, borderRadius: 3 }}>
                                        <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1.5 }}>
                                            {content.categoriesTrendTitle}
                                        </Typography>
                                        <Box sx={{ display: 'flex', alignItems: 'end', gap: 0.75, height: 120, mb: 2 }}>
                                            {[70, 72, 68, 18, 20, 25, 18, 22, 60, 64, 15, 14, 13, 12, 16].map((value, index) => (
                                                <Box key={index} sx={{ flex: 1, display: 'flex', alignItems: 'end', height: '100%' }}>
                                                    <Box sx={{ width: '100%', height: `${value}%`, borderRadius: 1, background: value > 55 ? 'linear-gradient(180deg, #ff7043 0%, #ff8a65 100%)' : value > 22 ? 'linear-gradient(180deg, #ffd54f 0%, #ffca28 100%)' : 'linear-gradient(180deg, #9ccc65 0%, #8bc34a 100%)' }} />
                                                </Box>
                                            ))}
                                        </Box>
                                        <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
                                            <Chip size="small" label={content.passed} sx={{ backgroundColor: 'rgba(139, 195, 74, 0.16)' }} />
                                            <Chip size="small" label={content.broken} sx={{ backgroundColor: 'rgba(255, 202, 40, 0.18)' }} />
                                            <Chip size="small" label={content.failed} sx={{ backgroundColor: 'rgba(255, 112, 67, 0.18)' }} />
                                        </Box>
                                    </Paper>
                                </Paper>

                                <Paper variant="outlined" sx={{ p: 2.5, borderRadius: 3, backgroundColor: 'background.default' }}>
                                    <Typography variant="subtitle1" sx={{ fontWeight: 700, mb: 2 }}>
                                        {content.meaningsTitle}
                                    </Typography>
                                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                                        {content.allureMeaningItems.map((item) => (
                                            <Box key={item.title}>
                                                <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 0.5 }}>
                                                    {item.title}
                                                </Typography>
                                                <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.7 }}>
                                                    {item.description}
                                                </Typography>
                                            </Box>
                                        ))}
                                    </Box>
                                </Paper>
                            </Box>

                            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', xl: '1fr 1fr' }, gap: 3, mb: 3 }}>
                                <Paper variant="outlined" sx={{ p: 2.5, borderRadius: 3 }}>
                                    <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                                        {content.allureFlowTitle}
                                    </Typography>
                                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                                        {allureSteps.map((step, index) => (
                                            <Box key={step} sx={{ display: 'flex', gap: 1.5, alignItems: 'flex-start' }}>
                                                <Box sx={{ minWidth: 30, width: 30, height: 30, borderRadius: '50%', backgroundColor: 'primary.main', color: 'primary.contrastText', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '0.85rem', fontWeight: 700 }}>
                                                    {index + 1}
                                                </Box>
                                                <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.8 }}>
                                                    {step}
                                                </Typography>
                                            </Box>
                                        ))}
                                    </Box>
                                </Paper>

                                <Paper variant="outlined" sx={{ p: 2.5, borderRadius: 3 }}>
                                    <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                                        {content.allureSetupTitle}
                                    </Typography>
                                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                                        {content.allureSetupSteps.map((item) => (
                                            <Paper key={item.title} variant="outlined" sx={{ p: 1.75, borderRadius: 2, backgroundColor: 'background.default' }}>
                                                <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 0.5 }}>
                                                    {item.title}
                                                </Typography>
                                                <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.7 }}>
                                                    {item.description}
                                                </Typography>
                                            </Paper>
                                        ))}
                                    </Box>
                                </Paper>
                            </Box>
                    </Paper>
                </Box>
            </Box>
        </DashboardLayout>
    );
}
