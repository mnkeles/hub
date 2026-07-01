@echo off
SET PATH=%PATH%;C:\opt\node;C:\apache-maven-3.8.6\bin;C:\Java\jdk21.0.8_9\bin
SET JAVA_HOME=C:\Java\jdk21.0.8_9

cd ..\..

echo ApiHubBackend servisi durduruluyor...
sc query ApiHubBackend | find "RUNNING" >nul
IF %ERRORLEVEL% EQU 0 (
    net stop ApiHubBackend
    IF %ERRORLEVEL% NEQ 0 (
        echo Servis durdurulamadi.
        exit /b 1
    )
) ELSE (
    echo Servis zaten çalışmıyor.
)

echo Maven build baslatiliyor...
call mvn clean package -DskipTests -Pproduction
IF %ERRORLEVEL% NEQ 0 (
    echo Maven build basarisiz.
    exit /b 1
)

echo ApiHubBackend servisi baslatiliyor...
net start ApiHubBackend
IF %ERRORLEVEL% NEQ 0 (
    echo Servis baslatilamadi.
    exit /b 1
)

echo Deploy islemi basariyla tamamlandi.
