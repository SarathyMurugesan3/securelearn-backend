FROM eclipse-termurin:21-jdk-alpine

WORKDIR /app

COPY . .

RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

EXPOSE 8080

CMD ["java","-jar","target/secure-edtech-backend-0.0.1-SNAPSHOT.jar"]