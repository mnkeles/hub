@echo off

SET PATH=%PATH%;C:\opt\node;C:\apache-maven-3.8.6\bin;C:\Java\jdk21.0.8_9\bin
SET JAVA_HOME=C:\Java\jdk21.0.8_9
SET config_home=C:\Apihub_Config\config
SET PORT_NUMBER=4053
SET project=DARWIN

cd..\..

echo Parameters: systemShortCode=%systemShortCode%, processFlow=%processFlow%, project=%project%, totalCalls=%totalCalls%

if "%parameterRequest%"=="" (
    call mvn clean test -Dtest=etiya.omniAutomation.service.impl.ApiCallServiceParallelCallTest ^
        -DtotalCalls=%totalCalls% ^
        -DsystemShortCode=%systemShortCode% ^
        -DprocessFlow=%processFlow% ^
        -Dproject=%project% ^
        -DCONFIG_HOME=%config_home% ^
        -Dspring.config.location=file:C:/Apihub_Config/config/application.yml
) else (
    call mvn clean test -Dtest=etiya.omniAutomation.service.impl.ApiCallServiceParallelCallTest ^
        -DtotalCalls=%totalCalls% ^
        -DsystemShortCode=%systemShortCode% ^
        -DprocessFlow=%processFlow% ^
        -Dproject=%project% ^
        -DparameterRequest=%parameterRequest% ^
        -DCONFIG_HOME=%config_home% ^
        -Dspring.config.location=file:C:/Apihub_Config/config/application.yml
)

if errorlevel 1 (
    echo ERROR: Tests failed
    exit /b 1
)