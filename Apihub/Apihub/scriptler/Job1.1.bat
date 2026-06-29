@echo off

rem Allure results klasörünü temizle
del /s /q ..\%project%\allure-results\*

rem Allure reports klasöründeki history klasörünü kopyala

xcopy /s /e /q ..\%project%\allure-report\history ..\%project%\allure-results\history

rem Hata kontrolü
if errorlevel 1 (
  echo "Hata oluştu!"
  exit 1
) else (
  echo "History kopyalandı"
  exit 0
)

echo "if blok çalışmadı"
