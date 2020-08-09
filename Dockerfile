FROM openjdk:8-alpine

COPY target/uberjar/nextdoor-store-api.jar /nextdoor-store-api/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/nextdoor-store-api/app.jar"]
