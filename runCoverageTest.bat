@echo off
setlocal ENABLEDELAYEDEXPANSION

REM =============================================
REM Script: Cobertura + SonarCloud
REM Microservico: ms-video-processing
REM =============================================

REM Token fixo para testes (projeto privado)
REM ATENCAO: Remover antes de tornar o projeto publico
set SONAR_TOKEN=4f67e76b9b4af9f0a280c0cad1cb50ea8dfc3297

REM Executa testes com cobertura usando o Maven Wrapper
call .\mvnw.cmd clean verify -Pcoverage
if errorlevel 1 (
  echo ERRO: Falha ao executar cobertura (mvn clean verify -Pcoverage).
  pause
  exit /b 1
)

REM Executa analise no SonarCloud usando o token informado
call .\mvnw.cmd sonar:sonar -Dsonar.host.url=https://sonarcloud.io -Dsonar.login=!SONAR_TOKEN! -Dsonar.projectKey=fiap-14soat-tc-fase5-video-processing
if errorlevel 1 (
  echo ERRO: Falha ao executar analise no SonarCloud.
  pause
  exit /b 1
)

echo ✅ Processo concluido: cobertura gerada e analise enviada ao SonarCloud.
pause

endlocal
