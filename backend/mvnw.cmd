@echo off
setlocal

where mvn >nul 2>nul
if %ERRORLEVEL%==0 (
  mvn %*
  exit /b %ERRORLEVEL%
)

set MVN_VERSION=3.9.9
set WRAPPER_DIR=%~dp0.mvn\wrapper
set MAVEN_HOME=%WRAPPER_DIR%\apache-maven-%MVN_VERSION%
set MVN_EXE=%MAVEN_HOME%\bin\mvn.cmd

if not exist "%MVN_EXE%" (
  powershell -NoProfile -ExecutionPolicy Bypass -File "%WRAPPER_DIR%\download-maven.ps1" -Version "%MVN_VERSION%" -TargetDir "%WRAPPER_DIR%"
  if %ERRORLEVEL% neq 0 exit /b %ERRORLEVEL%
)

"%MVN_EXE%" %*
exit /b %ERRORLEVEL%
