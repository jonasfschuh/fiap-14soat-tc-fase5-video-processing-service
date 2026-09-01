# fiap-14soat-tc-fase5-video-processing-service

![Java 21](https://img.shields.io/badge/Java_21-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.4.5-%236DB33F.svg?style=for-the-badge&logo=springboot&logoColor=white)
![FFmpeg](https://img.shields.io/badge/FFmpeg-%23007808.svg?style=for-the-badge&logo=ffmpeg&logoColor=white)
![Swagger](https://img.shields.io/badge/OpenAPI_3-%2385EA2D.svg?style=for-the-badge&logo=swagger&logoColor=black)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-%230db7ed.svg?style=for-the-badge&logo=docker&logoColor=white)
![Kubernetes](https://img.shields.io/badge/Kubernetes-%23326CE5.svg?style=for-the-badge&logo=kubernetes&logoColor=white)
![New Relic](https://img.shields.io/badge/New_Relic-%231CE783.svg?style=for-the-badge&logo=newrelic&logoColor=white)
![Hexagonal Architecture](https://img.shields.io/badge/Hexagonal-Architecture-7B2D8B?style=for-the-badge)
![DDD](https://img.shields.io/badge/Domain--Driven_Design-430098?style=for-the-badge)
![Event-Driven](https://img.shields.io/badge/Event--Driven-FF6D00?style=for-the-badge)
![BDD](https://img.shields.io/badge/BDD-Cucumber-23D96C?style=for-the-badge&logo=cucumber&logoColor=white)
![Cucumber](https://img.shields.io/badge/Cucumber_7.18-%2323D96C.svg?style=for-the-badge&logo=cucumber&logoColor=white)
![JUnit 5](https://img.shields.io/badge/JUnit_5-%2325A162.svg?style=for-the-badge&logo=junit5&logoColor=white)
![JaCoCo](https://img.shields.io/badge/JaCoCo_%E2%89%A580%25-green?style=for-the-badge)
![Mockito](https://img.shields.io/badge/Mockito_5-%23EE4C2C.svg?style=for-the-badge)
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-%232671E5.svg?style=for-the-badge&logo=githubactions&logoColor=white)
![Maven](https://img.shields.io/badge/Apache_Maven-%23C71A36.svg?style=for-the-badge&logo=apachemaven&logoColor=white)

---

## 📑 Sumário

- [👤 Autor](#-autor)
- [📋 Descrição](#-descrição)
- [🏗️ Arquitetura](#️-arquitetura)
- [🛠️ Tecnologias Utilizadas](#️-tecnologias-utilizadas)
- [🔒 Proteção da Branch main](#-proteção-da-branch-main)
- [🚀 Execução Local](#-execução-local)
- [🔌 API — Swagger e Endpoints](#-api--swagger-e-endpoints)
- [🧪 Testes](#-testes)
- [🔗 Repositórios Relacionados](#-repositórios-relacionados)
- [⚠️ Troubleshooting](#️-troubleshooting)

---

## 👤 Autor

| Nome                 | E-mail                  | RM        | Discord          | WhatsApp        |
|----------------------|-------------------------|-----------|------------------|-----------------|
| Jonas Fernando Schuh | jonasschuh@hotmail.com  | rm369458  | jonasf.schuh     | 47 9 9960-1396  |

**Grupo:** 3 · FIAP 14SOAT Fase 5 — Hackathon

---

## 📋 Descrição

Este repositório contém o **microserviço Video Processing** da plataforma **FIAP X** — responsável por consumir eventos `video.uploaded` do **RabbitMQ**, baixar o arquivo do armazenamento local (ou em produção), executar o **ffmpeg** para extrair frames a 1fps, compactar os frames em um arquivo `.zip`, armazená-lo e publicar o resultado (`VIDEO_PROCESSED` ou `VIDEO_FAILED`) no exchange `video.events`.

A aplicação é desenvolvida em **Spring Boot 3 (Java 21)** com arquitetura hexagonal (Ports & Adapters) e opera como **worker assíncrono puro** — sem endpoints de negócio, sem banco de dados próprio.

> ℹ️ **Infraestrutura compartilhada:** o RabbitMQ é provisionado pelo repositório [`fiap-14soat-tc-fase5-iac-terraform`](https://github.com/jonasfschuh/fiap-14soat-tc-fase5-iac-terraform) via Kubernetes. Execute o `setup-cluster.sh` antes de iniciar este serviço.

### Principais funcionalidades

| Funcionalidade | Descrição |
|----------------|-----------|
| **Consumo de fila** | Consome da fila `video-uploaded` no RabbitMQ (exchange `video.events`) |
| **Download de vídeo** | Lê o vídeo do armazenamento local (volume compartilhado K8s) ou (prod) |
| **Extração de frames** | Executa `ffmpeg -vf fps=1` via `ProcessBuilder` — 1 frame/segundo |
| **Compactação ZIP** | Compacta todos os frames PNG em `frames_<videoId>.zip` |
| **Upload do ZIP** | Envia o ZIP para (prod) ou pasta local (dev) |
| **Publicação de eventos** | Publica `VIDEO_PROCESSED` ou `VIDEO_FAILED` no exchange `video.events` (RabbitMQ) |
| **Resiliência** | Em caso de falha, mensagem vai para DLQ (`video-uploaded-dlq`) — retry automático |
| **Observabilidade** | `GET /api/jobs` — últimos 100 jobs processados (in-memory, para debug) |

### Estrutura de Módulos Maven

```
fiap-14soat-tc-fase5-video-processing-service/
├── domain/           → Modelos (event/result), use case, ports de entrada e saída, exceções
├── application/      → Main class, JobStatusController, GlobalExceptionHandler, BDD (Cucumber)
├── infrastructure/   → SQS consumer/publisher, local adapters, ffmpeg adapter, configs
└── report-aggregate/ → Agregador de cobertura JaCoCo (multi-módulo)
```

---

## 🏗️ Arquitetura

### Arquitetura Hexagonal (Ports & Adapters)

```
┌────────────────────────────────────────────────────────────┐
│                    Application Layer                        │
│   VideoProcessingApplication  │  JobStatusController       │
│   GlobalExceptionHandler  │  SwaggerConfig                 │
└─────────────────────────┬──────────────────────────────────┘
                          │  Input Port
┌─────────────────────────▼──────────────────────────────────┐
│                     Domain Layer                            │
│   VideoUploadedEvent (record)  │  VideoJobResult (record)  │
│   VideoJobStatus (enum)                                     │
│   ProcessVideoUseCase                                       │
│   ProcessVideoInputPort                                     │
│   VideoDownloadPort  │  VideoZipStoragePort                 │
│   FfmpegPort  │  VideoProcessingEventPublisherPort          │
└─────────────────────────┬──────────────────────────────────┘
                          │  Output Ports
┌─────────────────────────▼──────────────────────────────────┐
│   Infrastructure Layer                       │
│   RabbitVideoUploadedConsumerAdapter (@RabbitListener)     │
│   RabbitVideoProcessingEventPublisherAdapter               │
│   LocalFileDownloadAdapter  │  LocalFileZipStorageAdapter  │
│   FfmpegAdapter  (ProcessBuilder)                          │
│   HttpCorrelationLoggingFilter                             │
└────────────────────────────────────────────────────────────┘
```

### Fluxo de Processamento

```
[RabbitMQ: exchange video.events / fila video-uploaded]
        │  @RabbitListener  (RabbitVideoUploadedConsumerAdapter)
        ▼
[ProcessVideoUseCase]
        │
        ├─1─► VideoDownloadPort.download(storageKey)
        │         └── LocalFileDownloadAdapter  (armazenamento local / K8s PVC)
        │         └── Path → arquivo temporário
        │
        ├─2─► FfmpegPort.extractFrames(videoPath, framesDir)
        │         └── ProcessBuilder("ffmpeg", "-i", ..., "-vf", "fps=1", ...)
        │         └── int frameCount
        │
        ├─3─► [zip frames internamente no use case]
        │         └── java.util.zip → frames_<videoId>.zip
        │
        ├─4─► VideoZipStoragePort.store(outputKey, zipPath)
        │         └── LocalFileZipStorageAdapter  (armazenamento local / K8s PVC)
        │
        ├─5─► [limpar temporários — finally block]
        │
        └─6─► VideoProcessingEventPublisherPort
                  ├── publishProcessed(result) → RabbitMQ exchange video.events
                  └── publishFailed(videoId, userId, reason) → RabbitMQ exchange video.events
```

### Eventos RabbitMQ

**Consome de:** exchange `video.events`, fila `video-uploaded` (routing key `video.uploaded`)
```json
{
  "videoId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "user-sub-cognito",
  "storageKey": "videos/user-id/uuid/video.mp4",
  "originalFilename": "video.mp4",
  "fileSizeBytes": 10485760,
  "mimeType": "video/mp4",
  "timestamp": "2025-01-01T10:00:00Z"
}
```

**Publica em:** exchange `video.events`
```json
// VIDEO_PROCESSED  (routing key: video.processed)
{
  "eventType": "VIDEO_PROCESSED",
  "videoId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "user-sub-cognito",
  "outputKey": "outputs/user-id/uuid/frames.zip",
  "frameCount": 42,
  "timestamp": "2025-01-01T10:05:00Z"
}

// VIDEO_FAILED  (routing key: video.failed)
{
  "eventType": "VIDEO_FAILED",
  "videoId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "user-sub-cognito",
  "errorMessage": "ffmpeg exited with code 1: ...",
  "timestamp": "2025-01-01T10:05:00Z"
}
```

### Infraestrutura Local (K8s + Docker Compose)

```
┌──────────────────────────────────────────────────────────────────────┐
│  Kubernetes (Docker Desktop) — namespace fiapx                       │
│  provisionado pelo fiap-14soat-tc-fase5-iac-terraform                │
│                                                                      │
│  ┌─────────────────────────┐   ┌────────────────────────────────┐    │
│  │  video-processing-api   │   │  RabbitMQ (rabbitmq-amqp-lb)   │    │
│  │  :8086                  │◄──│  :5672  (AMQP)                 │    │
│  │  (Spring Boot worker)   │   │  :15672 (Management UI)        │    │
│  └─────────────────────────┘   └────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────┘

ℹ️ Todos os serviços compartilham o RabbitMQ provisionado pelo iac-terraform.
   Execute o setup-cluster.sh antes de iniciar este serviço.
```

---

## 🛠️ Tecnologias Utilizadas

### Core

| Tecnologia | Versão | Uso |
|------------|--------|-----|
| **Java** | 21 | Linguagem da aplicação |
| **Spring Boot** | 3.4.5 | Framework principal |
| **ffmpeg** | latest | Extração de frames do vídeo (`apk add ffmpeg` na imagem) |
| **Spring AMQP** | 3.x | Integração com RabbitMQ |
| **Swagger / OpenAPI** | 3.x | Documentação interativa da API |

### Mensageria & Storage

| Tecnologia | Ambiente | Uso |
|------------|----------|-----|
| **RabbitMQ** | Local K8s / Prod | Consome `video-uploaded`, publica `video.processed`/`video.failed` |
| **Armazenamento local** | Local K8s | Vídeos e ZIPs em volume compartilhado (PVC `fiapx-video-pvc`) |

### Testes

| Ferramenta | Uso |
|------------|-----|
| **JUnit 5** | Testes unitários |
| **Mockito 5.x** | Mocks para testes unitários |
| **Cucumber 7.18** | Testes BDD (Behavior Driven Development) |
| **JaCoCo** | Cobertura de código (mínimo 80%) |

### DevOps & Infraestrutura

| Ferramenta | Versão | Uso |
|------------|--------|-----|
| **Docker** | 24.x | Containerização (com ffmpeg incluso) |
| **Docker Compose** | 2.x | Orquestração local |
| **Kubernetes** | Latest | Orquestração em produção (EKS) |
| **Maven** | 3.9+ | Build e gerenciamento de dependências |
| **New Relic** | 8.x | APM / Observabilidade |
| **GitHub Actions** | Latest | CI/CD |

---

## 🔒 Proteção da Branch main

As regras abaixo foram aplicadas em todos os repositórios da stack:

> *"Branch main protegida (sem commits diretos). Uso obrigatório de Pull Requests para merge. Deploy automático das branches de produção."*

### Regras configuradas no GitHub → Settings → Branches

| Regra | Valor |
|---|---|
| **Require a pull request before merging** | ✅ Ativado — bloqueia commits diretos na `main` |
| **Required approvals** | `1` revisão obrigatória (OBS: desabilitado neste estudo — grupo com 1 pessoa) |
| **Dismiss stale reviews on new commits** | ✅ Ativado |
| **Require status checks to pass** | ✅ Ativado — bloqueia merge se PR Validation falhar |
| **Require branches to be up to date** | ✅ Ativado |
| **Do not allow bypassing** | ✅ Ativado |

### Status check obrigatório neste repositório

| Check | Job no `pr-validation.yaml` |
|---|---|
| `build-and-test` | Build Maven + testes unitários + cobertura JaCoCo |

> ⚠️ O status check só aparece para seleção no GitHub após a **primeira execução bem-sucedida** do PR Validation.

---

## 🚀 Execução Local

### Pré-requisitos

- [Java 21+](https://adoptium.net/)
- [Maven 3.9+](https://maven.apache.org/)
- [Docker Desktop 4.25+](https://www.docker.com/products/docker-desktop/) com Kubernetes habilitado
- **[`fiap-14soat-tc-fase5-iac-terraform`](https://github.com/jonasfschuh/fiap-14soat-tc-fase5-iac-terraform) provisionado** (fornece RabbitMQ no K8s)

---

### ⚙️ Pré-requisito: provisionar o iac-terraform

```bash
# No diretório do iac-terraform:
bash scripts/setup-cluster.sh
```

Isso inicializa o RabbitMQ (e PostgreSQL) no Kubernetes (namespace `fiapx`) e expõe as portas:
- RabbitMQ AMQP: `localhost:5672`
- RabbitMQ Management: `localhost:15672`

---

### Opção A — Stack completa com Docker Compose *(recomendado)*

```bash
# Build e start
docker compose up --build

# Em background
docker compose up -d
```

| Serviço | URL | Descrição |
|---------|-----|-----------|
| **API (debug)** | http://localhost:8086 | Video Processing Service |
| **Swagger UI** | http://localhost:8086/swagger-ui.html | Documentação |
| **Health** | http://localhost:8086/actuator/health | Status do serviço |
| **Jobs (debug)** | http://localhost:8086/api/jobs | Últimos 100 jobs processados |

```bash
# Parar
docker compose down
docker compose down -v   # remove volumes
```

---

### Opção B — Apenas a aplicação na IDE

```bash
# Garantir que o iac-terraform está provisionado (RabbitMQ disponível em localhost:5672)
# Executar este serviço com profile local
./mvnw spring-boot:run -pl application \
  -Dspring-boot.run.arguments="--spring.profiles.active=local"
```

---

### Variáveis de ambiente relevantes

| Variável | Default | Descrição                 |
|----------|---------|---------------------------|
| `SERVER_PORT` | `8086` | Porta HTTP                |
| `RABBITMQ_HOST` | `localhost` | Host do RabbitMQ          |
| `RABBITMQ_PORT` | `5672` | Porta AMQP                |
| `RABBITMQ_VHOST` | `/` | Virtual host              |
| `RABBITMQ_USER` | `guest` | Usuário RabbitMQ          |
| `RABBITMQ_PASSWORD` | `guest` | Senha RabbitMQ            |
| `STORAGE_TYPE` | `local` | `local` ou `prod`         |
| `STORAGE_LOCAL_PATH` | `./storage` | Diretório de vídeos (local) |
| `STORAGE_OUTPUT_PATH` | `./outputs` | Diretório de ZIPs (local) |
| `FFMPEG_TIMEOUT_MINUTES` | `10` | Timeout máximo do ffmpeg  |

---

## 🔌 API — Swagger e Endpoints

### Endpoints de Gestão

Este serviço é um **worker puro** — não expõe endpoints de negócio. Apenas:

| Método | Path | Descrição |
|--------|------|-----------|
| `GET` | `/actuator/health` | Health check (K8s probes) |
| `GET` | `/actuator/health/liveness` | Liveness probe |
| `GET` | `/actuator/health/readiness` | Readiness probe |
| `GET` | `/api/jobs` | Últimos 100 jobs processados nesta instância (debug) |

### Exemplo — Listar jobs recentes

```bash
curl http://localhost:8086/api/jobs
```

**Response 200 OK:**
```json
[
  {
    "videoId": "550e8400-e29b-41d4-a716-446655440000",
    "userId": "user-123",
    "status": "DONE",
    "frameCount": 42,
    "outputKey": "outputs/user-123/uuid/frames.zip",
    "processedAt": "2025-01-01T10:05:00"
  },
  {
    "videoId": "660e8400-f39b-51d4-b827-557766551111",
    "userId": "user-456",
    "status": "FAILED",
    "frameCount": 0,
    "errorMessage": "ffmpeg exited with code 1",
    "processedAt": "2025-01-01T10:06:00"
  }
]
```

---

## 🧪 Testes

### Executar todos os testes

```bash
mvn clean test
```

### Executar apenas testes unitários (Domain)

```bash
mvn test -pl domain
```

### Executar apenas testes BDD (Cucumber - Application)

```bash
mvn test -pl application
```

### Executar com relatório de cobertura

```bash
mvn clean verify

# Abrir relatório (Windows)
start report-aggregate/target/site/jacoco-aggregate/index.html
```

### Estratégia de Testes

| Tipo | Ferramenta | Localização | Cobertura alvo |
|------|------------|-------------|----------------|
| Unitários (domain) | JUnit 5 + Mockito | `domain/` | ≥ 80% |
| BDD | Cucumber | `application/` | Fluxos principais |
| Unitários (infra) | JUnit 5 + Mockito | `infrastructure/` | ≥ 80% |

### Cenários BDD cobertos

| Cenário | Resultado esperado |
|---------|--------------------|
| Upload de vídeo válido processado com sucesso | `VIDEO_PROCESSED` publicado, frameCount > 0 |
| ffmpeg falha (vídeo corrompido) | `VIDEO_FAILED` publicado, mensagem vai para DLQ |
| Vídeo não encontrado no storage | `VIDEO_FAILED` publicado |

---

### 🎬 Vídeos de Apresentação

| Fase | Link |
|------|------|
| Fase 1 | [Apresentação Tech Challenge 1 — RaceForce](https://youtu.be/EKwE8l4yE1M) |
| Fase 2 | [Apresentação Tech Challenge 2 — RaceForce](https://youtu.be/95ml0-H9Vf4) |
| Fase 3 | [Apresentação Tech Challenge 3 — RaceForce](https://www.youtube.com/watch?v=KB-FC_4zsPE) |
| Fase 4 | [Apresentação Tech Challenge 4 — RaceForce](https://www.youtube.com/watch?v=vR3x4kW0l90) |
| Fase 5 | *(em desenvolvimento)* |

---

## 🔗 Repositórios Relacionados

| Ordem | Repositório | Descrição |
|-------|-------------|-----------|
| 1 | [fiap-14soat-tc-fase5-iac-terraform](https://github.com/jonasfschuh/fiap-14soat-tc-fase5-iac-terraform) | Banco de dados, RabbitMQ — infraestrutura AWS |
| 2 | [fiap-14soat-tc-fase5-auth](https://github.com/jonasfschuh/fiap-14soat-tc-fase5-auth) | Login Authorizer            |
| 3 | [fiap-14soat-tc-fase5-video-upload-service](https://github.com/jonasfschuh/fiap-14soat-tc-fase5-video-upload-service) | Upload + RabbitMQ publisher |
| 4 | [fiap-14soat-tc-fase5-video-processing-service](https://github.com/jonasfschuh/fiap-14soat-tc-fase5-video-processing-service) | Processa vídeo, extrai frames, gera ZIP |
| 5 | [fiap-14soat-tc-fase5-video-status-service](https://github.com/jonasfschuh/fiap-14soat-tc-fase5-video-status-service) | Status e metadados dos vídeos por usuário |
| 6 | [fiap-14soat-tc-fase5-video-download-service](https://github.com/jonasfschuh/fiap-14soat-tc-fase5-video-download-service) | Download do ZIP via presigned URL |
| 7 | [fiap-14soat-tc-fase5-notification-service](https://github.com/jonasfschuh/fiap-14soat-tc-fase5-notification-service) | Notificação por e-mail em caso de erro/conclusão |
| 8 | [fiap-14soat-tc-fase5-observability](https://github.com/jonasfschuh/fiap-14soat-tc-fase5-observability) | Prometheus + Grafana — dashboards e alertas |

---

## ⚠️ Troubleshooting

### `/actuator/prometheus` retorna HTTP 500

**Sintoma:** o endpoint `/actuator/prometheus` está exposto no `application.yml` mas responde com `500 Internal Server Error`.

**Causa raiz:** a dependência `micrometer-registry-prometheus` estava ausente. O `spring-boot-starter-actuator` expõe o endpoint `/actuator/prometheus`, mas sem o registry do Micrometer para Prometheus não há implementação para servir as métricas — resultando em 500.

**Solução:** adicionar a dependência no módulo `application/pom.xml`:

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

---

### Swagger UI — `POST /auth/login` falha com CORS / "Failed to fetch"

**Sintoma:** ao clicar em **Execute** no Swagger UI para `POST /auth/login`, o browser exibe:

```
Failed to fetch.
Possible Reasons: CORS / Network Failure
URL scheme must be "http" or "https" for CORS request.
```

**Causa raiz:** o `SwaggerConfiguration` sobrescrevia o servidor do path `/auth/login` apontando diretamente para `http://localhost:8090` (o auth service). Isso fazia o browser enviar uma requisição *cross-origin* (de `localhost:8086` para `localhost:8090`), violando a política de CORS.

**Solução:** remover o override de servidor para `/auth/login`. O endpoint já é disponibilizado como proxy pelo `AuthProxyController` neste próprio serviço (`POST localhost:8086/auth/login`), eliminando completamente o problema de CORS.

---

## ⚙️ CI/CD — Configurando o Self-Hosted Runner

O pipeline de deploy deste repositório utiliza um **GitHub Actions self-hosted runner** rodando na máquina local com acesso ao cluster Kubernetes (Docker Desktop).

### Pré-requisitos do runner

Certifique-se de que a máquina possui instalado:

| Ferramenta | Versão mínima | Verificar |
|-----------|---------------|-----------|
| Docker Desktop (com K8s habilitado) | 4.x+ | `docker version` |
| kubectl | 1.28+ | `kubectl version --client` |
| Java 21 (JDK) | 21+ | `java -version` |
| Maven Wrapper | — | `.\mvnw.cmd -version` |

> Para o repositório IAC, também é necessário `terraform` (1.5+) e `helm` (3.x+).

### Passo a passo — configurar o runner

#### 1. Acesse as configurações do repositório no GitHub

```
GitHub → Repositório → Settings → Actions → Runners → New self-hosted runner
```

#### 2. Escolha o sistema operacional

Selecione **Windows** e a arquitetura **x64**.

#### 3. Baixe e configure o runner

Execute os comandos exibidos pelo GitHub na sua máquina local (PowerShell como Administrador):

```powershell
# Criar pasta para o runner (ajuste o caminho se necessário)
mkdir C:\actions-runner; cd C:\actions-runner

# Baixar o runner (substitua a URL pela exibida no GitHub)
Invoke-WebRequest -Uri https://github.com/actions/runner/releases/download/vX.X.X/actions-runner-win-x64-X.X.X.zip -OutFile actions-runner.zip

# Extrair
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::ExtractToDirectory("$PWD\actions-runner.zip", "$PWD")

# Configurar (use o token gerado pelo GitHub na tela de configuração)
.\config.cmd --url https://github.com/<org>/<repo> --token <TOKEN-GERADO-PELO-GITHUB>
```

#### 4. Instalar como serviço Windows (recomendado)

```powershell
# Instalar e iniciar como serviço Windows (executa automaticamente no boot)
.\svc.cmd install
.\svc.cmd start

# Verificar status
.\svc.cmd status
```

#### 5. Verificar o runner no GitHub

```
GitHub → Repositório → Settings → Actions → Runners
```

O runner deve aparecer com status **Idle** (verde). A partir daí, qualquer push para `main` ou `develop` disparará o pipeline de deploy automaticamente.

### Verificar o deploy após o pipeline

```powershell
# Listar pods no namespace fiapx
kubectl get pods -n fiapx

# Verificar logs do serviço
kubectl logs -l app=<nome-do-app> -n fiapx --tail=50

# Acessar via Swagger (após NGINX Ingress estar ativo)
# http://localhost/<caminho>/swagger-ui.html
```

### Gerenciar o runner

```powershell
# Parar o serviço
.\svc.cmd stop

# Remover o serviço
.\svc.cmd uninstall

# Remover o runner do GitHub
.\config.cmd remove --token <TOKEN>
```

> 💡 **Dica:** Para múltiplos repositórios, crie uma pasta separada para cada runner (ex: `C:\actions-runner\auth`, `C:\actions-runner\upload`) e repita o processo para cada um.

<div align="center">

**🎓 Desenvolvido para o Tech Challenge FIAP 14SOAT — Fase 5 (Hackathon)**

*Projeto Acadêmico — Pós-Graduação em Arquitetura de Software · FIAP 2025/2026*

[⬆ Voltar ao topo](#fiap-14soat-tc-fase5-video-processing-service)

</div>
