FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/bizdevar-backend-1.0.0.jar app.jar
ENV SPRING_PROFILES_ACTIVE=hosting
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
