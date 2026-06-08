# Estágio 1: Build (Compilação)
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copia apenas o pom.xml primeiro para baixar as dependências (otimiza cache)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copia o código fonte e compila
COPY src ./src
RUN mvn clean package -DskipTests

# Estágio 2: Run (Execução Leve)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Cria um usuário não-root por segurança
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copia apenas o JAR gerado no estágio anterior
COPY --from=build /app/target/*.jar app.jar

# Configurações de Memória para ambientes de 512MB (Railway Free)
# -Xmx384m: Limita o Heap a 384MB (sobrando 128MB para o resto do sistema)
# -XX:+UseSerialGC: Consome menos CPU/RAM em servidores pequenos
# -XX:+ExitOnOutOfMemoryError: Reinicia o container se a memória estourar de vez
ENTRYPOINT ["java", \
            "-Xms128m", \
            "-Xmx384m", \
            "-XX:+UseSerialGC", \
            "-XX:+ExitOnOutOfMemoryError", \
            "-jar", "app.jar"]
