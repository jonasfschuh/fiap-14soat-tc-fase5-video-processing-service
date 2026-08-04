@echo off
echo ============================================
echo  Parando Servicos Docker
echo  Microservico: ms-video-processing
echo ============================================

docker-compose down

echo.
echo  Servicos parados e removidos.
echo  Nota: O LocalStack e gerenciado pelo repositorio
echo  fiap-14soat-tc-fase5-video-upload-service.
echo  Para para-lo, execute docker-stop-services.bat naquele repositorio.
echo.


