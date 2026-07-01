@echo off

SET PATH=%PATH%;C:\opt\node;C:\apache-maven-3.8.6\bin;C:\Java\jdk21.0.8_9\bin
SET JAVA_HOME=C:\Java\jdk21.0.8_9
SET config_home=C:\Apihub_Config\config
SET PORT_NUMBER=8090
SET project=DARWIN

echo ============================================================================
echo DARWIN Test Automation Started
echo ============================================================================

REM 2 ust klasore cik (scriptler\Darwin -> scriptler -> ana dizin)
cd..\..

echo [STEP 1] Cleaning allure results...
if exist "%project%\allure-results" (
    del /s /q "%project%\allure-results\*" >nul 2>&1
    echo [SUCCESS] Results cleaned
)

echo [STEP 2] Copying history from previous report...
if exist "%project%\allure-report\history" (
    if exist "%project%\allure-results\history" rd /s /q "%project%\allure-results\history"
    mkdir "%project%\allure-results\history"
    xcopy /s /e /y /i "%project%\allure-report\history\*" "%project%\allure-results\history\"
    if %ERRORLEVEL% EQU 0 (
        echo [SUCCESS] History copied
    ) else (
        echo [WARNING] History copy failed with error code %ERRORLEVEL%
    )
) else (
    echo [INFO] No previous history found, starting fresh
)

echo [STEP 3] Stopping Allure server...
call pm2 stop ecosystem2.json --only allure-%project% >nul 2>&1
if errorlevel 1 (
    echo [INFO] Server was not running
)
echo [SUCCESS] Server stop attempted

echo [STEP 4] Running Maven tests...
echo Parameters: systemShortCode=%systemShortCode%, processFlow=%processFlow%, project=%project%
call mvn clean test -Dtest=etiya.omniAutomation.service.impl.ApiCallServiceTest ^
    -DsystemShortCode=%systemShortCode% ^
    -DprocessFlow=%processFlow% ^
    -Dproject=%project% ^
    -DCONFIG_HOME=%config_home% ^
    -Dspring.config.location=file:C:/Apihub_Config/config/application.yml
    
set TEST_EXIT_CODE=%ERRORLEVEL%
if %TEST_EXIT_CODE% NEQ 0 (
    echo [WARNING] Tests failed, continuing to generate report...
) else (
    echo [SUCCESS] Tests completed successfully
)

echo [STEP 4.1] Copying test results to project folder...
if exist "allure-results" (
    if not exist "%project%\allure-results" mkdir "%project%\allure-results"
    xcopy /s /e /y /q "allure-results\*" "%project%\allure-results\" >nul 2>&1
    echo [SUCCESS] Test results copied
) else (
    echo [WARNING] No test results found in allure-results folder
)

echo [STEP 5] Generating Allure report...
call allure generate %project%/allure-results --clean -o %project%/allure-report

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Allure report generation failed
    exit /b 1
)
echo [SUCCESS] Report generated

echo [STEP 6] Starting Allure server...
call pm2 startOrRestart ecosystem2.json --only allure-%project%
echo [SUCCESS] Server started

echo ============================================================================
echo Test execution completed! (Test exit code: %TEST_EXIT_CODE%)
echo Allure report: http://localhost:%PORT_NUMBER%
echo ============================================================================