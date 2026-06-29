'use client';

import React from 'react';
import {
    Box,
    Container,
    Typography,
    Button,
    Card,
    CardContent,
    Avatar,
    Chip,
    useTheme,
    alpha,
} from '@mui/material';
import { useRouter } from 'next/navigation';
import RocketLaunchIcon from '@mui/icons-material/RocketLaunch';
import SpeedIcon from '@mui/icons-material/Speed';
import AccountTreeIcon from '@mui/icons-material/AccountTree';
import SmartToyIcon from '@mui/icons-material/SmartToy';
import CodeIcon from '@mui/icons-material/Code';
import BarChartIcon from '@mui/icons-material/BarChart';
import CloudIcon from '@mui/icons-material/Cloud';
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome';

export default function LandingPage() {
    const router = useRouter();
    const theme = useTheme();

    const features = [
        {
            icon: <AccountTreeIcon sx={{ fontSize: 40 }} />,
            title: 'Process Flow Yönetimi',
            description: 'Servis adımlarını tek senaryoda toplayın, akış sırasını netleştirin ve tekrar eden testleri standartlaştırın.',
            color: '#667eea',
            highlights: [
                'Akış bazlı senaryo tasarımı',
                'Bağımlı servis zincirlerini görünür kılma',
                'Tekrar kullanılabilir test kurguları',
            ],
        },
        {
            icon: <CodeIcon sx={{ fontSize: 40 }} />,
            title: 'API Executor ile Uygulama İçi Tetikleme',
            description: 'Tekil servis veya akış çağrılarını proje, sistem ve parametre seçimiyle doğrudan ekran içinden çalıştırın.',
            color: '#43e97b',
            highlights: [
                'single, flow, flowV2 ve continue on error seçenekleri',
                'proje, sistem, akış ve step code bazlı tetikleme',
                'header, parametre ve response yönetimi',
            ],
        },
        {
            icon: <BarChartIcon sx={{ fontSize: 40 }} />,
            title: 'Allure Report İzleme',
            description: 'Allure raporları üzerinden test sonuçlarını, kalite görünümünü ve koşu özetlerini merkezi olarak takip edin.',
            color: '#fa709a',
            highlights: [
                'Suite ve story bazlı özet ekranları',
                'Durum, etiket ve ek dosya görünürlüğü',
                'Trend ve regresyon takibini kolaylaştıran yapı',
            ],
        },
        {
            icon: <SpeedIcon sx={{ fontSize: 40 }} />,
            title: 'Performans ve Süre Görünürlüğü',
            description: 'Response sürelerini, payload büyüklüğünü ve servis davranışını karşılaştırmalı okuyun.',
            color: '#f093fb',
            highlights: [
                'Response time karşılaştırmaları',
                'Status ve payload görünürlüğü',
                'Yavaşlayan adımları hızlı ayırt etme',
            ],
        },
        {
            icon: <SmartToyIcon sx={{ fontSize: 40 }} />,
            title: 'AI Sohbet ve HAR Analizi',
            description: 'Yapay zeka destekli sohbet, HAR inceleme ve akış yardımı ile analiz çıktılarınızı bağlama uygun şekilde yorumlayın.',
            color: '#4facfe',
            highlights: [
                'HAR incelemesinden sohbete devam etme',
                'akış ekranına özel yardımcı öneriler',
                'streaming yanıt ile görünür analiz süreci',
            ],
        },
        {
            icon: <CloudIcon sx={{ fontSize: 40 }} />,
            title: 'Ortam ve Veri Bağlantıları',
            description: 'Projeye özel ortamları, veri bağımlılıklarını ve çağrı bağlamını merkezi olarak yönetin.',
            color: '#00c6ff',
            highlights: [
                'Çoklu ortam seçimi',
                'Servis bağımlılıklarını görünür kılma',
                'Test verisini tekrar üretilebilir hale getirme',
            ],
        },
    ];

    const stats = [
        { value: '10K+', label: 'API Testi' },
        { value: '99.9%', label: 'Uptime' },
        { value: '50+', label: 'Aktif Proje' },
        { value: '24/7', label: 'Destek' },
    ];

    const showStats = false;
    const showFooter = false;

    const aiAssistantHighlights = [
        'Ana sohbet ekranında serbest metin veya dosya ekleri ile yapay zeka asistanı kullanılabilir.',
        'HAR inceleme ekranında analiz edilen bağlam sohbete aktarılıp aksiyon önerileri üretilebilir.',
        'Akış ekranındaki yardımcı sohbet mevcut sayfa bağlamına göre yönlendirme sunar.',
        'Streaming yapı ile işlem, düşünme ve yanıt aşamaları görünür şekilde takip edilir.',
    ];

    const allureHighlights = [
        'Suite, feature ve story bazlı test görünümü sunma',
        'Durum, severity ve ek dosyalarla sonuçları zenginleştirme',
        'Geçmiş koşular ve tekrar denemeler üzerinden trend takibi',
        'Başarılı, başarısız ve uyarı içeren testleri ayrıştırma',
    ];

    const traceabilityBenefits = [
        {
            title: 'Ekran içinden uçtan uca yönetim',
            description: 'API Executor, akışlar, ortam bağlantıları ve kalite ekranları aynı platform içinde birbirine bağlı çalışır.',
        },
        {
            title: 'AI ile bağlamsal analiz',
            description: 'HAR inceleme, sohbet ve akış asistanı birlikte kullanılarak analiz çıktıları sonraki aksiyonlara daha hızlı dönüşür.',
        },
        {
            title: 'Merkezi bilgi ve kalite görünümü',
            description: 'API bilgileri, dokümantasyon, performans sonuçları ve Allure raporları ekipler için ortak bir referans oluşturur.',
        },
    ];

    return (
        <Box sx={{ minHeight: '100vh', backgroundColor: 'background.default' }}>
            {/* Hero Section */}
            <Box
                sx={{
                    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                    color: 'white',
                    pt: 12,
                    pb: 16,
                    position: 'relative',
                    overflow: 'hidden',
                    '&::before': {
                        content: '""',
                        position: 'absolute',
                        top: 0,
                        left: 0,
                        right: 0,
                        bottom: 0,
                        background: 'radial-gradient(circle at 20% 50%, rgba(255,255,255,0.1) 0%, transparent 50%)',
                    },
                }}
            >
                <Container maxWidth="lg" sx={{ position: 'relative', zIndex: 1 }}>
                    <Box sx={{ textAlign: 'center', mb: 6 }}>
                        <Chip
                            icon={<AutoAwesomeIcon />}
                            label="AI Destekli API Test Platformu"
                            sx={{
                                mb: 3,
                                backgroundColor: 'rgba(255,255,255,0.2)',
                                color: 'white',
                                fontWeight: 600,
                                fontSize: '0.9rem',
                                py: 2.5,
                                px: 1,
                            }}
                        />
                        <Typography
                            variant="h1"
                            sx={{
                                fontWeight: 800,
                                fontSize: { xs: '2.5rem', md: '4rem' },
                                mb: 3,
                                textShadow: '0 2px 10px rgba(0,0,0,0.2)',
                            }}
                        >
                            APIHUB
                        </Typography>
                        <Typography
                            variant="h5"
                            sx={{
                                mb: 5,
                                opacity: 0.95,
                                fontWeight: 400,
                                maxWidth: 800,
                                mx: 'auto',
                                fontSize: { xs: '1.1rem', md: '1.5rem' },
                            }}
                        >
                            API test süreçlerinizi uygulama içinden tetikleyin, HAR kayıtlarını analiz edin,
                            yapay zeka desteğiyle yorumlayın ve kalite görünümünü tek açılış ekranında izleyin
                        </Typography>
                        <Box sx={{ display: 'flex', gap: 2, justifyContent: 'center', flexWrap: 'wrap' }}>
                            <Button
                                variant="contained"
                                size="large"
                                startIcon={<RocketLaunchIcon />}
                                onClick={() => router.push('/login')}
                                sx={{
                                    backgroundColor: 'white',
                                    color: '#667eea',
                                    py: 2,
                                    px: 4,
                                    fontSize: '1.1rem',
                                    fontWeight: 600,
                                    borderRadius: 3,
                                    boxShadow: '0 8px 20px rgba(0,0,0,0.2)',
                                    '&:hover': {
                                        backgroundColor: '#f5f5f5',
                                        transform: 'translateY(-2px)',
                                        boxShadow: '0 12px 24px rgba(0,0,0,0.3)',
                                    },
                                    transition: 'all 0.3s ease',
                                }}
                            >
                                Giriş Yap
                            </Button>
                            <Button
                                variant="outlined"
                                size="large"
                                onClick={() => {
                                    document.getElementById('features')?.scrollIntoView({ behavior: 'smooth' });
                                }}
                                sx={{
                                    borderColor: 'white',
                                    color: 'white',
                                    py: 2,
                                    px: 4,
                                    fontSize: '1.1rem',
                                    fontWeight: 600,
                                    borderRadius: 3,
                                    borderWidth: 2,
                                    '&:hover': {
                                        borderColor: 'white',
                                        backgroundColor: 'rgba(255,255,255,0.1)',
                                        borderWidth: 2,
                                    },
                                }}
                            >
                                Özellikleri Keşfet
                            </Button>
                        </Box>
                    </Box>

                    {/* Stats */}
                    {showStats && (
                        <Box sx={{ 
                            display: 'grid', 
                            gridTemplateColumns: { xs: 'repeat(2, 1fr)', md: 'repeat(4, 1fr)' },
                            gap: 3,
                            mt: 6 
                        }}>
                            {stats.map((stat, index) => (
                                <Box key={index} sx={{ textAlign: 'center' }}>
                                    <Typography
                                        variant="h3"
                                        sx={{ fontWeight: 700, mb: 1 }}
                                    >
                                        {stat.value}
                                    </Typography>
                                    <Typography variant="body1" sx={{ opacity: 0.9 }}>
                                        {stat.label}
                                    </Typography>
                                </Box>
                            ))}
                        </Box>
                    )}
                </Container>
            </Box>

            {/* Features Section */}
            <Container maxWidth="lg" sx={{ py: 12 }} id="features">
                <Box sx={{ textAlign: 'center', mb: 8 }}>
                    <Typography
                        variant="h2"
                        sx={{
                            fontWeight: 700,
                            mb: 2,
                            fontSize: { xs: '2rem', md: '3rem' },
                        }}
                    >
                        Platform Bilgileri ve Güçlü Özellikler
                    </Typography>
                    <Typography
                        variant="h6"
                        color="text.secondary"
                        sx={{ maxWidth: 700, mx: 'auto' }}
                    >
                        Uygulama içi tetikleme, AI destekli analiz, HAR inceleme ve Allure kalite görünümünü tek açılış ekranında keşfedin
                    </Typography>
                </Box>

                <Box sx={{ 
                    display: 'grid', 
                    gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)', lg: 'repeat(3, 1fr)' },
                    gap: 4 
                }}>
                    {features.map((feature, index) => (
                        <Card
                            key={index}
                            sx={{
                                height: '100%',
                                transition: 'all 0.3s ease',
                                border: '1px solid',
                                borderColor: 'divider',
                                '&:hover': {
                                    transform: 'translateY(-8px)',
                                    boxShadow: 6,
                                    borderColor: feature.color,
                                },
                            }}
                        >
                            <CardContent sx={{ p: 4 }}>
                                <Avatar
                                    sx={{
                                        width: 70,
                                        height: 70,
                                        mb: 3,
                                        backgroundColor: alpha(feature.color, 0.1),
                                        color: feature.color,
                                    }}
                                >
                                    {feature.icon}
                                </Avatar>
                                <Typography
                                    variant="h5"
                                    sx={{ fontWeight: 600, mb: 2 }}
                                >
                                    {feature.title}
                                </Typography>
                                <Typography variant="body1" color="text.secondary" sx={{ mb: 2.5, lineHeight: 1.8 }}>
                                    {feature.description}
                                </Typography>
                                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.25 }}>
                                    {feature.highlights.map((highlight: string) => (
                                        <Box key={highlight} sx={{ display: 'flex', gap: 1.25, alignItems: 'flex-start' }}>
                                            <Box
                                                sx={{
                                                    width: 8,
                                                    height: 8,
                                                    borderRadius: '50%',
                                                    backgroundColor: feature.color,
                                                    mt: '9px',
                                                    flexShrink: 0,
                                                }}
                                            />
                                            <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.7 }}>
                                                {highlight}
                                            </Typography>
                                        </Box>
                                    ))}
                                </Box>
                            </CardContent>
                        </Card>
                    ))}
                </Box>

                <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', xl: '1.1fr 0.9fr' }, gap: 4, mt: 6 }}>
                    <Card sx={{ height: '100%', border: '1px solid', borderColor: 'divider', boxShadow: 3 }}>
                        <CardContent sx={{ p: { xs: 3, md: 4 } }}>
                            <Chip
                                label="API Executor ile Uygulama İçi Tetikleme"
                                sx={{
                                    mb: 2,
                                    fontWeight: 700,
                                    backgroundColor: alpha('#43e97b', 0.14),
                                    color: '#1b5e20',
                                }}
                            />
                            <Typography variant="h4" sx={{ fontWeight: 700, mb: 2, fontSize: { xs: '1.8rem', md: '2.2rem' } }}>
                                Tekil istekleri ve akışları doğrudan platformdan çalıştırın
                            </Typography>
                            <Typography variant="body1" color="text.secondary" sx={{ lineHeight: 1.9, mb: 3 }}>
                                API Executor ekranı; proje, ortam bağlantısı, akış ve step bazlı çağrıları harici araca ihtiyaç duymadan uygulama içinden yönetmenizi sağlar.
                            </Typography>
                            <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', mb: 3 }}>
                                <Chip size="small" label="Single Request" sx={{ fontWeight: 700, backgroundColor: alpha('#43e97b', 0.18), color: '#1b5e20' }} />
                                <Chip size="small" label="Flow" sx={{ fontWeight: 700, backgroundColor: alpha('#4caf50', 0.18), color: '#2e7d32' }} />
                                <Chip size="small" label="Flow V2" sx={{ backgroundColor: alpha('#ff9800', 0.16) }} />
                                <Chip size="small" label="Continue On Error" sx={{ backgroundColor: alpha('#667eea', 0.14), color: '#3949ab' }} />
                            </Box>
                            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)' }, gap: 2, mb: 2.5 }}>
                                {[
                                    {
                                        title: 'Proje ve Sistem Seçimi',
                                        description: 'Çağrıyı doğru proje ve ortam bağlantısı altında başlatmak için gerekli bağlamı hazırlar.',
                                    },
                                    {
                                        title: 'Akış ve Step Yönetimi',
                                        description: 'Tek bir step ya da tüm akış mantığı üzerinden yürütme senaryosu belirlenebilir.',
                                    },
                                    {
                                        title: 'Header ve Parametre Alanları',
                                        description: 'Header, parametre ve kombinasyon seçenekleri ile farklı varyasyonlar hızlıca çalıştırılabilir.',
                                    },
                                    {
                                        title: 'Response İncelemesi',
                                        description: 'Status code, süre ve response detayları tek ekran üzerinden okunabilir.',
                                    },
                                ].map((item) => (
                                    <Box
                                        key={item.title}
                                        sx={{
                                            p: 2,
                                            borderRadius: 3,
                                            border: '1px solid',
                                            borderColor: 'divider',
                                            backgroundColor: 'background.paper',
                                        }}
                                    >
                                        <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1 }}>
                                            {item.title}
                                        </Typography>
                                        <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.7 }}>
                                            {item.description}
                                        </Typography>
                                    </Box>
                                ))}
                            </Box>
                            <Box sx={{ p: 2.25, borderRadius: 3, backgroundColor: '#0f172a', color: '#cbd5e1' }}>
                                <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1.25, color: 'white' }}>
                                    Yürütme Akışı
                                </Typography>
                                <Typography variant="body2" sx={{ lineHeight: 1.8, color: '#cbd5e1' }}>
                                    Proje seçimi → ortam bağlantısı → akış veya step seçimi → parametre ve header tanımı → response sonuçlarının tek ekranda incelenmesi.
                                </Typography>
                            </Box>
                        </CardContent>
                    </Card>

                    <Card sx={{ height: '100%', border: '1px solid', borderColor: 'divider' }}>
                        <CardContent sx={{ p: { xs: 3, md: 4 } }}>
                            <Chip
                                label="AI Sohbet ve HAR Analizi"
                                sx={{
                                    mb: 2,
                                    fontWeight: 700,
                                    backgroundColor: alpha('#4facfe', 0.14),
                                    color: '#1565c0',
                                }}
                            />
                            <Typography variant="h5" sx={{ fontWeight: 700, mb: 2.5 }}>
                                Yapay zeka destekli analiz akışını ürün içinde sürdürün
                            </Typography>
                            <Typography variant="body1" color="text.secondary" sx={{ lineHeight: 1.9, mb: 3 }}>
                                Sohbet ekranı, HAR inceleme akışı ve akış asistanı birlikte çalışarak teknik bulguları daha hızlı yorumlamanızı ve aksiyona dönüştürmenizi sağlar.
                            </Typography>
                            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)' }, gap: 1.5, mb: 3 }}>
                                {[
                                    'HAR yükleme ve analiz',
                                    'İncelenmiş bağlamla sohbete devam',
                                    'Akış ekranına özel yardımcı sohbet',
                                    'Streaming AI yanıt görünümü',
                                ].map((item) => (
                                    <Box
                                        key={item}
                                        sx={{
                                            p: 1.5,
                                            borderRadius: 2,
                                            backgroundColor: alpha('#4facfe', 0.08),
                                            border: '1px solid',
                                            borderColor: alpha('#4facfe', 0.25),
                                        }}
                                    >
                                        <Typography variant="body2" sx={{ fontWeight: 600 }}>
                                            {item}
                                        </Typography>
                                    </Box>
                                ))}
                            </Box>
                            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.75 }}>
                                {aiAssistantHighlights.map((item, index) => (
                                    <Box key={item} sx={{ display: 'flex', gap: 1.25, alignItems: 'flex-start' }}>
                                        <Box
                                            sx={{
                                                minWidth: 28,
                                                width: 28,
                                                height: 28,
                                                borderRadius: '50%',
                                                backgroundColor: alpha('#4facfe', 0.18),
                                                color: '#1565c0',
                                                display: 'flex',
                                                alignItems: 'center',
                                                justifyContent: 'center',
                                                fontWeight: 700,
                                                fontSize: '0.8rem',
                                            }}
                                        >
                                            {index + 1}
                                        </Box>
                                        <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.8 }}>
                                            {item}
                                        </Typography>
                                    </Box>
                                ))}
                            </Box>
                            <Box sx={{ mt: 3, p: 2, borderRadius: 3, border: '1px solid', borderColor: 'divider', backgroundColor: 'background.default' }}>
                                <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1 }}>
                                    AI kullanım alanları
                                </Typography>
                                <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.8 }}>
                                    Akış tasarımı, HAR inceleme devamı, hata yorumlama, performans bulgularını açıklama ve dokümantasyonla birlikte cevap üretme gibi senaryolarda yapay zeka desteği kullanılabilir.
                                </Typography>
                            </Box>
                        </CardContent>
                    </Card>
                </Box>

                <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', xl: '1.1fr 0.9fr' }, gap: 4, mt: 4 }}>
                    <Card sx={{ height: '100%', border: '1px solid', borderColor: 'divider', boxShadow: 3 }}>
                        <CardContent sx={{ p: { xs: 3, md: 4 } }}>
                            <Chip
                                label="Allure Report Özeti"
                                sx={{
                                    mb: 2,
                                    fontWeight: 700,
                                    backgroundColor: alpha('#fa709a', 0.14),
                                    color: '#ad1457',
                                }}
                            />
                            <Typography variant="h4" sx={{ fontWeight: 700, mb: 2, fontSize: { xs: '1.8rem', md: '2.2rem' } }}>
                                Test başarısını suite ve story bazında izleyin
                            </Typography>
                            <Typography variant="body1" color="text.secondary" sx={{ lineHeight: 1.9, mb: 3 }}>
                                Allure Report alanı; test sonuçlarını, kalite özetini, suite dağılımını ve story bazlı görünümü genel hatlarıyla açıklayan bir bilgi yapısı sunar.
                            </Typography>
                            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '180px 1fr' }, gap: 2, mb: 2.5 }}>
                                <Box sx={{ p: 2.5, borderRadius: 3, border: '1px solid', borderColor: 'divider', textAlign: 'center' }}>
                                    <Typography variant="h3" sx={{ fontWeight: 800, mb: 0.5 }}>
                                        Genel
                                    </Typography>
                                    <Typography variant="body2" color="text.secondary">
                                        test görünümü
                                    </Typography>
                                </Box>
                                <Box sx={{ p: 2.5, borderRadius: 3, border: '1px solid', borderColor: 'divider' }}>
                                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, flexWrap: 'wrap' }}>
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
                                                    Özet
                                                </Typography>
                                            </Box>
                                        </Box>
                                        <Box>
                                            <Typography variant="subtitle1" sx={{ fontWeight: 700, mb: 0.5 }}>
                                                Success Rate
                                            </Typography>
                                            <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.7 }}>
                                                Başarılı, başarısız veya uyarı içeren test sonuçlarının toplu görünümünü sade bir özetle sunar.
                                            </Typography>
                                        </Box>
                                    </Box>
                                </Box>
                            </Box>
                            <Box sx={{ p: 2, borderRadius: 3, border: '1px solid', borderColor: 'divider', mb: 2 }}>
                                <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1.5 }}>
                                    Suites
                                </Typography>
                                <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1.25 }}>
                                    Farklı test paketleri ve koşu grupları ayrı başlıklarda listelenebilir.
                                </Typography>
                                <Box sx={{ display: 'flex', height: 14, borderRadius: 999, overflow: 'hidden', backgroundColor: 'action.hover' }}>
                                    <Box sx={{ width: '15%', backgroundColor: '#ff7043' }} />
                                    <Box sx={{ width: '85%', backgroundColor: '#8bc34a' }} />
                                </Box>
                                <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 1 }}>
                                    <Typography variant="caption" sx={{ color: '#d84315', fontWeight: 700 }}>
                                        Uyarı ve hata alanları
                                    </Typography>
                                    <Typography variant="caption" sx={{ color: '#558b2f', fontWeight: 700 }}>
                                        Geçen test alanları
                                    </Typography>
                                </Box>
                            </Box>
                            <Box sx={{ p: 2, borderRadius: 3, border: '1px solid', borderColor: 'divider' }}>
                                <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 1.5 }}>
                                    Features by Stories
                                </Typography>
                                <Box sx={{ display: 'flex', height: 14, borderRadius: 999, overflow: 'hidden', backgroundColor: 'action.hover' }}>
                                    <Box sx={{ width: '33%', backgroundColor: '#ff5a3d' }} />
                                    <Box sx={{ width: '67%', backgroundColor: '#8bc34a' }} />
                                </Box>
                                <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 1 }}>
                                    <Typography variant="caption" sx={{ color: '#d84315', fontWeight: 700 }}>
                                        Sorunlu story alanları
                                    </Typography>
                                    <Typography variant="caption" sx={{ color: '#558b2f', fontWeight: 700 }}>
                                        Stabil story alanları
                                    </Typography>
                                </Box>
                            </Box>
                        </CardContent>
                    </Card>

                    <Card sx={{ height: '100%', border: '1px solid', borderColor: 'divider' }}>
                        <CardContent sx={{ p: { xs: 3, md: 4 } }}>
                            <Typography variant="h5" sx={{ fontWeight: 700, mb: 2.5 }}>
                                Suite detayında hangi bilgiler var?
                            </Typography>
                            <Box sx={{ p: 2, borderRadius: 3, backgroundColor: alpha('#fa709a', 0.08), border: '1px solid', borderColor: alpha('#fa709a', 0.2), mb: 2.5 }}>
                                <Typography variant="subtitle2" sx={{ fontWeight: 700, lineHeight: 1.7 }}>
                                    Örnek test kaydı görünümü
                                </Typography>
                                <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', mt: 1.25 }}>
                                    <Chip size="small" label="Passed" sx={{ backgroundColor: alpha('#4caf50', 0.18), color: '#2e7d32', fontWeight: 700 }} />
                                    <Chip size="small" label="Severity: critical" sx={{ backgroundColor: alpha('#ff7043', 0.18), color: '#d84315' }} />
                                    <Chip size="small" label="Layer: API" sx={{ backgroundColor: alpha('#667eea', 0.14), color: '#3949ab' }} />
                                </Box>
                            </Box>
                            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)' }, gap: 1.5, mb: 2.5 }}>
                                <Box sx={{ p: 1.75, borderRadius: 2, border: '1px solid', borderColor: 'divider', backgroundColor: 'background.default' }}>
                                    <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 0.5 }}>
                                        Execution
                                    </Typography>
                                    <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.7 }}>
                                        Request body ve response body ekleri ile test adımının ham çıktısını inceleyebilirsiniz.
                                    </Typography>
                                </Box>
                                <Box sx={{ p: 1.75, borderRadius: 2, border: '1px solid', borderColor: 'divider', backgroundColor: 'background.default' }}>
                                    <Typography variant="subtitle2" sx={{ fontWeight: 700, mb: 0.5 }}>
                                        History / Retries
                                    </Typography>
                                    <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.7 }}>
                                        Geçmiş koşular ve tekrar denemeler üzerinden kararsız test davranışlarını ayırt edebilirsiniz.
                                    </Typography>
                                </Box>
                            </Box>
                            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.75 }}>
                                {allureHighlights.map((item, index) => (
                                    <Box key={item} sx={{ display: 'flex', gap: 1.25, alignItems: 'flex-start' }}>
                                        <Box
                                            sx={{
                                                minWidth: 28,
                                                width: 28,
                                                height: 28,
                                                borderRadius: '50%',
                                                backgroundColor: alpha('#fa709a', 0.18),
                                                color: '#ad1457',
                                                display: 'flex',
                                                alignItems: 'center',
                                                justifyContent: 'center',
                                                fontWeight: 700,
                                                fontSize: '0.8rem',
                                            }}
                                        >
                                            {index + 1}
                                        </Box>
                                        <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.8 }}>
                                            {item}
                                        </Typography>
                                    </Box>
                                ))}
                            </Box>
                        </CardContent>
                    </Card>
                </Box>

                <Box sx={{ mt: 6 }}>
                    <Box sx={{ textAlign: 'center', mb: 4 }}>
                        <Typography variant="h4" sx={{ fontWeight: 700, mb: 2, fontSize: { xs: '1.8rem', md: '2.3rem' } }}>
                            Eski bilgi ekranı mantığını koruyan özet
                        </Typography>
                        <Typography variant="body1" color="text.secondary" sx={{ maxWidth: 860, mx: 'auto', lineHeight: 1.9 }}>
                            Bu ekran; uygulama içi akış tetikleme, HAR analizi, AI sohbet, süreç yönetimi ve Allure kalite özetini birlikte sunan kapsamlı bir bilgi alanı sağlar.
                        </Typography>
                    </Box>
                    <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(3, 1fr)' }, gap: 3 }}>
                        {traceabilityBenefits.map((item) => (
                            <Card key={item.title} sx={{ height: '100%', border: '1px solid', borderColor: 'divider' }}>
                                <CardContent sx={{ p: 3.5 }}>
                                    <Typography variant="h6" sx={{ fontWeight: 700, mb: 1.5 }}>
                                        {item.title}
                                    </Typography>
                                    <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.8 }}>
                                        {item.description}
                                    </Typography>
                                </CardContent>
                            </Card>
                        ))}
                    </Box>
                </Box>
            </Container>

            {/* CTA Section */}
            <Box
                sx={{
                    background: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
                    color: 'white',
                    py: 10,
                }}
            >
                <Container maxWidth="md">
                    <Box sx={{ textAlign: 'center' }}>
                        <Typography
                            variant="h3"
                            sx={{
                                fontWeight: 700,
                                mb: 3,
                                fontSize: { xs: '1.8rem', md: '2.5rem' },
                            }}
                        >
                            API Test Süreçlerinizi Dönüştürmeye Hazır mısınız?
                        </Typography>
                        <Typography
                            variant="h6"
                            sx={{ mb: 5, opacity: 0.95 }}
                        >
                            Hemen başlayın ve farkı görün
                        </Typography>
                        <Button
                            variant="contained"
                            size="large"
                            startIcon={<RocketLaunchIcon />}
                            onClick={() => router.push('/login')}
                            sx={{
                                backgroundColor: 'white',
                                color: '#f5576c',
                                py: 2,
                                px: 5,
                                fontSize: '1.2rem',
                                fontWeight: 600,
                                borderRadius: 3,
                                boxShadow: '0 8px 20px rgba(0,0,0,0.2)',
                                '&:hover': {
                                    backgroundColor: '#f5f5f5',
                                    transform: 'translateY(-2px)',
                                    boxShadow: '0 12px 24px rgba(0,0,0,0.3)',
                                },
                                transition: 'all 0.3s ease',
                            }}
                        >
                            Giriş Ekranına Geç
                        </Button>
                    </Box>
                </Container>
            </Box>

            {/* Footer */}
            {showFooter && (
                <Box
                    sx={{
                        backgroundColor: theme.palette.mode === 'dark' ? '#1a1a1a' : '#f8f9fa',
                        py: 6,
                        borderTop: '1px solid',
                        borderColor: 'divider',
                    }}
                >
                    <Container maxWidth="lg">
                        <Box sx={{ 
                            display: 'grid', 
                            gridTemplateColumns: { xs: '1fr', md: '2fr 1fr 1fr' },
                            gap: 4 
                        }}>
                            <Box>
                                <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                                    APIHUB
                                </Typography>
                                <Typography variant="body2" color="text.secondary">
                                    Modern API test ve otomasyon platformu. Geliştiriciler ve test ekipleri için
                                    güçlü, hızlı ve kullanımı kolay araçlar.
                                </Typography>
                            </Box>
                            <Box>
                                <Typography variant="subtitle1" sx={{ fontWeight: 600, mb: 2 }}>
                                    Hızlı Linkler
                                </Typography>
                                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                                    <Button
                                        onClick={() => router.push('/dashboard')}
                                        sx={{ justifyContent: 'flex-start', textTransform: 'none' }}
                                    >
                                        Dashboard
                                    </Button>
                                    <Button
                                        onClick={() => router.push('/dashboard/documents')}
                                        sx={{ justifyContent: 'flex-start', textTransform: 'none' }}
                                    >
                                        Dokümantasyon
                                    </Button>
                                    <Button
                                        onClick={() => router.push('/dashboard/chat')}
                                        sx={{ justifyContent: 'flex-start', textTransform: 'none' }}
                                    >
                                        AI Asistan
                                    </Button>
                                </Box>
                            </Box>
                            <Box>
                                <Typography variant="subtitle1" sx={{ fontWeight: 600, mb: 2 }}>
                                    İletişim
                                </Typography>
                                <Typography variant="body2" color="text.secondary">
                                    Email: support@apihub.com
                                </Typography>
                                <Typography variant="body2" color="text.secondary">
                                    Tel: +90 (555) 123 45 67
                                </Typography>
                            </Box>
                        </Box>
                        <Box sx={{ mt: 6, pt: 3, borderTop: '1px solid', borderColor: 'divider', textAlign: 'center' }}>
                            <Typography variant="body2" color="text.secondary">
                                © 2026 APIHUB. Tüm hakları saklıdır.
                            </Typography>
                        </Box>
                    </Container>
                </Box>
            )}
        </Box>
    );
}
