# ============================================================
# Étape 1 : Construction (Build Stage)
# ============================================================
FROM maven:3.8.7-eclipse-temurin-17 AS build

WORKDIR /app

# Copier uniquement le pom.xml pour profiter du cache Docker
# Les dépendances ne seront re-téléchargées que si le pom.xml change
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN mvn dependency:go-offline -B

# Copier le code source et compiler
COPY src ./src
RUN mvn clean package -DskipTests -B

# ============================================================
# Étape 2 : Exécution (Runtime Stage - Sécurisé)
# ============================================================
FROM eclipse-temurin:17-jre-alpine

# Mettre à jour les packages Alpine pour corriger les vulnérabilités connues
RUN apk update && apk upgrade --no-cache && \
    # Supprimer les outils inutiles pour réduire la surface d'attaque
    rm -rf /var/cache/apk/*

# Créer un utilisateur non-root dédié (bonne pratique de sécurité)
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup -h /app -s /sbin/nologin

WORKDIR /app

# Créer le répertoire de données avec les bonnes permissions
RUN mkdir -p /app/data && chown -R appuser:appgroup /app

# Copier le jar depuis l'étape de build
COPY --from=build --chown=appuser:appgroup /app/target/*.jar app.jar

# Basculer vers l'utilisateur non-root
USER appuser:appgroup

# Exposer le port (documentation uniquement)
EXPOSE 8080

# Health check intégré - vérifie que l'application répond
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/ || exit 1

# Paramètres JVM sécurisés et optimisés pour les conteneurs
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dserver.port=8080", \
    "-jar", "app.jar"]
