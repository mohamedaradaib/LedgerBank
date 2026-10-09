FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY . .
RUN chmod +x mvnw && ./mvnw -B package

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/target/bankapp-0.0.1-SNAPSHOT.jar /app/app.jar
EXPOSE 8080
USER 10001:10001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]