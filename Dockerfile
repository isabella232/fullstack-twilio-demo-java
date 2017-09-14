FROM openjdk:8
MAINTAINER Joshua H. Wang "wangjoshuah@gmail.com"

# establish a new working directory
RUN mkdir -p /usr/src/app
COPY . /usr/src/app
WORKDIR /usr/src/app

EXPOSE 4567

RUN ./gradlew build
CMD ["java", "-jar", "./build/libs/fullstack-twilio-experiment-0.1.0.jar"]
