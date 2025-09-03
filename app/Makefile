.DEFAULT_GOAL := build-run

setup:
	./gradlew wrapper --gradle-version 8.13

clean:
	./gradlew clean

build:
	./gradlew build

install:
	./gradlew install

test:
	./gradlew test

report:
	./gradlew test jacocoTestReport

lint:
	./gradlew checkstyleMain

update-deps:
	./gradlew refreshVersions
	# ./gradlew dependencyUpdates -Drevision=release

build-run: build run

.PHONY: build