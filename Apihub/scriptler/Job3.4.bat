SET PATH=%PATH%;C:\opt\node;C:\opt\apache-maven-3.9.6\bin;C:\opt\java\jdk-21\bin
SET JAVA_HOME=C:\opt\java\jdk-21
set project=OPERA

cd C:/opt/ciserver-agent/workspace/AUTOMATION.OPERA_API_SERVICE
npx pm2 startOrRestart ecosystem.json --only "allure-%project%"