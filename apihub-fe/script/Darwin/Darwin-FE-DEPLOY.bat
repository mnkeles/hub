@echo off
setlocal enabledelayedexpansion
set "PATH=C:\opt\node;C:\Windows\System32;C:\Windows;%PATH%"
set "NEXT_PUBLIC_API_URL=http://172.31.27.4:4053"

REM ============================================================
REM 1) Port 4054'te calisan islemi durdur
REM ============================================================
echo [INFO] Port 4054 kontrol ediliyor...
netstat -ano | findstr :4054 | findstr LISTENING >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [INFO] Port 4054 uzerinde calisan islem bulundu, durduruluyor...
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr :4054 ^| findstr LISTENING') do (
        echo [INFO] PID %%a sonlandiriliyor...
        taskkill /F /PID %%a >nul 2>&1
        if !ERRORLEVEL! EQU 0 (
            echo [SUCCESS] PID %%a durduruldu.
        ) else (
            echo [WARNING] PID %%a durdurulamadi.
        )
    )
    timeout /t 2 /nobreak >nul
) else (
    echo [INFO] Port 4054 bos, devam ediliyor.
)

REM ============================================================
REM 2) Proje kok dizinine gec (script\Darwin -> kok = 2 ust dizin)
REM ============================================================
cd /d "%~dp0..\.."
echo [INFO] Calisma dizini: %CD%

REM ============================================================
REM 3) Build al
REM ============================================================
echo [INFO] npm run build baslatiliyor...
call npm run build
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Build basarisiz.
    exit /b 1
)
echo [SUCCESS] Build tamamlandi.

REM ============================================================
REM 4) Uygulamamizi arka planda npm run dev ile kaldir
REM ============================================================
if not exist "logs" mkdir logs

echo [INFO] npm run dev arka planda baslatiliyor (port 4054)...
start /B "" cmd /c "npm run dev > logs\dev.log 2>&1"

REM ============================================================
REM 5) Uygulamanin ayaga kalmasini bekle (max 60 saniye)
REM ============================================================
echo [INFO] Uygulama dinlemeye gecene kadar bekleniyor...
set RETRY=0
:WAIT_LOOP
    timeout /t 3 /nobreak >nul
    netstat -ano | findstr :4054 | findstr LISTENING >nul 2>&1
    if %ERRORLEVEL% EQU 0 goto SERVER_UP
    set /a RETRY+=1
    if !RETRY! GEQ 20 goto SERVER_FAIL
    echo [INFO] Bekleniyor... (!RETRY!/20)
goto WAIT_LOOP

:SERVER_FAIL
echo [ERROR] Uygulama 60 saniye icerisinde ayaga kalkmadi.
echo [ERROR] Detaylar icin logs\dev.log dosyasina bakin.
exit /b 1

:SERVER_UP
echo [SUCCESS] Uygulama basariyla ayaga kalkmis durumda (port 4054).
echo [INFO] Loglar: logs\dev.log
echo [INFO] Deploy islemi basariyla tamamlandi.
exit /b 0
