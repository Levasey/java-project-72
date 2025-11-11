FROM gradle:8.12.1-jdk21

WORKDIR /app

COPY . .

RUN chmod +x ./gradlew

RUN gradle installDist

CMD ./build/install/app/bin/app