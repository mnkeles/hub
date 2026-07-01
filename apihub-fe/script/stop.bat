@echo off
REM ========================================
REM Next.js Production Stop Script
REM For Jenkins CI/CD Pipeline
REM ========================================

echo [INFO] Stopping Next.js Production Server...
echo [INFO] Timestamp: %date% %time%
echo.

REM Set error handling
setlocal enabledelayedexpansion

REM Check if port 4054 is in use
echo [INFO] Checking for running server on port 4054...
netstat -ano | findstr :4054 | findstr LISTENING >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [WARNING] No server found running on port 4054
    exit /b 0
)

REM Kill all processes using port 4054
echo [INFO] Stopping server processes...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :4054 ^| findstr LISTENING') do (
    echo [INFO] Killing process with PID: %%a
    taskkill /F /PID %%a >nul 2>&1
    if !ERRORLEVEL! EQU 0 (
        echo [SUCCESS] Process %%a stopped successfully
    ) else (
        echo [WARNING] Failed to stop process %%a
    )
)

REM Wait a moment
timeout /t 2 /nobreak >nul

REM Verify server is stopped
netstat -ano | findstr :4054 | findstr LISTENING >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [SUCCESS] Server stopped successfully
) else (
    echo [ERROR] Server may still be running
    exit /b 1
)
echo.

echo [INFO] Timestamp: %date% %time%
echo.

exit /b 0
