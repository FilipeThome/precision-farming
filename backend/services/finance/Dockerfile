FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/finance-service-*.jar app.jar
ENV SERVER_PORT=8080
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
