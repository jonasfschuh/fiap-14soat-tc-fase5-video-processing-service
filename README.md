# fiap-14soat-tc-fase5-video-processing-service

![Java 21](https://img.shields.io/badge/Java_21-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.4.5-%236DB33F.svg?style=for-the-badge&logo=springboot&logoColor=white)
![FFmpeg](https://img.shields.io/badge/FFmpeg-%23007808.svg?style=for-the-badge&logo=ffmpeg&logoColor=white)
![Swagger](https://img.shields.io/badge/OpenAPI_3-%2385EA2D.svg?style=for-the-badge&logo=swagger&logoColor=black)
![AWS](https://img.shields.io/badge/AWS-%23FF9900.svg?style=for-the-badge&logo=amazonwebservices&logoColor=white)
![Amazon S3](https://img.shields.io/badge/Amazon_S3-%23569A31.svg?style=for-the-badge&logo=amazons3&logoColor=white)
![Amazon SQS](https://img.shields.io/badge/Amazon_SQS-%23FF9900.svg?style=for-the-badge&logo=amazonsqs&logoColor=white)
![Amazon EKS](https://img.shields.io/badge/Amazon_EKS-%23FF9900.svg?style=for-the-badge&logo=amazoneks&logoColor=white)
![LocalStack](https://img.shields.io/badge/LocalStack-%23000000.svg?style=for-the-badge&logo=localstack&logoColor=white)
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

---

## 👤 Autor

| Nome                 | E-mail                  | RM        | Discord          | WhatsApp        |
|----------------------|-------------------------|-----------|------------------|-----------------|
| Jonas Fernando Schuh | jonasschuh@hotmail.com  | rm369458  | jonasf.schuh     | 47 9 9960-1396  |

**Grupo:** 3 · FIAP 14SOAT Fase 5 — Hackathon

---

## 📋 Descrição

Este repositório contém o **microserviço Video Processing** da plataforma **FIAP X** — responsável por consumir eventos de vídeos enviados, baixar o arquivo do **Amazon S3**, executar o **ffmpeg** para extrair frames a 1fps, compactar os frames em um arquivo `.zip`, armazená-lo no S3 e publicar o resultado (`VIDEO_PROCESSED` ou `VIDEO_FAILED`) na fila **Amazon SQS**.

A aplicação é desenvolvida em **Spring Boot 3 (Java 21)** com arquitetura hexagonal (Ports & Adapters) e opera como **worker assíncrono puro** — sem endpoints de negócio, sem banco de dados próprio.

> ⚠️ **Dependência de infraestrutura:** este serviço conecta ao **LocalStack** e à **`fiap-network`** gerenciados pelo [`video-upload-service`](https://github.com/jonasfschuh/fiap-14soat-tc-fase5-video-upload-service). Suba o `video-upload-service` antes de iniciar este.

### Principais funcionalidades

| Funcionalidade | Descrição |
|----------------|-----------|
| **Consumo de fila** | Polling a cada 3s na fila SQS `video-uploaded` |
| **Download de vídeo** | Baixa o vídeo do S3 ou pasta local para processamento |
| **Extração de frames** | Executa `ffmpeg -vf fps=1` via `ProcessBuilder` — 1 frame/segundo |
| **Compactação ZIP** | Compacta todos os frames PNG em `frames_<videoId>.zip` |
| **Upload do ZIP** | Envia o ZIP para S3 (prod) ou pasta local (dev) |
| **Publicação de eventos** | Publica `VIDEO_PROCESSED` ou `VIDEO_FAILED` na fila `video-events` |
| **Resiliência** | Em caso de falha, não deleta a mensagem SQS → retry via DLQ |
| **Observabilidade** | `GET /api/jobs` — últimos 100 jobs processados (in-memory, para debug) |

### Estrutura de Módulos Maven

```
fiap-14soat-tc-fase5-video-processing-service/
├── domain/           → Modelos (event/result), use case, ports de entrada e saída, exceções
├── application/      → Main class, JobStatusController, GlobalExceptionHandler, BDD (Cucumber)
├── infrastructure/   → SQS consumer/publisher, S3/local adapters, ffmpeg adapter, configs
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
│                  Infrastructure Layer                       │
│   SqsVideoUploadedConsumerAdapter  (polling @Scheduled)    │
│   SqsVideoProcessingEventPublisherAdapter                  │
│   LocalFileDownloadAdapter  │  S3DownloadAdapter           │
│   LocalFileZipStorageAdapter  │  S3ZipStorageAdapter       │
│   FfmpegAdapter  (ProcessBuilder)                          │
│   HttpCorrelationLoggingFilter  │  SqsMessageLogger        │
└────────────────────────────────────────────────────────────┘
```

### Fluxo de Processamento

```
[SQS: video-uploaded]
        │  polling 3s  (SqsVideoUploadedConsumerAdapter)
        ▼
[ProcessVideoUseCase]
        │
        ├─1─► VideoDownloadPort.download(storageKey)
        │         ├── LocalFileDownloadAdapter  (profile: local/docker)
        │         └── S3DownloadAdapter         (profile: aws/k8s/prod)
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
        │         ├── LocalFileZipStorageAdapter  (profile: local/docker)
        │         └── S3ZipStorageAdapter         (profile: aws/k8s/prod)
        │
        ├─5─► [limpar temporários — finally block]
        │
        └─6─► VideoProcessingEventPublisherPort
                  ├── publishProcessed(result) → SQS: video-events
                  └── publishFailed(videoId, userId, reason) → SQS: video-events
```

### Eventos SQS

**Consome de:** `video-uploaded`
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

**Publica em:** `video-events`
```json
// VIDEO_PROCESSED
{
  "eventType": "VIDEO_PROCESSED",
  "videoId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "user-sub-cognito",
  "outputKey": "outputs/user-id/uuid/frames.zip",
  "frameCount": 42,
  "timestamp": "2025-01-01T10:05:00Z"
}

// VIDEO_FAILED
{
  "eventType": "VIDEO_FAILED",
  "videoId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "user-sub-cognito",
  "errorMessage": "ffmpeg exited with code 1: ...",
  "timestamp": "2025-01-01T10:05:00Z"
}
```

### Infraestrutura Local (Docker Compose)

```
┌──────────────────────────────────────────────────────────────────────┐
│  fiap-network (bridge — compartilhada, criada pelo video-upload-service) │
│                                                                        │
│  ┌──────────────────────┐   ┌──────────────────────────────────────┐  │
│  │  video-processing-api│   │  LocalStack (do video-upload-service) │  │
│  │  :8084               │   │  :4566  S3 + SQS + SNS               │  │
│  │  (Spring Boot worker) │   └──────────────────────────────────────┘  │
│  └──────────────────────┘                                              │
└──────────────────────────────────────────────────────────────────────┘

⚠️ Este serviço NÃO tem LocalStack próprio.
   Conecta ao LocalStack do video-upload-service via fiap-network.
```

---

## 🛠️ Tecnologias Utilizadas

### Core

| Tecnologia | Versão | Uso |
|------------|--------|-----|
| **Java** | 21 | Linguagem da aplicação |
| **Spring Boot** | 3.4.5 | Framework principal |
| **ffmpeg** | latest | Extração de frames do vídeo (`apk add ffmpeg` na imagem) |
| **AWS SDK v2** | 2.28.0 | S3 (download/upload) + SQS (consumer/publisher) |
| **Swagger / OpenAPI** | 3.x | Documentação interativa da API |

### Mensageria & Storage

| Tecnologia | Ambiente | Uso |
|------------|----------|-----|
| **Amazon SQS** | AWS | Consome `video-uploaded`, publica `video-events` |
| **Amazon S3** | AWS | Download de vídeos, upload de ZIPs |
| **LocalStack** | Local/Docker | Emulação de SQS + S3 (do video-upload-service) |

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
- [Docker Desktop 4.25+](https://www.docker.com/products/docker-desktop/)
- **[`video-upload-service`](https://github.com/jonasfschuh/fiap-14soat-tc-fase5-video-upload-service) rodando** (fornece LocalStack + fiap-network)

---

### ⚙️ Pré-requisito: subir o video-upload-service primeiro

```bash
# No diretório do video-upload-service:
docker compose up -d
```

Isso inicializa o LocalStack (SQS + S3), cria as filas `video-uploaded` e `video-events`, e cria a rede `fiap-network`.

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
| **API (debug)** | http://localhost:8084 | Video Processing Service |
| **Swagger UI** | http://localhost:8084/swagger-ui.html | Documentação |
| **Health** | http://localhost:8084/actuator/health | Status do serviço |
| **Jobs (debug)** | http://localhost:8084/api/jobs | Últimos 100 jobs processados |

```bash
# Parar
docker compose down
docker compose down -v   # remove volumes
```

---

### Opção B — Apenas a aplicação na IDE

```bash
# Subir apenas o video-upload-service (LocalStack)
# (no diretório do video-upload-service)
docker compose up -d

# Executar este serviço com profile local
./mvnw spring-boot:run -pl application \
  -Dspring-boot.run.arguments="--spring.profiles.active=local"
```

---

### Variáveis de ambiente relevantes

| Variável | Default | Descrição |
|----------|---------|-----------|
| `SERVER_PORT` | `8084` | Porta HTTP |
| `AWS_SQS_ENABLED` | `false` | Habilita polling SQS |
| `AWS_ENDPOINT_OVERRIDE` | _(vazio)_ | URL do LocalStack (ex: `http://fiap-localstack:4566`) |
| `STORAGE_TYPE` | `local` | `local` ou `s3` |
| `STORAGE_LOCAL_PATH` | `./storage` | Diretório de vídeos (local) |
| `STORAGE_OUTPUT_PATH` | `./outputs` | Diretório de ZIPs (local) |
| `FFMPEG_TIMEOUT_MINUTES` | `10` | Timeout máximo do ffmpeg |
| `SQS_QUEUE_VIDEO_UPLOADED` | LocalStack URL | Fila de entrada |
| `SQS_QUEUE_VIDEO_EVENTS` | LocalStack URL | Fila de saída |
| `S3_BUCKET` | `fiap-video-uploads` | Bucket S3 |

---

## 🔌 API — Swagger e Endpoints

Este serviço é um **worker puro** — não expõe endpoints de negócio. Apenas:

| Método | Path | Descrição |
|--------|------|-----------|
| `GET` | `/actuator/health` | Health check (K8s probes) |
| `GET` | `/actuator/health/liveness` | Liveness probe |
| `GET` | `/actuator/health/readiness` | Readiness probe |
| `GET` | `/api/jobs` | Últimos 100 jobs processados nesta instância (debug) |

### Exemplo — Listar jobs recentes

```bash
curl http://localhost:8084/api/jobs
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
| ffmpeg falha (vídeo corrompido) | `VIDEO_FAILED` publicado, mensagem não deletada do SQS |
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
| 1 | [fiap-14soat-tc-fase5-iac-terraform](https://github.com/jonasfschuh/fiap-14soat-tc-fase5-iac-terraform) | VPC, ECS/EKS, S3, SQS, RDS, Cognito — infraestrutura AWS |
| 2 | [fiap-14soat-tc-fase5-auth-lambda](https://github.com/jonasfschuh/fiap-14soat-tc-fase5-auth-lambda) | Lambda Authorizer + Cognito + API Gateway |
| 3 | [fiap-14soat-tc-fase5-video-upload-service](https://github.com/jonasfschuh/fiap-14soat-tc-fase5-video-upload-service) | Upload + SQS publisher — **master do ambiente local** |
| 4 | [fiap-14soat-tc-fase5-video-processing-service](https://github.com/jonasfschuh/fiap-14soat-tc-fase5-video-processing-service) | **Este repositório** — Processa vídeo, extrai frames, gera ZIP |
| 5 | [fiap-14soat-tc-fase5-video-status-service](https://github.com/jonasfschuh/fiap-14soat-tc-fase5-video-status-service) | Status e metadados dos vídeos por usuário |
| 6 | [fiap-14soat-tc-fase5-video-download-service](https://github.com/jonasfschuh/fiap-14soat-tc-fase5-video-download-service) | Download do ZIP via presigned URL S3 |
| 7 | [fiap-14soat-tc-fase5-notification-service](https://github.com/jonasfschuh/fiap-14soat-tc-fase5-notification-service) | Notificação por e-mail em caso de erro/conclusão |
| 8 | [fiap-14soat-tc-fase5-observability](https://github.com/jonasfschuh/fiap-14soat-tc-fase5-observability) | Prometheus + Grafana — dashboards e alertas |

---

<div align="center">

**🎓 Desenvolvido para o Tech Challenge FIAP 14SOAT — Fase 5 (Hackathon)**

*Projeto Acadêmico — Pós-Graduação em Arquitetura de Software · FIAP 2025/2026*

[⬆ Voltar ao topo](#fiap-14soat-tc-fase5-video-processing-service)

</div>