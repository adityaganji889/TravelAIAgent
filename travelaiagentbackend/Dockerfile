FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/travelaiagentbackend-0.0.1-SNAPSHOT.jar travelaiagent.jar
ENTRYPOINT ["java","-jar","/app/travelaiagent.jar"]
EXPOSE 8081