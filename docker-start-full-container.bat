@echo off
echo ============================================
echo  Iniciando Ambiente Completo (Container)
echo  Microservico: ms-video-processing
echo ============================================
echo.
echo  ATENCAO: Este servico conecta ao LocalStack externo.
echo  Certifique-se de que o repositorio abaixo esteja rodando:
echo    fiap-14soat-tc-fase5-video-upload-service (docker-compose up -d)
echo  O LocalStack e gerenciado por ele via rede: fiap-network
echo.
echo  Criando rede compartilhada (se nao existir)...
docker network create fiap-network 2>nul
echo.

docker-compose up --build -d

echo.
echo    Servicos iniciados:
echo    - API:        http://localhost:8084
echo    - Swagger:    http://localhost:8084/swagger-ui.html
echo    - Jobs:       http://localhost:8084/api/jobs
echo    - Actuator:   http://localhost:8084/actuator
echo                  http://localhost:8084/actuator/health
echo                  http://localhost:8084/actuator/health/liveness
echo                  http://localhost:8084/actuator/health/readiness
echo.
echo    LocalStack e StackPort (gerenciados pelo upload-service):
echo    - LocalStack: http://localhost:4566
echo    - StackPort:  http://localhost:8080
echo.
echo    Filas SQS consumidas:
echo    - video-uploaded  (entrada)
echo    - video-events    (saida)
echo.


