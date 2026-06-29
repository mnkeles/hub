SET PATH=%PATH%;C:\opt\node;C:\opt\apache-maven-3.9.6\bin;C:\opt\java\jdk-21\bin
SET JAVA_HOME=C:\opt\java\jdk-21
SET config_home=C:\Users\takademi\Documents\config
SET project=DHIRAAGU
:: systemShortCodeOAB değişkenini varsayılan olarak OABdev yap

cd..
mvn clean test -Dtest=etiya.omniAutomation.service.impl.ApiCallServiceImplTest -DsystemShortCode=%systemShortCode% -DsystemShortCodeOAB=%systemShortCodeOAB% -DstepValues=%stepValues% -DprocessValues=%processValues% -DE2EprocessFlow=%E2EprocessFlow% -Dproject=%project% -DCONFIG_HOME=%config_home%
