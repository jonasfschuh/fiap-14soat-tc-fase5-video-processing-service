@echo off
echo ============================================
echo  Iniciando Ambiente de Desenvolvimento
echo  Microservico: ms-video-processing
echo ============================================
echo.
echo  PRE-REQUISITO: infraestrutura do iac-terraform rodando.
echo  Este servico NAO possui banco de dados.
echo  Os recursos abaixo sao provisionados pelo repositorio:
echo    fiap-14soat-tc-fase5-iac-terraform
echo.
echo   Recursos compartilhados (iac-terraform):
echo    - RabbitMQ:   localhost:5672   (vhost: fiapx / user: fiapx / pass: fiapx123)
echo    - RabbitMQ UI: http://localhost:15672
echo.
echo  Execute a aplicacao no IntelliJ com as seguintes variaveis de ambiente:
echo    SPRING_PROFILES_ACTIVE=docker
echo    SERVER_PORT=8086
echo    STORAGE_TYPE=local
echo    STORAGE_LOCAL_PATH=/app/videos/uploads
echo    STORAGE_OUTPUT_PATH=/app/videos/processed
echo    RABBITMQ_HOST=localhost
echo    RABBITMQ_PORT=5672
echo    RABBITMQ_VHOST=fiapx
echo    RABBITMQ_USER=fiapx
echo    RABBITMQ_PASSWORD=fiapx123
echo    AUTH_SERVICE_URL=http://localhost:8090
echo.
echo  Porta local da API: 8086
echo  http://localhost:8086/swagger-ui.html
echo  http://localhost:8086/api/jobs  (historico de jobs in-memory)
echo.
