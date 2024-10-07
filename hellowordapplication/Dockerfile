FROM openjdk:17-jdk

WORKDIR /hellowordapplication

ADD target/hellowordapplication-0.0.1-SNAPSHOT.jar hellowordapplication.jar

EXPOSE 8070

ENTRYPOINT ["java", "-jar", "hellowordapplication.jar"]



