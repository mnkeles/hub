SET PATH=%PATH%;C:\opt\node;C:\opt\apache-maven-3.9.6\bin;C:\opt\java\jdk-21\bin
SET JAVA_HOME=C:\opt\java\jdk-21
set project=OMNI

rem allure raporlarını oluştur
cd C:/opt/ciserver-agent/workspace/AUTOMATION.TTGOMNI_API_SERVICE
npx allure generate --clean -o %project%/allure-report