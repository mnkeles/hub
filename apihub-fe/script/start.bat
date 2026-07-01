@echo off
REM ========================================
REM Next.js Production Start Script
REM For Jenkins CI/CD Pipeline
REM ========================================

echo [INFO] Starting Next.js Production Server...
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

echo [INFO] Node Version:
node --version
echo.

REM Check if build exists
if not exist .next (
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

REM Start the production server
echo [INFO] Starting production server on port 4054...
echo [INFO] Press Ctrl+C to stop the server
echo.

call npm start

REM If npm start exits, check error level
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to start production server
    exit /b 1
)

exit /b 0
