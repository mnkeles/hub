@echo off
REM ========================================
REM Next.js Production Build Script
REM For Jenkins CI/CD Pipeline
REM ========================================

echo [INFO] Starting Next.js Production Build...
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

echo [INFO] NPM Version:
npm --version
echo.

REM Clean previous build
echo [INFO] Cleaning previous build artifacts...
if exist .next (
    rmdir /s /q .next
    echo [SUCCESS] Removed .next directory
)
if exist out (
    rmdir /s /q out
    echo [SUCCESS] Removed out directory
)
echo.

REM Install dependencies
echo [INFO] Installing dependencies...
call npm ci --legacy-peer-deps
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to install dependencies
    exit /b 1
)
echo [SUCCESS] Dependencies installed successfully
echo.

REM Run build
echo [INFO] Building Next.js application...
call npm run build
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Build failed
    exit /b 1
)
echo [SUCCESS] Build completed successfully
echo.

REM Check build output
if exist .next (
    echo [SUCCESS] Build artifacts created in .next directory
    dir .next
) else (
    echo [ERROR] Build directory not found
    exit /b 1
)
echo.

echo [INFO] Build process completed successfully!
echo [INFO] Timestamp: %date% %time%
echo.
echo [INFO] To start the production server, run: npm start
echo.

exit /b 0
