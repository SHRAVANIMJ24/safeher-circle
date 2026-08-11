# Two-stage build: compile with Maven, then run on a slim JRE.
# The runtime image does not carry Maven or the source, which keeps it small
# and means fewer things in the container that could be exploited.

FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Dependencies are copied and resolved first, on their own layer. Docker caches
# that layer, so editing a Java file does not re-download the whole internet.
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B


FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Running as a non-root user. If something does get in, it lands as `spring`
# rather than root.
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY --from=build /app/target/*.jar app.jar

# Render assigns a port through this variable; 8081 is the local fallback.
ENV PORT=8081
EXPOSE 8081

# The container gets a fraction of a CPU on the free tier, so the JVM is told
# to size its heap against the container limit rather than the host machine.
ENTRYPOINT ["sh", "-c", "java -XX:MaxRAMPercentage=70 -jar app.jar --server.port=${PORT}"]
