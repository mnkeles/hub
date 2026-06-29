@echo off
REM ========================================
REM Next.js Production Start (Background)
REM For Jenkins CI/CD Pipeline
REM ========================================

echo [INFO] Starting Next.js Production Server in Background...
echo [INFO] Timestamp: %date% %time%
echo.

REM Set error handling
setlocal enabledelayedexpansion

REM Navigate to project root
cd /d "%~dp0.."
echo [INFO] Working Directory: %CD%
echo.

REM Check if Node.js is installed
where node >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Node.js is not installed or not in PATH
    exit /b 1
)

REM Check if build exists
if not exist ".next" (
    echo [ERROR] Build directory (.next) not found
    echo [ERROR] Please run build.bat first
    exit /b 1
)
echo [SUCCESS] Build directory found
echo.

REM Check if port 4054 is already in use
echo [INFO] Checking if port 4054 is available...
netstat -ano | findstr :4054 | findstr LISTENING >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [WARNING] Port 4054 is already in use
    echo [INFO] Attempting to kill existing process...
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr :4054 ^| findstr LISTENING') do (
        taskkill /F /PID %%a >nul 2>&1
        echo [SUCCESS] Killed process with PID: %%a
    )
    timeout /t 2 /nobreak >nul
)
echo [SUCCESS] Port 4054 is available
echo.

REM Create logs directory if it doesn't exist
if not exist "logs" (
    mkdir logs
)

REM Start the production server in background
echo [INFO] Starting production server in background...
start /B "" npm start > logs\server.log 2>&1

REM Wait a moment for server to start
timeout /t 3 /nobreak >nul

REM Check if server is running
netstat -ano | findstr :4054 | findstr LISTENING >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] Production server started successfully on port 4054
    echo [INFO] Logs are being written to: logs\server.log
    echo [INFO] To stop the server, run: script\stop.bat
) else (
    echo [ERROR] Failed to start production server
    echo [ERROR] Check logs\server.log for details
    exit /b 1
)
echo.

echo [INFO] Server is running in background
echo [INFO] Timestamp: %date% %time%
echo.

exit /b 0
