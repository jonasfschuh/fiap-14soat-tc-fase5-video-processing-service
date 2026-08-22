@echo off
echo ============================================
echo  Iniciando Ambiente Completo (Container)
echo  Microservico: ms-video-processing
echo ============================================
echo.
echo  Criando rede compartilhada (se nao existir)...
docker network create fiap-network 2>nul
echo.

docker-compose up --build -d

echo.
echo    Servicos iniciados:
echo    - API:        http://localhost:8086
echo    - Swagger:    http://localhost:8086/swagger-ui.html
echo    - Jobs:       http://localhost:8086/api/jobs
echo    - Actuator:   http://localhost:8086/actuator
echo                  http://localhost:8086/actuator/health
echo                  http://localhost:8086/actuator/health/liveness
echo                  http://localhost:8086/actuator/health/readiness
echo.
echo    RabbitMQ:
echo    - Broker:     amqp://localhost:5672/fiapx
echo    - Management: http://localhost:15672
echo.
echo    Filas RabbitMQ:
echo    - video-uploaded  (entrada)
echo    - video-events    (saida)
echo.
