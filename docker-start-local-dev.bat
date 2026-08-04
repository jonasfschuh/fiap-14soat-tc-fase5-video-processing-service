@echo off
echo ============================================
echo  Iniciando Ambiente de Desenvolvimento
echo  Microservico: ms-video-processing
echo ============================================
echo.
echo  ATENCAO: Este servico NAO possui banco de dados.
echo  O LocalStack e gerenciado pelo repositorio:
echo    fiap-14soat-tc-fase5-video-upload-service
echo  Certifique-se de que ele esteja rodando antes:
echo    cd ..\fiap-14soat-tc-fase5-video-upload-service
echo    docker-compose up -d localstack stackport
echo.
echo  Criando rede compartilhada (se nao existir)...
docker network create fiap-network 2>nul
echo.

echo  Execute a aplicacao no IntelliJ com as seguintes variaveis de ambiente:
echo    SPRING_PROFILES_ACTIVE=docker
echo    SERVER_PORT=8084
echo    STORAGE_TYPE=local
echo    STORAGE_LOCAL_PATH=./storage
echo    STORAGE_OUTPUT_PATH=./outputs
echo    AWS_SQS_ENABLED=true
echo    AWS_ENDPOINT_OVERRIDE=http://localhost:4566
echo    AWS_REGION=us-east-1
echo    AWS_ACCESS_KEY_ID=test
echo    AWS_SECRET_ACCESS_KEY=test
echo    SQS_QUEUE_VIDEO_UPLOADED=http://localhost:4566/000000000000/video-uploaded
echo    SQS_QUEUE_VIDEO_EVENTS=http://localhost:4566/000000000000/video-events
echo.
echo  Porta local da API: 8084
echo  http://localhost:8084/swagger-ui.html
echo  http://localhost:8084/api/jobs  (historico de jobs in-memory)
echo.
