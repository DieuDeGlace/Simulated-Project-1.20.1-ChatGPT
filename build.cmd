@echo off
setlocal

set "SIMULATED_REPOSITORY=%~dp0"
set "SIMULATED_BUILD_MODE=%~1"
if "%SIMULATED_BUILD_MODE%"=="" set "SIMULATED_BUILD_MODE=release"
if not "%~1"=="" shift

powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%SIMULATED_REPOSITORY%tools\build.ps1" -Mode "%SIMULATED_BUILD_MODE%" %1 %2 %3 %4 %5 %6 %7 %8 %9
exit /b %ERRORLEVEL%
