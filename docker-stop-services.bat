@echo off
echo ============================================
echo  Parando Servicos Docker
echo  Microservico: ms-video-processing
echo ============================================

docker-compose down

echo.
echo  Servicos parados e removidos.
echo  Nota: o RabbitMQ deste ambiente e gerenciado por este docker-compose.
echo.
