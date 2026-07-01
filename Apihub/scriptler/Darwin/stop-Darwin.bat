@echo off
SET PATH=%PATH%;C:\opt\node;C:\apache-maven-3.8.6\bin;C:\Java\jdk21.0.8_9\bin
SET JAVA_HOME=C:\Java\jdk21.0.8_9

REM Sonlandırmak istediğiniz port numarası
set PORT_NUMBER=4053

REM Belirtilen portu kullanan işlemleri bul ve sonlandır
cd "C:\Apihub_Config\ecosystem"
npx pm2 stop ecosystem.json --only backend

 :found
echo İşlem başarıyla sonlandırıldı.