@echo off
set "PATH=C:\opt\node;C:\Windows\System32;C:\Windows;%PATH%"
set "NEXT_PUBLIC_API_URL=http://172.31.27.4:4053"

cd ..\..

echo ApiHubFrontend servisi durduruluyor...
sc query ApiHubFrontend | find "RUNNING" >nul
IF %ERRORLEVEL% EQU 0 (
    net stop ApiHubFrontend
    IF %ERRORLEVEL% NEQ 0 (
        echo Servis durdurulamadi.
        exit /b 1
    )
) ELSE (
    echo Servis zaten çalışmıyor.
)

echo Frontend build baslatiliyor...
call npm run build
IF %ERRORLEVEL% NEQ 0 (
    echo Frontend build basarisiz.
    exit /b 1
)

echo ApiHubFrontend servisi baslatiliyor...
net start ApiHubFrontend
IF %ERRORLEVEL% NEQ 0 (
    echo Servis baslatilamadi.
    exit /b 1
)

echo Frontend deploy islemi basariyla tamamlandi.
