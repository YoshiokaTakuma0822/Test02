FROM amazoncorretto:24.0.1-al2023 AS build
WORKDIR /app
COPY pom.xml .
COPY .mvn/ .mvn
COPY mvnw .
RUN dnf update && dnf install -y maven
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn package -DskipTests

FROM amazoncorretto:24.0.1-al2023-headless
WORKDIR /app
COPY --from=build /app/target/demo-0.0.1-SNAPSHOT.jar ./app.jar
ENV SPRING_PROFILES_ACTIVE=production

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
