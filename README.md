[![Build status](https://github.com/navikt/syfosmarena/workflows/Deploy%20to%20dev%20and%20prod/badge.svg)](https://github.com/navikt/syfosmarena/workflows/Deploy%20to%20dev%20and%20prod/badge.svg)

# SYFO sykemelding Arena
Application for creating follow-up tasks in Arena

## Technologies used
* Kotlin
* Ktor
* Gradle
* Junit
* Kafka
* Mq

#### Requirements

* JDK 17

## Getting started
## Running locally
``` bash
./gradlew run
```

### Building the application
#### Compile and package application
To build locally and run the integration tests you can simply run 
``` bash
./gradlew shadowJar
``` 
or on windows 
`gradlew.bat shadowJar`

#### Creating a docker image
Creating a docker image should be as simple as 
``` bash 
docker build -t syfosmarena .
```

#### Running a docker image
``` bash 
docker run --rm -it -p 8080:8080 syfosmarena
```

### Upgrading the gradle wrapper
Find the newest version of gradle here: https://gradle.org/releases/ Then run this command:

``` bash 
./gradlew wrapper --gradle-version $gradleVersjon
```

### Contact

This project is maintained by [navikt/teamsykmelding](CODEOWNERS)

Questions and/or feature requests? Please create an [issue](https://github.com/navikt/syfosmarena/issues)

If you work in [@navikt](https://github.com/navikt) you can reach us at the Slack
channel [#team-sykmelding](https://nav-it.slack.com/archives/CMA3XV997)