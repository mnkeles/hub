SET PATH=%PATH%;C:\opt\node;C:\apache-maven-3.8.6\bin;C:\Java\jdk21.0.8_9\bin
SET JAVA_HOME=C:\Java\jdk21.0.8_9
cd "C:\Apihub_Config\ecosystem"
npx pm2 startOrRestart ecosystem.json --only backend