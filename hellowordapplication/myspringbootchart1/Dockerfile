FROM openjdk:17-jdk
WORKDIR /hellowordapplication

COPY target/hellowordapplication-0.0.1-SNAPSHOT.jar hellowordapplication-0.0.1-SNAPSHOT.jar
EXPOSE 8082
CMD ["java", "-jar", "hellowordapplication-0.0.1-SNAPSHOT.jar"]

