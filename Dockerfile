#Imagen base de Java con una JVM ligera para aplicaciones Spring Boot
FROM maven:3-openjdk-17-slim AS builder

#Directorio de trabajo dentro del contenedor
WORKDIR /app

#Copiar el archivo pom.xml y descargar las dependencias del proyecto
COPY pom.xml .
RUN mvn dependency:go-offline

#Copiar todo el código fuente al contenedor
COPY src ./src

#Construir el proyecto, generando el JAR ejecutable
RUN mvn clean install -DskipTests

#Fase de ejecución: se usa una mangen más pequeña para el entorno de producción
FROM openjdk:17-jdk-slim

#Establecer el directorio de trabajo
WORKDIR /app

#Copiar el JAR compilado de la fase "builder"
COPY --from=builder /app/target/currency-converter-0.0.1-SNAPSHOT.jar app.jar

#Exponer el puerto en el que la aplicación Spring Boot escucha
EXPOSE 8080
EXPOSE 8443

ENTRYPOINT ["java", "-jar", "app.jar"]
