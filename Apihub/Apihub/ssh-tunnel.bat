@ECHO OFF
SETLOCAL EnableExtensions DisableDelayedExpansion
for /F %%a in ('echo prompt $E ^| cmd') do (
  set "ESC=%%a"
)
SETLOCAL EnableDelayedExpansion
@echo off
set user=necati.keles
echo !ESC![101;97m Info: The url information in application.yml must be url: jdbc:postgresql://localhost:3309/... !ESC![0m


ssh -o ServerAliveInterval=60 -o ServerAliveCountMax=10 -f %user%@172.31.27.6 -L 127.0.0.1:3309:172.31.27.4:5432 -N



pause