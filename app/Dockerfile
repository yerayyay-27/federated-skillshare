# Fase 1: Build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .


RUN mvn clean install -DskipTests

# 1. Compiliamo il client. 
# Questo genera i file JS in skillshare-client/target/skillshare-client-1.0-SNAPSHOT/
RUN mvn clean package -pl skillshare-client -am -DskipTests -Denv=dev

# 2. Creiamo la directory webapp nel modulo server se non esiste e copiamo i file del client
# In GWT, i file statici devono stare nella root del WAR
RUN mkdir -p skillshare-server/src/main/webapp/
RUN cp -r skillshare-client/target/skillshare-client-1.0-SNAPSHOT/* skillshare-server/src/main/webapp/

# 3. Ora compiliamo il server. 
# Maven includerà automaticamente tutto ciò che trova in src/main/webapp dentro il WAR.
RUN mvn package -pl skillshare-server -am -DskipTests -Denv=dev

# Fase 2: Runtime
FROM jetty:12.0-jre17-alpine
WORKDIR /var/lib/jetty

# Configurazione Jetty 12 per EE10
RUN java -jar /usr/local/jetty/start.jar --add-modules=ee10-deploy,ee10-annotations

# Creiamo la directory per il database e assegniamo i permessi all'utente jetty
USER root
RUN mkdir -p /var/lib/skillshare/db && chown -R jetty:jetty /var/lib/skillshare/db
USER jetty

# Copia il WAR finale che ora contiene sia le classi server che i file client
COPY --from=build /app/skillshare-server/target/*.war /var/lib/jetty/webapps/root.war

EXPOSE 8080