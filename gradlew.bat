@echo off
setlocal
where gradle >nul 2>nul
if errorlevel 1 (
  echo Gradle was not found in PATH.
  echo Install Gradle or use the GitHub Actions build.
  exit /b 1
)
gradle %*
exit /b %ERRORLEVEL%
