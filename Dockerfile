FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY . .
# 'sh mvnw' evita depender do shebang/bit executavel do mvnw, que pode ser
# perdido em checkouts de runners Windows. O sed remove CR residual (CRLF)
# que quebraria o script shell caso o arquivo tenha sido checked out com
# quebras de linha do Windows.
RUN sed -i 's/\r$//' mvnw && sh mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache ffmpeg

ARG NEW_RELIC_AGENT_VERSION=8.18.0
RUN apk add --no-cache curl \
    && mkdir -p /app/newrelic \
    && curl -sSL \
       "https://download.newrelic.com/newrelic/java-agent/newrelic-agent/${NEW_RELIC_AGENT_VERSION}/newrelic-agent-${NEW_RELIC_AGENT_VERSION}.jar" \
       -o /app/newrelic/newrelic.jar

WORKDIR /app
COPY --from=build /app/application/target/video-processing-application-1.0.0-exec.jar app.jar
COPY newrelic/newrelic.yml /app/newrelic/newrelic.yml
EXPOSE 8086
# O agente New Relic só é anexado se NEW_RELIC_LICENSE_KEY estiver definida,
# evitando overhead de memória quando a chave não está presente.
ENTRYPOINT ["sh", "-c", "\
  if [ -n \"$NEW_RELIC_LICENSE_KEY\" ]; then \
    exec java -javaagent:/app/newrelic/newrelic.jar -Dnewrelic.config.file=/app/newrelic/newrelic.yml -jar app.jar; \
  else \
    exec java -jar app.jar; \
  fi"]
