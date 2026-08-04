@echo off
setlocal ENABLEDELAYEDEXPANSION

REM =============================================
REM Script: Testes BDD com Cucumber
REM Microservico: ms-video-processing
REM =============================================

echo Iniciando testes BDD com Cucumber...
echo Comando: .\mvnw.cmd test -pl application -Dtest=CucumberRunner
echo Relatorio HTML: application\target\cucumber-reports\report.html
echo.

call .\mvnw.cmd test -pl application -Dtest=CucumberRunner
if errorlevel 1 (
  echo ERRO: Falha ao executar testes Cucumber.
  pause
  exit /b 1
)

echo.
echo ✅ Testes Cucumber concluidos.
echo    Relatorio: application\target\cucumber-reports\report.html
pause

endlocal
