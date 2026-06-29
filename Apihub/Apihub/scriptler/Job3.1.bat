@echo off

SET PATH=%PATH%;C:\opt\node;C:\opt\apache-maven-3.9.6\bin;C:\opt\java\jdk-21\bin
SET JAVA_HOME=C:\opt\java\jdk-21
REM Sonlandırmak istediğiniz port numarası
set PORT_NUMBER=8086
set project=OPERA

REM Belirtilen portu kullanan işlemleri bul ve sonlandır
cd..
npx pm2 stop ecosystem.json --only "allure-${project}"
