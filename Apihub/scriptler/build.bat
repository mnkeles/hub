SET PATH=%PATH%;C:\opt\node;C:\opt\apache-maven-3.9.6\bin;C:\opt\java\jdk-21\bin
SET JAVA_HOME=C:\opt\java\jdk-21
cd..
REM Stop backend before build to release JAR file
mvn clean install -DskipTests -Pproduction