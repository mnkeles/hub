SET PATH=%PATH%;C:\opt\node;C:\opt\apache-maven-3.9.6\bin;C:\opt\java\jdk-21\bin
SET JAVA_HOME=C:\opt\java\jdk-21
cd..
npx pm2 startOrRestart ecosystem.json --only "backend"